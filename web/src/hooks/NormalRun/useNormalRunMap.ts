import { useEffect, useRef } from 'react';

import type { CoordinatePoint } from '@/apis';
import { useNormalRunLocation } from './useNormalRunLocation';

const DEFAULT_CENTER = { lat: 37.5665, lng: 126.978 };

const POLYLINE_STROKE_WEIGHT = 5;
const POLYLINE_STROKE_COLOR = '#4F80FF';
const POLYLINE_STROKE_OPACITY = 0.9;

function createUserDotContent(): HTMLElement {
  const dot = document.createElement('div');
  dot.style.cssText = `
    width: 16px;
    height: 16px;
    background: #3B82F6;
    border: 2.5px solid white;
    border-radius: 50%;
    box-shadow: 0 0 0 6px rgba(59,130,246,0.2), 0 2px 4px rgba(0,0,0,0.3);
  `;
  return dot;
}

export function useNormalRunMap(
  containerRef: React.RefObject<HTMLDivElement | null>,
  params: {
    coordinates: CoordinatePoint[];
    courseAnchor: { latitude: number; longitude: number } | null;
  }
): void {
  const { location } = useNormalRunLocation();
  const mapRef = useRef<kakao.maps.Map | null>(null);
  const userDotRef = useRef<kakao.maps.CustomOverlay | null>(null);
  const safeZoneCircleRef = useRef<kakao.maps.Circle | null>(null);

  // 지도 초기화 + 폴리라인 + 사용자 도트
  useEffect(() => {
    if (!containerRef.current) return;

    const container = containerRef.current;
    const { coordinates } = params;

    kakao.maps.load(() => {
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

      // 사용자 위치 도트
      userDotRef.current = new kakao.maps.CustomOverlay({
        map: mapRef.current,
        position: center,
        content: createUserDotContent(),
        xAnchor: 0.5,
        yAnchor: 0.5,
        zIndex: 10,
      });
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [containerRef]);

  // 사용자 위치 변화 시 도트 이동
  useEffect(() => {
    if (!location || !userDotRef.current) return;

    const latLng = new kakao.maps.LatLng(location.latitude, location.longitude);
    userDotRef.current.setPosition(latLng);

    // 지도 중심도 현재 위치로 부드럽게 이동
    mapRef.current?.panTo(latLng);
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
