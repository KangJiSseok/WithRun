import { useEffect, useRef } from 'react';

import type { CoordinatePoint } from '@/apis';
import { useRunLocation } from '@/shared/hooks/useRunLocation';
import { haversineM } from '@/shared/utils/geo';
import { loadKakaoMapSdk } from '@/shared/utils';

const DEFAULT_CENTER = { lat: 37.5665, lng: 126.978 };

const POLYLINE_STROKE_WEIGHT = 5;
const POLYLINE_STROKE_COLOR = '#4F80FF';
const POLYLINE_STROKE_OPACITY = 0.9;

const LERP = 0.12;

function lerpAngle(current: number, target: number, t: number): number {
  const diff = ((target - current + 540) % 360) - 180;
  return (current + diff * t + 360) % 360;
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

// 고스트 러너 도트
function createGhostMarkerContent(): HTMLElement {
  const wrapper = document.createElement('div');
  wrapper.style.cssText = `
    position: relative;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
  `;

  const dot = document.createElement('div');
  dot.style.cssText = `
    width: 16px;
    height: 16px;
    background: #F97316;
    border: 2.5px solid white;
    border-radius: 50%;
    box-shadow: 0 0 0 6px rgba(249,115,22,0.2), 0 2px 4px rgba(0,0,0,0.3);
  `;

  wrapper.appendChild(dot);
  return wrapper;
}

/** cumDist[i] = 좌표 0 → i 까지 누적 거리(m) */
function buildCumDist(coords: CoordinatePoint[]): number[] {
  const cumDist = [0];
  for (let i = 1; i < coords.length; i++) {
    const d = haversineM(
      coords[i - 1].latitude,
      coords[i - 1].longitude,
      coords[i].latitude,
      coords[i].longitude
    );
    cumDist.push(cumDist[i - 1] + d);
  }
  return cumDist;
}

/** 누적 거리 배열에서 targetDist 위치의 lat/lng 보간 */
function interpolateAlongRoute(
  coords: CoordinatePoint[],
  cumDist: number[],
  totalDist: number,
  targetDist: number
): { lat: number; lng: number } {
  if (targetDist >= totalDist) {
    const last = coords[coords.length - 1];
    return { lat: last.latitude, lng: last.longitude };
  }

  // 이분 탐색으로 세그먼트 찾기
  let lo = 0;
  let hi = cumDist.length - 1;
  while (lo + 1 < hi) {
    const mid = (lo + hi) >> 1;
    if (cumDist[mid] <= targetDist) lo = mid;
    else hi = mid;
  }

  const segLen = cumDist[hi] - cumDist[lo];
  const t = segLen > 0 ? (targetDist - cumDist[lo]) / segLen : 0;

  return {
    lat: coords[lo].latitude + t * (coords[hi].latitude - coords[lo].latitude),
    lng:
      coords[lo].longitude + t * (coords[hi].longitude - coords[lo].longitude),
  };
}

export function useGhostRunMap(
  containerRef: React.RefObject<HTMLDivElement | null>,
  params: {
    coordinates: CoordinatePoint[];
    courseAnchor: { latitude: number; longitude: number } | null;
    ghostDurationSec: number;
    isPaused: boolean;
    ghostDistanceMRef?: React.MutableRefObject<number>;
  }
): void {
  const { location } = useRunLocation();
  const mapRef = useRef<kakao.maps.Map | null>(null);
  const userDotRef = useRef<kakao.maps.CustomOverlay | null>(null);
  const ghostDotRef = useRef<kakao.maps.CustomOverlay | null>(null);
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

  const ghostStateRef = useRef({
    startMs: 0,
    totalPausedMs: 0,
    pauseStartMs: 0,
    isPaused: false,
    cumDist: [] as number[],
    totalDist: 0,
    coords: [] as CoordinatePoint[],
  });

  // 지도 초기화 + 폴리라인 + 사용자 마커 + 고스트 마커 + RAF 루프
  useEffect(() => {
    if (!containerRef.current) return;

    const container = containerRef.current;
    const { coordinates, ghostDurationSec } = params;
    const hasGhost = ghostDurationSec > 0 && coordinates.length >= 2;

    let cancelled = false;

    loadKakaoMapSdk()
      .then(() => {
        if (cancelled) return;
        console.log(
          '[GhostRunMap] SDK loaded. ghostDurationSec:',
          ghostDurationSec,
          'hasGhost:',
          hasGhost,
          'coords:',
          coordinates.length
        );

        const center = new kakao.maps.LatLng(
          DEFAULT_CENTER.lat,
          DEFAULT_CENTER.lng
        );
        mapRef.current = new kakao.maps.Map(container, { center, level: 5 });

        // 폴리라인
        if (coordinates.length > 0) {
          const path = coordinates.map(
            (c) => new kakao.maps.LatLng(c.latitude, c.longitude)
          );
          const polyline = new kakao.maps.Polyline({
            path,
            strokeWeight: POLYLINE_STROKE_WEIGHT,
            strokeColor: POLYLINE_STROKE_COLOR,
            strokeOpacity: POLYLINE_STROKE_OPACITY,
            strokeStyle: 'solid',
          });
          polyline.setMap(mapRef.current);

          const bounds = new kakao.maps.LatLngBounds();
          path.forEach((latLng) => bounds.extend(latLng));
          mapRef.current.setBounds(bounds);
        }

        // 사용자 마커
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

        // 고스트 마커 + 누적거리 사전 계산
        if (hasGhost) {
          const ghostEl = createGhostMarkerContent();
          ghostDotRef.current = new kakao.maps.CustomOverlay({
            map: mapRef.current,
            position: new kakao.maps.LatLng(
              coordinates[0].latitude,
              coordinates[0].longitude
            ),
            content: ghostEl,
            xAnchor: 0.5,
            yAnchor: 0.5,
            zIndex: 9,
          });

          const cumDist = buildCumDist(coordinates);
          ghostStateRef.current.cumDist = cumDist;
          ghostStateRef.current.totalDist = cumDist[cumDist.length - 1];
          ghostStateRef.current.coords = coordinates;
          ghostStateRef.current.startMs = Date.now();
          ghostStateRef.current.totalPausedMs = 0;
          ghostStateRef.current.pauseStartMs = 0;
          ghostStateRef.current.isPaused = params.isPaused;
          if (params.isPaused) {
            ghostStateRef.current.pauseStartMs = Date.now();
          }
          console.log(
            '[GhostRunMap] 고스트 마커 생성 완료. totalDist:',
            ghostStateRef.current.totalDist,
            'm'
          );
        }

        // RAF 루프
        const s = animStateRef.current;
        const g = ghostStateRef.current;

        function tick() {
          // 사용자 도트 위치 lerp
          s.currentLat += (s.targetLat - s.currentLat) * LERP;
          s.currentLng += (s.targetLng - s.currentLng) * LERP;
          userDotRef.current?.setPosition(
            new kakao.maps.LatLng(s.currentLat, s.currentLng)
          );

          // 방향 cone
          const isMoving = (s.targetSpeed ?? 0) > 0.5;
          if (s.coneEl && s.targetHeading !== null && isMoving) {
            s.currentHeading = lerpAngle(
              s.currentHeading,
              s.targetHeading,
              LERP
            );
            s.coneEl.style.transform = `rotate(${s.currentHeading}deg)`;
            s.coneEl.style.opacity = '0.75';
          } else if (s.coneEl) {
            s.coneEl.style.opacity = '0';
          }

          // 고스트 도트 위치 갱신
          if (hasGhost) {
            const nowMs = Date.now();
            const elapsedSec = (nowMs - g.startMs - g.totalPausedMs) / 1000;
            const progress = Math.min(elapsedSec / ghostDurationSec, 1);
            const targetDist = progress * g.totalDist;
            if (params.ghostDistanceMRef) {
              params.ghostDistanceMRef.current = targetDist;
            }
            if (ghostDotRef.current && !g.isPaused) {
              const pos = interpolateAlongRoute(
                g.coords,
                g.cumDist,
                g.totalDist,
                targetDist
              );
              ghostDotRef.current.setPosition(
                new kakao.maps.LatLng(pos.lat, pos.lng)
              );
            }
          }

          s.rafId = requestAnimationFrame(tick);
        }

        s.rafId = requestAnimationFrame(tick);
      })
      .catch((err) => {
        console.error('[GhostRunMap] SDK 초기화 에러:', err);
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

  // GPS 업데이트: 애니메이션 target 갱신 + 지도 중심 이동
  useEffect(() => {
    if (!location) return;

    const s = animStateRef.current;
    s.targetLat = location.latitude;
    s.targetLng = location.longitude;
    s.targetHeading = location.heading;
    s.targetSpeed = location.speedMps;

    if (!s.initialized) {
      s.currentLat = location.latitude;
      s.currentLng = location.longitude;
      s.currentHeading = location.heading ?? 0;
      s.initialized = true;
    }

    mapRef.current?.panTo(
      new kakao.maps.LatLng(location.latitude, location.longitude)
    );
  }, [location]);

  // isPaused 변화 → 고스트 시계 pause/resume
  useEffect(() => {
    const g = ghostStateRef.current;
    const wasPaused = g.isPaused;
    const nowPaused = params.isPaused;
    g.isPaused = nowPaused;

    if (!wasPaused && nowPaused) {
      // 일시정지 시작
      g.pauseStartMs = Date.now();
    } else if (wasPaused && !nowPaused) {
      // 재개: 일시정지 시간 누산
      g.totalPausedMs += Date.now() - g.pauseStartMs;
    }
  }, [params.isPaused]);

  // courseAnchor 변화 시 안정권 원 표시/제거
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
