import { useEffect, useRef } from 'react';

// GPS는 비동기라 응답이 늦게 오기에 먼저 서울 지도를 보여주고,
// GPS 응답이 오면, 실제 위치로 이동하게 해줌.
const DEFAULT_CENTER = { lat: 37.5665, lng: 126.978 };

export function useFreeRunMap() {
  // useRef: 화면을 다시 그리지 않고 값을 저장하는 상자
  // useState랑 다르게 값이 바뀌어도 리렌더링이 안 됨.

  // 지도를 그릴 DOM 요소 참조 (div 태그 안에 지도 그리기 위함)
  const containerRef = useRef<HTMLDivElement>(null);
  // 카카오 Map 인스턴스 (지도의 객체를 저장하고, "지도 중심 이동" 같은 명령을 내릴 때 사용)
  const mapRef = useRef<kakao.maps.Map | null>(null);
  // 현재 위치 마커 (지도 위에 찍힌 마커 객체 저장 ("마커 위치 옮겨" 같은 명령을 내릴 때 사용))
  const markerRef = useRef<kakao.maps.Marker | null>(null);

  // 1. 서울 기본 좌표로 카카오 지도 생성
  // 2. 기본 위치에 마커 찍기
  // 3. navigator.geolocation.getCurrentPosition으로
  //    현재 위치 받아와서 지도 중심 이동 및 마커 위치 업데이트
  // useEffect: 컴포넌트가 화면에 나타난 직후 딱 한 번 실행되는 코드 블록
  useEffect(() => {
    // div 태그가 DOM에 없으면 중단
    if (!containerRef.current) return;

    const container = containerRef.current;

    // 콜백 (카카오 SDK 준비되면 콜백 실행)
    kakao.maps.load(() => {
      // 1. 서울 좌표 지도 생성
      const kakaoCenter = new kakao.maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng);
      // 2. mapRef에 저장
      mapRef.current = new kakao.maps.Map(container, {
        center: kakaoCenter,
        level: 3,
      });
      // 3. 서울 좌표에 마커 찍기
      markerRef.current = new kakao.maps.Marker({ position: kakaoCenter });
      // 4. markerRef에 저장
      markerRef.current.setMap(mapRef.current);
      // 5. GPS에게 비동기 요청 (지금 내 위치 알려주기)
      // [FreeRunScreen 시작화면용] 1회만 위치 가져옴 - 현재 위치 확인용
      // navigator.geolocation.getCurrentPosition(
      //   // coords: getCurrentPosition의 콜백 함수가 받는 GPS응답 객체
      //   // 브라우저가 GPS로 위치를 측정하면, 결과를 coords 객체에 담아서 콜백으로 넘김
      //   // (구조분해한 것)
      //   ({ coords }) => {
      //     const latLng = new kakao.maps.LatLng(coords.latitude, coords.longitude);
      //     mapRef.current?.setCenter(latLng);
      //     markerRef.current?.setPosition(latLng);
      //   },
      //   // 실패 : 서울 그대로 유지 (아무것도 안함)
      //   () => {},
      //   { enableHighAccuracy: true },
      // );

      // [RunScreen 러닝화면용] 위치 변화 감지 - 이동할 때마다 지도/마커 업데이트
      // 페이지 벗어날 때 clearWatch로 반드시 해제해야 함 (배터리 소모 방지)
      const watchId = navigator.geolocation.watchPosition(
        ({ coords }) => {
          const latLng = new kakao.maps.LatLng(coords.latitude, coords.longitude);
          mapRef.current?.setCenter(latLng);
          markerRef.current?.setPosition(latLng);
        },
        () => {},
        // GPS 칩 사용 (배터리 소모량이 많지만, 정확도 높아서 좋음)
        // false로 설정하면 WiFi/기지국 기반이라 오차가 큼
        { enableHighAccuracy: true },
      );

      return () => navigator.geolocation.clearWatch(watchId);
    });
  }, []);

  // 내부 훅인 mapRef, markerRef를 제외한 containerRef은
  // 지도 프레임이기에 <div ref={containerRef}> 로 연결하면 끝
  return { containerRef };
}
