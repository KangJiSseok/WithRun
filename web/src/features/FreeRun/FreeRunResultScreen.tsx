import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Award, Home, Activity, Timer, Check, UploadCloud } from 'lucide-react';
import { cn, formatDuration, formatPace } from '@/shared/utils';
import { CourseShareModal } from './CourseShareModal';
import { useGeneratePaceData } from '@/hooks/FreeRun/useGeneratePaceData';
import { useKakaoMap } from '@/shared/hooks/useKakaoMap';
import type { RunningResult } from './FreeRunningScreen';
import { useAuthStore } from '@/stores/Auth';

interface FreeRunResultScreenProps {
  result: RunningResult | null;
  courseId: number | null;
  runningSessionId: number | null;
  onCourseRegistered: (courseId: number) => void;
}

interface StatItemProps {
  label: string;
  value: string;
  unit: string;
}

function StatItem({ label, value, unit }: StatItemProps) {
  return (
    <div className="flex-1 flex flex-col items-center gap-1 bg-[hsl(var(--secondary-2))] rounded-2xl py-4 px-2">
      <span className="caption-r text-[hsl(var(--secondary-5))]">{label}</span>
      <span className="h3-b text-[hsl(var(--text-inverse))]">{value}</span>
      {unit && <span className="caption-r text-[hsl(var(--secondary-5))]">{unit}</span>}
    </div>
  );
}

export function FreeRunResultScreen({
  result,
  courseId,
  runningSessionId,
  onCourseRegistered,
}: FreeRunResultScreenProps) {
  const navigate = useNavigate();
  const nickname = useAuthStore((s) => s.user?.nickname ?? '러너');
  const [showShareModal, setShowShareModal] = useState(false);

  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapCoordinates = (result?.gpsSamples ?? []).map((s) => ({
    latitude: s.latitude,
    longitude: s.longitude,
    elevationM: s.altitudeM ?? 0,
  }));
  useKakaoMap(mapContainerRef, { coordinates: mapCoordinates });

  const durationSec = result?.durationSec ?? 0;
  const distanceM = result?.distanceM ?? 0;
  const caloriesKcal = result?.caloriesKcal ?? 0;
  const avgPaceSecPerKm = result?.avgPaceSecPerKm ?? 0;

  const time = formatDuration(durationSec);
  const distance = (distanceM / 1000).toFixed(2);
  const pace = formatPace(avgPaceSecPerKm);
  const calories = caloriesKcal;

  const { paceData, avgSeconds } = useGeneratePaceData(result?.splits ?? []);

  const heartRates = (result?.healthSamples ?? [])
    .map((s) => s.heartRate)
    .filter((hr) => hr > 0);
  const heartRateStats =
    heartRates.length > 0
      ? {
          min: Math.min(...heartRates),
          avg: Math.round(heartRates.reduce((a, b) => a + b, 0) / heartRates.length),
          max: Math.max(...heartRates),
        }
      : { min: 0, avg: 0, max: 0 };

  return (
    <div className="absolute inset-0 bg-[hsl(var(--secondary-1))]">
      <div className={cn('absolute inset-0 flex flex-col', showShareModal ? 'overflow-hidden' : 'overflow-y-auto')}>
        {/* 헤더 */}
        <div className="relative flex flex-col items-center pt-10 pb-6 px-6">
          <div className="p-4 rounded-full bg-primary/20 border border-primary/30 mb-4">
            <Award size={40} className="text-primary" />
          </div>
          <h1 className="h2-b text-[hsl(var(--text-inverse))] mb-1">수고하셨습니다, {nickname}님!</h1>
        </div>

        {/* 지도 영역 — 러닝 경로 폴리라인 */}
        <div
          ref={mapContainerRef}
          className="mx-6 mb-4 h-40 rounded-2xl overflow-hidden border border-[hsl(var(--secondary-3))]"
        />

        <div className="px-6 space-y-4 pb-8">
          {/* 이동 거리 - 크게 표시 */}
          <div className="bg-[hsl(var(--secondary-2))] rounded-2xl p-6 flex flex-col items-center relative overflow-hidden">
            <div className="absolute top-0 right-0 w-24 h-24 bg-primary/10 rounded-full blur-2xl -mr-6 -mt-6" />
            <span className="text-6xl font-bold text-[hsl(var(--text-inverse))] tabular-nums relative z-base">
              {distance}
            </span>
            <span className="caption-r text-[hsl(var(--secondary-5))] uppercase tracking-widest mt-1 relative z-base">
              킬로미터
            </span>
          </div>

          {/* 시간, 평균 페이스, 칼로리 */}
          <div className="flex gap-3">
            <StatItem label="시간" value={time} unit="" />
            <StatItem label="평균 페이스" value={pace} unit="/km" />
            <StatItem label="칼로리" value={String(calories)} unit="kcal" />
          </div>

          {/* 심박수 변동 */}
          <div className="bg-[hsl(var(--secondary-2))] rounded-2xl p-4">
            <div className="flex items-center gap-2 mb-4">
              <Activity size={16} className="text-primary" />
              <span className="caption-b text-[hsl(var(--text-inverse))]">심박수 변동</span>
            </div>
            <div className="flex gap-3">
              {[
                { label: '최소', value: String(heartRateStats.min) },
                { label: '평균', value: String(heartRateStats.avg), highlight: true },
                { label: '최대', value: String(heartRateStats.max) },
              ].map(({ label, value, highlight }) => (
                <div
                  key={label}
                  className="flex-1 flex flex-col items-center gap-1 bg-[hsl(var(--secondary-3))] rounded-xl py-3"
                >
                  <span className="caption-r text-[hsl(var(--secondary-5))]">{label}</span>
                  <span className={cn('h3-b', highlight ? 'text-primary' : 'text-[hsl(var(--text-inverse))]')}>
                    {value}
                  </span>
                  <span className="caption-r text-[hsl(var(--secondary-5))]">bpm</span>
                </div>
              ))}
            </div>
          </div>

          {/* 구간별 페이스 */}
          <div className="bg-[hsl(var(--secondary-2))] rounded-2xl p-4">
            <div className="flex items-center gap-2 mb-4">
              <Timer size={16} className="text-primary" />
              <span className="caption-b text-[hsl(var(--text-inverse))]">구간별 페이스 (1km)</span>
            </div>
            {paceData.length > 0 ? (
              <div className="rounded-xl overflow-hidden border border-[hsl(var(--secondary-3))]">
                <table className="w-full">
                  <thead className="bg-[hsl(var(--secondary-3))]">
                    <tr>
                      <th className="px-4 py-2 text-left caption-r text-[hsl(var(--secondary-5))] font-normal">구간</th>
                      <th className="px-4 py-2 text-left caption-r text-[hsl(var(--secondary-5))] font-normal">페이스</th>
                      <th className="px-4 py-2 text-left caption-r text-[hsl(var(--secondary-5))] font-normal">상태</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[hsl(var(--secondary-3))]">
                    {paceData.map((item) => (
                      <tr key={item.km}>
                        <td className="px-4 py-3 caption-m text-[hsl(var(--text-inverse))]">{item.km}km</td>
                        <td className="px-4 py-3 caption-m text-primary font-bold">{item.displayPace}</td>
                        <td className="px-4 py-3">
                          <div
                            className={cn(
                              'w-2 h-2 rounded-full',
                              item.seconds < avgSeconds
                                ? 'bg-[hsl(var(--success))]'
                                : 'bg-[hsl(var(--warning))]',
                            )}
                          />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="caption-r text-[hsl(var(--secondary-5))] text-center py-2">
                1km 미만 러닝으로 구간 데이터가 없습니다.
              </p>
            )}
          </div>

          {/* 코스 등록 버튼 */}
          <button
            onClick={() => setShowShareModal(true)}
            disabled={courseId !== null}
            className={cn(
              'w-full py-4 rounded-2xl body-b flex items-center justify-center gap-2 transition-colors',
              courseId !== null
                ? 'bg-[hsl(var(--secondary-4))] text-[hsl(var(--secondary-5))] cursor-default'
                : 'bg-primary/20 border border-primary/30 text-primary',
            )}
          >
            {courseId !== null ? <Check size={18} /> : <UploadCloud size={18} />}
            {courseId !== null ? '등록 완료' : '코스 등록'}
          </button>

          {/* 홈으로 버튼 */}
          <button
            onClick={() => navigate('/free-run')}
            className="w-full py-4 rounded-2xl bg-primary text-[hsl(var(--text-inverse))] body-b flex items-center justify-center gap-2"
          >
            <Home size={18} />
            홈으로
          </button>
        </div>
      </div>

      <CourseShareModal
        isOpen={showShareModal}
        onClose={() => setShowShareModal(false)}
        runningSessionId={runningSessionId}
        onRegistered={onCourseRegistered}
      />
    </div>
  );
}
