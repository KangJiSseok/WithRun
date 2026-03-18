import { useEffect, useRef } from 'react';

const DEFAULT_CENTER = { lat: 37.5665, lng: 126.978 };

export function useKakaoMap(containerRef: React.RefObject<HTMLDivElement | null>) {
  const mapRef = useRef<kakao.maps.Map | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;

    const container = containerRef.current;

    kakao.maps.load(() => {
      const center = new kakao.maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng);
      mapRef.current = new kakao.maps.Map(container, { center, level: 3 });
    });
  }, [containerRef]);

  return { mapRef };
}
