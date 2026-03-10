import { useEffect, useRef } from 'react';

const DEFAULT_CENTER = { lat: 37.5665, lng: 126.978 };

export function useFreeRunMap() {
  // 지도를 그릴 DOM 요소 참조
  const containerRef = useRef<HTMLDivElement>(null);
  // 카카오 Map 인스턴스
  const mapRef = useRef<kakao.maps.Map | null>(null);
  // 현재 위치 마커
  const markerRef = useRef<kakao.maps.Marker | null>(null);

  // 1. 서울 기본 좌표로 카카오 지도 생성
  // 2. 기본 위치에 마커 찍기
  // 3. navigator.geolocation.getCurrentPosition으로
  //    현재 위치 받아와서 지도 중심 이동 및 마커 위치 업데이트
  useEffect(() => {
    if (!containerRef.current) return;

    const container = containerRef.current;

    kakao.maps.load(() => {
      const kakaoCenter = new kakao.maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng);

      mapRef.current = new kakao.maps.Map(container, {
        center: kakaoCenter,
        level: 3,
      });

      markerRef.current = new kakao.maps.Marker({ position: kakaoCenter });
      markerRef.current.setMap(mapRef.current);

      navigator.geolocation.getCurrentPosition(
        ({ coords }) => {
          const latLng = new kakao.maps.LatLng(coords.latitude, coords.longitude);
          mapRef.current?.setCenter(latLng);
          markerRef.current?.setPosition(latLng);
        },
        () => {},
        { enableHighAccuracy: true },
      );
    });
  }, []);

  return { containerRef };
}
