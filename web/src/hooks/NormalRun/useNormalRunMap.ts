import { useEffect, useRef } from 'react';

import type { CoordinatePoint } from '@/apis';
import type { Checkpoint } from '@/hooks/shared/useRunCheckpoints';
import type { RouteType } from '@/shared/types/api.type';
import { useRunLocation } from '@/shared/hooks/useRunLocation';
import { loadKakaoMapSdk } from '@/shared/utils';

const DEFAULT_CENTER = { lat: 37.5665, lng: 126.978 };

const POLYLINE_STROKE_WEIGHT = 5;
const POLYLINE_STROKE_COLOR = '#4F80FF';
const POLYLINE_STROKE_COLOR_VISITED = '#22c55e';
const NEXT_GATE_STROKE_COLOR = '#f97316';
const NEXT_GATE_STROKE_WEIGHT = 4;
const POLYLINE_STROKE_OPACITY = 0.9;

const LERP = 0.12; // 60fps 기준 약 1초에 99% 수렴

const M_PER_DEG_LAT = 110_540; // 위도 1도당 미터 (타원체 보정값)
const M_PER_DEG_LON_EQUATOR = 111_320; // 적도 기준 경도 1도당 미터

const ROUTE_ARROW_COUNT = 15; // 경로 위 방향 화살표 수

/** 최단 경로 각도 보간 (359° → 1° = +2°, −358°가 아님) */
function lerpAngle(current: number, target: number, t: number): number {
  const diff = ((target - current + 540) % 360) - 180;
  return (current + diff * t + 360) % 360;
}

function createRouteArrowElement(bearingDeg: number): HTMLElement {
  const el = document.createElement('div');
  el.style.cssText = 'pointer-events:none;';
  el.innerHTML = `<svg viewBox="0 0 10 14" width="10" height="14" style="display:block;transform:rotate(${bearingDeg}deg);transform-origin:center"><path d="M5 0 L10 14 L5 9 L0 14Z" fill="white" fill-opacity="0.8"/></svg>`;
  return el;
}

function createUserMarkerContent(): {
  element: HTMLElement;
  coneEl: HTMLElement;
} {
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
    routeType?: RouteType;
    courseAnchor: { latitude: number; longitude: number } | null;
    checkpoints?: Checkpoint[];
    visitedCount?: number;
    nextCheckpointIdx?: number;
  }
): void {
  const { location } = useRunLocation();
  const mapRef = useRef<kakao.maps.Map | null>(null);
  const userDotRef = useRef<kakao.maps.CustomOverlay | null>(null);
  const safeZoneCircleRef = useRef<kakao.maps.Circle | null>(null);
  const visitedPolylineRef = useRef<kakao.maps.Polyline | null>(null);
  const remainingPolylineRef = useRef<kakao.maps.Polyline | null>(null);
  const checkpointOverlaysRef = useRef<kakao.maps.CustomOverlay[]>([]);
  const nextGatePolylineRef = useRef<kakao.maps.Polyline | null>(null);
  const routeArrowOverlaysRef = useRef<kakao.maps.CustomOverlay[]>([]);
  const animStateRef = useRef({
    rafId: null as number | null,
    currentLat: 0,
    currentLng: 0,
    // dead-reckoning: 마지막 GPS fix 기준으로 속도·방향 예측
    lastGpsLat: 0,
    lastGpsLng: 0,
    lastGpsSpeedMps: 0,
    lastGpsHeadingRad: 0,
    lastGpsTimestamp: 0,
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
      const center = new kakao.maps.LatLng(
        DEFAULT_CENTER.lat,
        DEFAULT_CENTER.lng
      );
      mapRef.current = new kakao.maps.Map(container, { center, level: 5 });

      // 폴리라인 (잔여 구간 파란색 + 방문 구간 녹색 오버레이)
      if (coordinates.length > 0) {
        const path = coordinates.map(
          (c) => new kakao.maps.LatLng(c.latitude, c.longitude)
        );

        remainingPolylineRef.current = new kakao.maps.Polyline({
          path,
          strokeWeight: POLYLINE_STROKE_WEIGHT,
          strokeColor: POLYLINE_STROKE_COLOR,
          strokeOpacity: POLYLINE_STROKE_OPACITY,
          strokeStyle: 'solid',
        });
        remainingPolylineRef.current.setMap(mapRef.current);

        visitedPolylineRef.current = new kakao.maps.Polyline({
          path: [],
          strokeWeight: POLYLINE_STROKE_WEIGHT,
          strokeColor: POLYLINE_STROKE_COLOR_VISITED,
          strokeOpacity: POLYLINE_STROKE_OPACITY,
          strokeStyle: 'solid',
        });
        visitedPolylineRef.current.setMap(mapRef.current);

        // 경로 전체가 보이도록 bounds 조정
        const bounds = new kakao.maps.LatLngBounds();
        path.forEach((latLng) => bounds.extend(latLng));
        mapRef.current.setBounds(bounds);

        // 진행 방향 화살표 오버레이 (약 ROUTE_ARROW_COUNT개 균등 배치)
        // OUT_AND_BACK은 전반부(출발→반환점)에만 화살표 배치
        const arrowCoords =
          params.routeType === 'OUT_AND_BACK'
            ? coordinates.slice(0, Math.ceil(coordinates.length / 2))
            : coordinates;
        const step = Math.max(
          1,
          Math.floor(arrowCoords.length / ROUTE_ARROW_COUNT)
        );
        const startOffset = Math.floor(step / 2);
        for (let i = startOffset; i < arrowCoords.length - 1; i += step) {
          const cur = arrowCoords[i];
          const next = arrowCoords[Math.min(i + 1, arrowCoords.length - 1)];
          const dx =
            (next.longitude - cur.longitude) *
            Math.cos((cur.latitude * Math.PI) / 180) *
            M_PER_DEG_LON_EQUATOR;
          const dy = (next.latitude - cur.latitude) * M_PER_DEG_LAT;
          const bearingDeg = Math.atan2(dx, dy) * (180 / Math.PI);

          const arrowEl = createRouteArrowElement(bearingDeg);

          routeArrowOverlaysRef.current.push(
            new kakao.maps.CustomOverlay({
              map: mapRef.current!,
              position: new kakao.maps.LatLng(cur.latitude, cur.longitude),
              content: arrowEl,
              xAnchor: 0.5,
              yAnchor: 0.5,
              zIndex: 3,
            })
          );
        }
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

        // dead-reckoning: 마지막 GPS fix + 경과 시간 × 속도·방향으로 예측 위치 계산
        if (s.initialized && s.lastGpsTimestamp > 0) {
          const elapsed = Math.min(
            (Date.now() - s.lastGpsTimestamp) / 1000,
            2.0
          );
          const cosLat = Math.cos((s.lastGpsLat * Math.PI) / 180);
          const predictedLat =
            s.lastGpsLat +
            (s.lastGpsSpeedMps * elapsed * Math.cos(s.lastGpsHeadingRad)) /
              M_PER_DEG_LAT;
          const predictedLng =
            s.lastGpsLng +
            (s.lastGpsSpeedMps * elapsed * Math.sin(s.lastGpsHeadingRad)) /
              (M_PER_DEG_LON_EQUATOR * cosLat);
          s.currentLat += (predictedLat - s.currentLat) * 0.25;
          s.currentLng += (predictedLng - s.currentLng) * 0.25;
        }
        userDotRef.current.setPosition(
          new kakao.maps.LatLng(s.currentLat, s.currentLng)
        );

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
      routeArrowOverlaysRef.current.forEach((o) => o.setMap(null));
      routeArrowOverlaysRef.current = [];
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [containerRef]);

  // GPS 업데이트: 애니메이션 target 갱신 + 지도 중심 이동 (1Hz)
  useEffect(() => {
    if (!location) return;

    const s = animStateRef.current;
    // dead-reckoning 기준점 갱신
    s.lastGpsLat = location.latitude;
    s.lastGpsLng = location.longitude;
    s.lastGpsSpeedMps = location.speedMps ?? 0;
    s.lastGpsHeadingRad = ((location.heading ?? 0) * Math.PI) / 180;
    s.lastGpsTimestamp = Date.now();
    s.targetHeading = location.heading;
    s.targetSpeed = location.speedMps;

    // 첫 GPS 수신 시 current를 초기화 (원점에서 lerp 방지)
    if (!s.initialized) {
      s.currentLat = location.latitude;
      s.currentLng = location.longitude;
      s.currentHeading = location.heading ?? 0;
      s.initialized = true;
    }

    // 지도 중심 이동: 1Hz, Kakao 자체 애니메이션 사용 (RAF 루프 내 호출 금지)
    mapRef.current?.panTo(
      new kakao.maps.LatLng(location.latitude, location.longitude)
    );
  }, [location]);

  // visitedCount 변화 → 방문 구간 폴리라인 + 체크포인트 도트 색상 갱신
  useEffect(() => {
    const { checkpoints = [], visitedCount = 0, coordinates } = params;
    if (!mapRef.current || coordinates.length === 0) return;

    if (checkpointOverlaysRef.current.length !== checkpoints.length) {
      checkpointOverlaysRef.current.forEach((overlay) => overlay.setMap(null));
      checkpointOverlaysRef.current = checkpoints.map((cp) => {
        const el = document.createElement('div');
        el.style.cssText = `
          width: 10px; height: 10px;
          background: #9ca3af;
          border: 2px solid white;
          border-radius: 50%;
          box-shadow: 0 1px 3px rgba(0,0,0,0.3);
        `;
        return new kakao.maps.CustomOverlay({
          map: mapRef.current!,
          position: new kakao.maps.LatLng(cp.latitude, cp.longitude),
          content: el,
          xAnchor: 0.5,
          yAnchor: 0.5,
          zIndex: 5,
        });
      });
    }

    // 방문한 마지막 체크포인트의 coordIdx까지 녹색 폴리라인 적용
    if (visitedCount > 0) {
      const lastVisitedCoordIdx = checkpoints[visitedCount - 1]?.coordIdx ?? 0;
      const visitedPath = coordinates
        .slice(0, lastVisitedCoordIdx + 1)
        .map((c) => new kakao.maps.LatLng(c.latitude, c.longitude));
      visitedPolylineRef.current?.setPath(visitedPath);
    } else {
      visitedPolylineRef.current?.setPath([]);
    }

    // 체크포인트 도트 색상 갱신 (방문=녹색, 미방문=회색)
    checkpointOverlaysRef.current.forEach((overlay, i) => {
      const el = overlay.getContent() as HTMLElement;
      el.style.background = i < visitedCount ? '#22c55e' : '#9ca3af';
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.checkpoints, params.visitedCount]);

  // 다음에 지나야 하는 체크포인트 게이트 1개만 시각화
  useEffect(() => {
    const { checkpoints = [], nextCheckpointIdx = 0 } = params;
    if (!mapRef.current) return;

    if (nextCheckpointIdx >= checkpoints.length) {
      nextGatePolylineRef.current?.setMap(null);
      nextGatePolylineRef.current = null;
      return;
    }

    const next = checkpoints[nextCheckpointIdx];
    const path = [
      new kakao.maps.LatLng(next.gate.startLatitude, next.gate.startLongitude),
      new kakao.maps.LatLng(next.gate.endLatitude, next.gate.endLongitude),
    ];

    if (!nextGatePolylineRef.current) {
      nextGatePolylineRef.current = new kakao.maps.Polyline({
        path,
        strokeWeight: NEXT_GATE_STROKE_WEIGHT,
        strokeColor: NEXT_GATE_STROKE_COLOR,
        strokeOpacity: 0.95,
        strokeStyle: 'shortdash',
      });
      nextGatePolylineRef.current.setMap(mapRef.current);
      return;
    }

    nextGatePolylineRef.current.setPath(path);
    nextGatePolylineRef.current.setMap(mapRef.current);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.checkpoints, params.nextCheckpointIdx]);

  // courseAnchor 변화 시 안정권 원(반지름 30m) 표시/제거
  useEffect(() => {
    const { courseAnchor } = params;

    if (courseAnchor && mapRef.current) {
      const center = new kakao.maps.LatLng(
        courseAnchor.latitude,
        courseAnchor.longitude
      );

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
