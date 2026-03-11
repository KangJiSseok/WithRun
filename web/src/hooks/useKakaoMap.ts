import { useEffect, useRef } from "react";

export default function useKakaoMap(containerRef: React.RefObject<HTMLDivElement | null>) {
  const mapRef = useRef<kakao.maps.Map | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;
    const options: kakao.maps.MapOptions = {
      center: new window.kakao.maps.LatLng(37.5665, 126.9780),
      level: 3,
    };
    mapRef.current = new window.kakao.maps.Map(containerRef.current, options);
  }, [containerRef]);

  return { mapRef };
}
