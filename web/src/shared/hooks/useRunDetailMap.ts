import { useEffect, useRef, useState } from 'react';

import type { CoordinatePoint } from '@/apis';
import { createMarkerContent, loadKakaoMapSdk } from '@/shared/utils';
import { useRunLocation } from './useRunLocation';

// 폴리라인 스타일
const POLYLINE_STROKE_WEIGHT = 5;
const POLYLINE_STROKE_COLOR = '#4F80FF';
const POLYLINE_STROKE_OPACITY = 0.9;

// 출발지 근접 판정 반경 (미터)
const PROXIMITY_THRESHOLD_M = 50;

// 두 GPS 좌표 간 거리 계산 (Haversine, 단위: 미터)
function haversineM(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const R = 6371000;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function createUserDotContent(label: string, color: string): HTMLElement {
  const wrapper = document.createElement('div');
  wrapper.style.cssText =
    'display:flex;flex-direction:column;align-items:center;gap:4px;';

  const dot = document.createElement('div');
  dot.style.cssText = `
    width: 16px;
    height: 16px;
    background: ${color};
    border: 2.5px solid white;
    border-radius: 50%;
    box-shadow: 0 0 0 6px rgba(59,130,246,0.2), 0 2px 4px rgba(0,0,0,0.3);
  `;

  const tag = document.createElement('span');
  tag.textContent = label;
  tag.style.cssText = `
    background: ${color};
    color: white;
    font-size: 11px;
    font-weight: 700;
    padding: 2px 6px;
    border-radius: 4px;
    white-space: nowrap;
    box-shadow: 0 1px 3px rgba(0,0,0,0.2);
  `;

  wrapper.appendChild(dot);
  wrapper.appendChild(tag);

  return wrapper;
}

type CourseMapParams = {
  coordinates: CoordinatePoint[];
  startLatitude: number;
  startLongitude: number;
  endLatitude: number;
  endLongitude: number;
};

export function useRunDetailMap(
  containerRef: React.RefObject<HTMLDivElement | null>,
  course: CourseMapParams
): { isNearStart: boolean } {
  const { location } = useRunLocation();
  const [isNearStart, setIsNearStart] = useState(false);

  const mapRef = useRef<kakao.maps.Map | null>(null);
  const userDotRef = useRef<kakao.maps.CustomOverlay | null>(null);
  // 첫 GPS 위치가 들어올 때 코스 + 사용자 위치를 한 화면에 담기 위해 경로 보관
  const coursePathRef = useRef<kakao.maps.LatLng[]>([]);
  // 초기 bounds 조정 완료 여부 (최초 1회만 실행)
  const hasInitialBoundsRef = useRef(false);

  // 지도 초기화 + 폴리라인 + 시작/끝 마커 (단일 kakao.maps.load 콜백)
  useEffect(() => {
    if (!containerRef.current) return;

    const container = containerRef.current;
    const {
      coordinates,
      startLatitude,
      startLongitude,
      endLatitude,
      endLongitude,
    } = course;

    let cancelled = false;

    loadKakaoMapSdk().then(() => {
      if (cancelled) return;
      const startLatLng = new kakao.maps.LatLng(startLatitude, startLongitude);

      mapRef.current = new kakao.maps.Map(container, {
        center: startLatLng,
        level: 5,
      });

      // 폴리라인
      if (coordinates.length > 0) {
        const path = coordinates.map(
          (c) => new kakao.maps.LatLng(c.latitude, c.longitude)
        );
        coursePathRef.current = path;

        const polyline = new kakao.maps.Polyline({
          path,
          strokeWeight: POLYLINE_STROKE_WEIGHT,
          strokeColor: POLYLINE_STROKE_COLOR,
          strokeOpacity: POLYLINE_STROKE_OPACITY,
          strokeStyle: 'solid',
        });
        polyline.setMap(mapRef.current);

        // GPS 위치를 아직 모르므로 일단 코스 전체가 보이게 설정
        const bounds = new kakao.maps.LatLngBounds();
        path.forEach((latLng) => bounds.extend(latLng));
        mapRef.current.setBounds(bounds);
      }

      // 출발 마커
      new kakao.maps.CustomOverlay({
        map: mapRef.current,
        position: startLatLng,
        content: createMarkerContent('출발', '#22C55E'),
        xAnchor: 0.5,
        yAnchor: 0.5,
      });

      // 도착 마커
      new kakao.maps.CustomOverlay({
        map: mapRef.current,
        position: new kakao.maps.LatLng(endLatitude, endLongitude),
        content: createMarkerContent('도착', '#EF4444'),
        xAnchor: 0.5,
        yAnchor: 0.5,
      });

      // 사용자 위치 도트 (초기 위치: 출발지, location 업데이트 시 이동)
      userDotRef.current = new kakao.maps.CustomOverlay({
        map: mapRef.current,
        position: startLatLng,
        content: createUserDotContent('내 위치', '#3B82F6'),
        xAnchor: 0.5,
        yAnchor: 0.5,
        zIndex: 10,
      });
    });

    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [containerRef]);

  // 사용자 위치 변화 시: 도트 이동 + 초기 bounds 조정(1회) + 근접 감지
  useEffect(() => {
    if (!location || !mapRef.current || !userDotRef.current) return;

    const { latitude, longitude } = location;
    const latLng = new kakao.maps.LatLng(latitude, longitude);

    userDotRef.current.setPosition(latLng);

    // 첫 위치 수신 시: 코스 전체 + 사용자 위치가 한 화면에 보이도록 bounds 재조정
    if (!hasInitialBoundsRef.current && coursePathRef.current.length > 0) {
      const bounds = new kakao.maps.LatLngBounds();
      coursePathRef.current.forEach((p) => bounds.extend(p));
      bounds.extend(latLng);
      mapRef.current.setBounds(bounds);
      hasInitialBoundsRef.current = true;
    }

    const distM = haversineM(
      latitude,
      longitude,
      course.startLatitude,
      course.startLongitude
    );
    setIsNearStart(distM <= PROXIMITY_THRESHOLD_M);
  }, [location, course.startLatitude, course.startLongitude]);

  return { isNearStart };
}
