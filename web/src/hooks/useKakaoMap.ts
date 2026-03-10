import { useEffect, useRef } from "react";

export default function useKakaoMap() {
  useEffect(() => {
    const mapRef = useRef<kakao.maps.Map | null>(null);
    mapRef.current = new window.kakao.maps.Map();
  }, []);
  return {};
}
