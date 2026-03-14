import { bridge } from '@/bridge';
import useWatchMessage from '@/hooks/useWatchMessage';
import { useEffect, useState } from 'react';

type GpsLocation = {
  latitude: number;
  longitude: number;
  accuracy: number | null;
};

export function HomePage() {
  const [location, setLocation] = useState<GpsLocation | null>(null);
  const { lastMessage } = useWatchMessage();

  useEffect(() => {
    const unsubscribe = bridge.addEventListener('gpsLocation', (data) => {
      setLocation(data);
    });
    return unsubscribe;
  }, []);

  return (
    <div>
      <h1>홈 페이지</h1>
      <p>위도: {location ? location.latitude.toFixed(6) : '-'}</p>
      <p>경도: {location ? location.longitude.toFixed(6) : '-'}</p>
      <p>
        정확도:{' '}
        {location?.accuracy != null ? `${location.accuracy.toFixed(1)}m` : '-'}
      </p>
      <p>워치 메시지 : {lastMessage || ''}</p>
    </div>
  );
}
