import { useBridgeDataStore, useWatchMessage } from '@/bridge';

export function HomePage() {
  const location = useBridgeDataStore((s) => s.gpsLocation);
  const { watchData } = useWatchMessage();

  return (
    <div>
      <h1 className="text-2xl font-black">모바일 gps 데이터</h1>
      <p>위도: {location ? location.latitude.toFixed(6) : '-'}</p>
      <p>경도: {location ? location.longitude.toFixed(6) : '-'}</p>
      <p>고도: {location ? location.altitude?.toFixed(6) : '-'}</p>
      <p>방향: {location ? location.heading?.toFixed(6) : '-'}</p>
      <p>
        정확도:{' '}
        {location?.accuracy != null ? `${location.accuracy.toFixed(1)}m` : '-'}
      </p>
      <h1 className="text-2xl font-black">워치 헬스 데이터</h1>
      <p>심박수: {watchData ? `${watchData.heartRate} bpm` : '-'}</p>
      <p>속도: {watchData ? `${watchData.speedMps.toFixed(2)} m/s` : '-'}</p>
      <p>거리: {watchData ? `${watchData.distanceM.toFixed(1)} m` : '-'}</p>
      <p>칼로리: {watchData ? `${watchData.caloriesKcal.toFixed(1)} kcal` : '-'}</p>
      <p>고도: {watchData ? `${watchData.altitudeM.toFixed(1)} m` : '-'}</p>
      <p>케이던스: {watchData ? `${watchData.cadenceSpm} spm` : '-'}</p>
    </div>
  );
}
