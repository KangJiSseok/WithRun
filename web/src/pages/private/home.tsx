import { useBridgeDataStore, useWatchMessage } from '@/bridge';

export function HomePage() {
  const location = useBridgeDataStore((s) => s.gpsLocation);
  const { watchData } = useWatchMessage();

  return (
    <div>
      <h1 className="text-2xl font-black">모바일 GPS (속도·거리·위치)</h1>
      <p>위도: {location ? location.latitude.toFixed(6) : '-'}</p>
      <p>경도: {location ? location.longitude.toFixed(6) : '-'}</p>
      <p>고도: {location ? `${location.altitude?.toFixed(1)} m` : '-'}</p>
      <p>방향: {location ? `${location.heading?.toFixed(1)}°` : '-'}</p>
      <p>정확도: {location?.accuracy != null ? `${location.accuracy.toFixed(1)} m` : '-'}</p>
      <p>속도 (Doppler): {location ? `${(location.speedMps ?? 0).toFixed(2)} m/s` : '-'}</p>
      <p>케이던스 (가속도계): {location ? `${location.cadenceSpm ?? 0} spm` : '-'}</p>

      <h1 className="text-2xl font-black mt-4">워치 전용 (HR·칼로리)</h1>
      <p>심박수: {watchData ? `${watchData.heartRate} bpm` : '-'}</p>
      <p>칼로리 (HS): {watchData ? `${watchData.caloriesKcal.toFixed(1)} kcal` : '-'}</p>
    </div>
  );
}
