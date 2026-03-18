import { useEffect, useState } from 'react';
import { useBridgeDataStore } from '@/bridge';

// 워치 연결 타임아웃 (3초 이상 메시지 없으면 미연결)
const WATCH_TIMEOUT_MS = 3000;

export function HomePage() {
  const location = useBridgeDataStore((s) => s.gpsLocation);
  const watchData = useBridgeDataStore((s) => s.watchHealthData);
  const lastWatchMessageAt = useBridgeDataStore((s) => s.lastWatchMessageAt);

  const [isWatchConnected, setIsWatchConnected] = useState(false);

  useEffect(() => {
    const check = () => {
      setIsWatchConnected(
        lastWatchMessageAt !== null && Date.now() - lastWatchMessageAt < WATCH_TIMEOUT_MS,
      );
    };
    check();
    const id = setInterval(check, 1000);
    return () => clearInterval(id);
  }, [lastWatchMessageAt]);

  // 케이던스 우선순위: 워치 연결 시 Samsung Health SPM, 미연결 시 모바일 가속도계
  const cadenceSpm = isWatchConnected
    ? (watchData?.cadenceSpm ?? location?.cadenceSpm ?? 0)
    : (location?.cadenceSpm ?? 0);
  const cadenceSource = isWatchConnected ? '워치(HS)' : '모바일(가속도계)';

  // 거리 우선순위: 워치 연결 + distanceM > 0 이면 워치 만보계, 아니면 '-'
  const watchDistanceM =
    isWatchConnected && watchData && watchData.distanceM > 0 ? watchData.distanceM : null;

  const src = (label: string) => (
    <span className="text-xs text-gray-400 ml-1">[{label}]</span>
  );

  return (
    <div className="p-4 space-y-6 text-sm">

      {/* 워치 연결 상태 */}
      <div className="flex items-center gap-2">
        <div className={`w-3 h-3 rounded-full ${isWatchConnected ? 'bg-green-500' : 'bg-gray-400'}`} />
        <span className="font-semibold">
          {isWatchConnected ? '워치 연결됨' : '워치 미연결 (모바일 폴백)'}
        </span>
      </div>

      {/* 모바일 GPS */}
      <section>
        <h2 className="text-base font-black mb-2">모바일 GPS</h2>
        <div className="space-y-1">
          <p>위도: {location ? location.latitude.toFixed(6) : '-'}</p>
          <p>경도: {location ? location.longitude.toFixed(6) : '-'}</p>
          <p>고도: {location ? `${location.altitude?.toFixed(1)} m` : '-'}</p>
          <p>방향: {location ? `${location.heading?.toFixed(1)}°` : '-'}</p>
          <p>정확도: {location?.accuracy != null ? `${location.accuracy.toFixed(1)} m` : '-'}</p>
          <p>속도: {location ? `${(location.speedMps ?? 0).toFixed(2)} m/s` : '-'}</p>
          <p>케이던스 (가속도계): {location?.cadenceSpm ?? 0} spm</p>
        </div>
      </section>

      {/* 워치 전용 (HR·칼로리) */}
      <section>
        <h2 className="text-base font-black mb-2">워치 원시 데이터</h2>
        <div className="space-y-1">
          <p>심박수: {watchData ? `${watchData.heartRate} bpm` : '-'}{src('워치 PPG')}</p>
          <p>칼로리: {watchData ? `${watchData.caloriesKcal.toFixed(1)} kcal` : '-'}{src('워치 HS')}</p>
          <p>케이던스: {watchData ? `${watchData.cadenceSpm} spm` : '-'}{src('워치 HS')}</p>
          <p>거리: {watchData && watchData.distanceM > 0 ? `${(watchData.distanceM / 1000).toFixed(3)} km` : '-'}{src('워치 만보계')}</p>
        </div>
      </section>

      {/* 우선순위 적용 지표 */}
      <section>
        <h2 className="text-base font-black mb-2">우선순위 적용</h2>
        <div className="space-y-1">
          <p>
            케이던스: {cadenceSpm} spm
            {src(cadenceSource)}
          </p>
          <p>
            거리: {watchDistanceM !== null
              ? `${(watchDistanceM / 1000).toFixed(3)} km`
              : 'GPS 기반 (FreeRun 화면에서 확인)'}
            {watchDistanceM !== null && src('워치 만보계')}
          </p>
        </div>
      </section>

    </div>
  );
}
