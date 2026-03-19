import { useEffect, useRef } from 'react';

import type { CoordinatePoint } from '@/apis';

const DEFAULT_CENTER = { lat: 37.5665, lng: 126.978 };
// 폴리라인 스타일 상수
const POLYLINE_STROKE_WEIGHT = 5;
const POLYLINE_STROKE_COLOR = '#4F80FF';
const POLYLINE_STROKE_OPACITY = 0.9;

type UseKakaoMapOptions = {
  // 코스 경로 좌표 (전달 시 폴리라인 렌더링 + 코스 중심으로 지도 이동)
  coordinates?: CoordinatePoint[];
};

export function useKakaoMap(
  containerRef: React.RefObject<HTMLDivElement | null>,
  options: UseKakaoMapOptions = {}
) {
  const mapRef = useRef<kakao.maps.Map | null>(null);
  const polylineRef = useRef<kakao.maps.Polyline | null>(null);

  // 지도 초기화
  useEffect(() => {
    if (!containerRef.current) return;

    const container = containerRef.current;

    kakao.maps.load(() => {
      const center = new kakao.maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng);
      mapRef.current = new kakao.maps.Map(container, { center, level: 3 });
    });
  }, [containerRef]);

  // 폴리라인 렌더링
  // coordinates가 전달되면:
  // 1. 좌표 배열을 kakao.maps.LatLng 객체 배열로 변환
  // 2. 기존 폴리라인이 있으면 지도에서 제거 후 새로 생성
  // 3. 경로 전체가 화면에 보이도록 LatLngBounds로 지도 범위 자동 조정
  useEffect(() => {
    const { coordinates } = options;
    if (!mapRef.current || !coordinates || coordinates.length === 0) return;

    // 1. CoordinatePoint[] → kakao.maps.LatLng[]
    const path = coordinates.map(
      (c) => new kakao.maps.LatLng(c.latitude, c.longitude)
    );

    // 2. 기존 폴리라인 제거
    if (polylineRef.current) {
      polylineRef.current.setMap(null);
    }

    // 3. 새 폴리라인 생성 후 지도에 등록
    polylineRef.current = new kakao.maps.Polyline({
      path,
      strokeWeight: POLYLINE_STROKE_WEIGHT,
      strokeColor: POLYLINE_STROKE_COLOR,
      strokeOpacity: POLYLINE_STROKE_OPACITY,
      strokeStyle: 'solid',
    });
    polylineRef.current.setMap(mapRef.current);

    // 4. 경로 전체가 보이도록 bounds 설정
    // LatLngBounds: 여러 좌표를 포함하는 최소 직사각형 영역
    const bounds = new kakao.maps.LatLngBounds();
    path.forEach((latLng) => bounds.extend(latLng));
    mapRef.current.setBounds(bounds);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [options.coordinates]);

  return { mapRef };
}
