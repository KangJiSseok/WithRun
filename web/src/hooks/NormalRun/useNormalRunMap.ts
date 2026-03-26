import { useEffect, useRef } from 'react';

import type { CoordinatePoint } from '@/apis';
import { useRunLocation } from '@/shared/hooks/useRunLocation';
import { loadKakaoMapSdk } from '@/shared/utils';

const DEFAULT_CENTER = { lat: 37.5665, lng: 126.978 };

const POLYLINE_STROKE_WEIGHT = 5;
const POLYLINE_STROKE_COLOR = '#4F80FF';
const POLYLINE_STROKE_OPACITY = 0.9;

const LERP = 0.12; // 60fps 기준 약 1초에 99% 수렴

/** 최단 경로 각도 보간 (359° → 1° = +2°, −358°가 아님) */
function lerpAngle(current: number, target: number, t: number): number {
  const diff = ((target - current + 540) % 360) - 180;
  return (current + diff * t + 360) % 360;
}

function createUserMarkerContent(): { element: HTMLElement; coneEl: HTMLElement } {
  const wrapper = document.createElement('div');
  wrapper.style.cssText = `
    position: relative;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
  `;

  // 방향 삼각형 (CSS border trick)
  const cone = document.createElement('div');
  cone.style.cssText = `
    position: absolute;
    left: calc(50% - 8px);
    bottom: 50%;
    transform-origin: 8px 18px;
    transform: rotate(0deg);
    width: 0;
    height: 0;
    border-left: 8px solid transparent;
    border-right: 8px solid transparent;
    border-bottom: 18px solid rgba(59,130,246,0.5);
    opacity: 0;
    transition: opacity 0.3s;
  `;

  // 파란 도트
  const dot = document.createElement('div');
  dot.style.cssText = `
    position: relative;
    z-index: 1;
    width: 16px;
    height: 16px;
    background: #3B82F6;
    border: 2.5px solid white;
    border-radius: 50%;
    box-shadow: 0 0 0 6px rgba(59,130,246,0.2), 0 2px 4px rgba(0,0,0,0.3);
  `;

  wrapper.appendChild(cone);
  wrapper.appendChild(dot);

  return { element: wrapper, coneEl: cone };
}

export function useNormalRunMap(
  containerRef: React.RefObject<HTMLDivElement | null>,
  params: {
    coordinates: CoordinatePoint[];
    courseAnchor: { latitude: number; longitude: number } | null;
  }
): void {
  const { location } = useRunLocation();
  const mapRef = useRef<kakao.maps.Map | null>(null);
  const userDotRef = useRef<kakao.maps.CustomOverlay | null>(null);
  const safeZoneCircleRef = useRef<kakao.maps.Circle | null>(null);
  const animStateRef = useRef({
    rafId: null as number | null,
    currentLat: 0,
    currentLng: 0,
    targetLat: 0,
    targetLng: 0,
    currentHeading: 0,
    targetHeading: null as number | null,
    targetSpeed: null as number | null,
    initialized: false,
    coneEl: null as HTMLElement | null,
  });

  // 지도 초기화 + 폴리라인 + 사용자 마커 + RAF 루프 시작
  useEffect(() => {
    if (!containerRef.current) return;

    const container = containerRef.current;
    const { coordinates } = params;

    let cancelled = false;

    loadKakaoMapSdk().then(() => {
      if (cancelled) return;
      const center = new kakao.maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng);
      mapRef.current = new kakao.maps.Map(container, { center, level: 5 });

      // 폴리라인
      if (coordinates.length > 0) {
        const path = coordinates.map((c) => new kakao.maps.LatLng(c.latitude, c.longitude));

        const polyline = new kakao.maps.Polyline({
          path,
          strokeWeight: POLYLINE_STROKE_WEIGHT,
          strokeColor: POLYLINE_STROKE_COLOR,
          strokeOpacity: POLYLINE_STROKE_OPACITY,
          strokeStyle: 'solid',
        });
        polyline.setMap(mapRef.current);

        // 경로 전체가 보이도록 bounds 조정
        const bounds = new kakao.maps.LatLngBounds();
        path.forEach((latLng) => bounds.extend(latLng));
        mapRef.current.setBounds(bounds);
      }

      // 사용자 위치 마커 (도트 + 방향 cone)
      const { element, coneEl } = createUserMarkerContent();
      animStateRef.current.coneEl = coneEl;

      userDotRef.current = new kakao.maps.CustomOverlay({
        map: mapRef.current,
        position: center,
        content: element,
        xAnchor: 0.5,
        yAnchor: 0.5,
        zIndex: 10,
      });

      // RAF 루프 시작
      const s = animStateRef.current;

      function tick() {
        if (!userDotRef.current) return;

        // 위치 lerp
        s.currentLat += (s.targetLat - s.currentLat) * LERP;
        s.currentLng += (s.targetLng - s.currentLng) * LERP;
        userDotRef.current.setPosition(new kakao.maps.LatLng(s.currentLat, s.currentLng));

        // 방향 cone 업데이트
        const isMoving = (s.targetSpeed ?? 0) > 0.5;
        if (s.coneEl && s.targetHeading !== null && isMoving) {
          s.currentHeading = lerpAngle(s.currentHeading, s.targetHeading, LERP);
          s.coneEl.style.transform = `rotate(${s.currentHeading}deg)`;
          s.coneEl.style.opacity = '0.75';
        } else if (s.coneEl) {
          s.coneEl.style.opacity = '0';
        }

        s.rafId = requestAnimationFrame(tick);
      }

      s.rafId = requestAnimationFrame(tick);
    });

    return () => {
      cancelled = true;
      const s = animStateRef.current;
      if (s.rafId !== null) {
        cancelAnimationFrame(s.rafId);
        s.rafId = null;
      }
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [containerRef]);

  // GPS 업데이트: 애니메이션 target 갱신 + 지도 중심 이동 (1Hz)
  useEffect(() => {
    if (!location) return;

    const s = animStateRef.current;
    s.targetLat = location.latitude;
    s.targetLng = location.longitude;
    s.targetHeading = location.heading;
    s.targetSpeed = location.speedMps;

    // 첫 GPS 수신 시 current를 target으로 초기화 (원점에서 lerp 방지)
    if (!s.initialized) {
      s.currentLat = location.latitude;
      s.currentLng = location.longitude;
      s.currentHeading = location.heading ?? 0;
      s.initialized = true;
    }

    // 지도 중심 이동: 1Hz, Kakao 자체 애니메이션 사용 (RAF 루프 내 호출 금지)
    mapRef.current?.panTo(new kakao.maps.LatLng(location.latitude, location.longitude));
  }, [location]);

  // courseAnchor 변화 시 안정권 원(반지름 30m) 표시/제거
  useEffect(() => {
    const { courseAnchor } = params;

    if (courseAnchor && mapRef.current) {
      const center = new kakao.maps.LatLng(courseAnchor.latitude, courseAnchor.longitude);

      if (!safeZoneCircleRef.current) {
        safeZoneCircleRef.current = new kakao.maps.Circle({
          map: mapRef.current,
          center,
          radius: 30,
          strokeWeight: 2,
          strokeColor: '#22c55e',
          strokeOpacity: 0.7,
          fillColor: '#22c55e',
          fillOpacity: 0.15,
        });
      } else {
        safeZoneCircleRef.current.setPosition(center);
      }
    } else {
      safeZoneCircleRef.current?.setMap(null);
      safeZoneCircleRef.current = null;
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.courseAnchor]);
}
