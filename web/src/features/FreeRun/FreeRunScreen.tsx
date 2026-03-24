import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { FreeRunMap } from './FreeRunMap';
import { RunCharacter } from '@/shared/components';
import { useBridgeDataStore } from '@/bridge';
import { createRunningSession } from '@/apis';

// 회원 시스템 구현 전 임시 mock 유저
const MOCK_USER_ID = 1;

export function FreeRunScreen() {
  const navigate = useNavigate();
  const gpsLocation = useBridgeDataStore((s) => s.gpsLocation);
  const [isStarting, setIsStarting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // state 업데이트 비동기 사이 중복 호출 방지 (동기 가드)
  const calledRef = useRef(false);

  const startSession = async (latitude: number, longitude: number) => {
    setError(null);
    try {
      const res = await createRunningSession({
        userId: MOCK_USER_ID,
        mode: 'FREE',
        startLatitude: latitude,
        startLongitude: longitude,
      });
      navigate('/free-run-session', { state: { runningSessionId: res.data.runningSessionId } });
    } catch {
      setError('서버 연결에 실패했습니다. 다시 시도해주세요.');
    } finally {
      calledRef.current = false;
      setIsStarting(false);
    }
  };

  const handleClick = () => {
    if (calledRef.current) return;
    calledRef.current = true;
    setIsStarting(true);

    if (gpsLocation) {
      startSession(gpsLocation.latitude, gpsLocation.longitude);
      return;
    }

    // Bridge GPS 없으면 브라우저 위치 API로 폴백
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => startSession(coords.latitude, coords.longitude),
      () => {
        setIsStarting(false);
        setError('위치를 가져올 수 없습니다. 위치 권한을 확인해주세요.');
      },
    );
  };

  return (
    <div className="absolute inset-0 overflow-hidden">
      <FreeRunMap />
      <div className="absolute inset-0 flex flex-col items-center justify-center z-sticky">
        {error && (
          <p className="mb-4 body-r text-center text-red-400 px-6">{error}</p>
        )}
        <RunCharacter isRunning={isStarting} onClick={handleClick} />
      </div>
    </div>
  );
}
