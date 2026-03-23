import { Activity, Award, Bookmark, Home, MapPin, Timer } from 'lucide-react';
import { NormalRunResultMap } from './NormalRunResultMap';
import { useNavigate } from 'react-router-dom';

import { cn } from '@/shared/utils';
import { useNormalRunSessionStore } from '@/stores/NormalRun';
import type { CompleteRunningSessionResponse, HealthSample } from '@/apis/runningSessions';

function formatDuration(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0) {
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

function formatPace(secPerKm: number): string {
  const m = Math.floor(secPerKm / 60);
  const s = secPerKm % 60;
  return `${m}'${String(s).padStart(2, '0')}"`;
}

function HeartRateSection({ samples }: { samples: HealthSample[] }) {
  const rates = samples.map((s) => s.heartRate).filter((r) => r > 0);

  if (rates.length === 0) return null;

  const min = Math.min(...rates);
  const max = Math.max(...rates);
  const avg = Math.round(rates.reduce((a, b) => a + b, 0) / rates.length);

  // SVG 폴리라인 포인트 계산
  const W = 300;
  const H = 64;
  const PAD = 4;
  const points =
    rates.length >= 2
      ? rates
          .map((hr, i) => {
            const x = PAD + (i / (rates.length - 1)) * (W - PAD * 2);
            const y = H - PAD - ((hr - min) / (max - min || 1)) * (H - PAD * 2);
            return `${x.toFixed(1)},${y.toFixed(1)}`;
          })
          .join(' ')
      : null;

  return (
    <div className="bg-[hsl(var(--secondary-2))] rounded-2xl p-4">
      <div className="flex items-center gap-2 mb-4">
        <Activity size={16} className="text-primary" />
        <span className="caption-b text-[hsl(var(--text-inverse))]">심박수 변동</span>
      </div>

      {/* min / avg / max 카드 */}
      <div className="flex gap-3 mb-4">
        {[
          { label: '최소', value: String(min), highlight: false },
          { label: '평균', value: String(avg), highlight: true },
          { label: '최대', value: String(max), highlight: false },
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

      {/* SVG 라인 차트 */}
      {points && (
        <svg
          viewBox={`0 0 ${W} ${H}`}
          className="w-full"
          style={{ height: 64 }}
          preserveAspectRatio="none"
        >
          <polyline
            points={points}
            fill="none"
            stroke="hsl(var(--primary))"
            strokeWidth="2"
            strokeLinejoin="round"
            strokeLinecap="round"
          />
        </svg>
      )}
    </div>
  );
}

type NormalRunResultScreenProps = {
  result: CompleteRunningSessionResponse;
};

export function NormalRunResultScreen({ result }: NormalRunResultScreenProps) {
  const navigate = useNavigate();
  const clearAll = useNormalRunSessionStore((s) => s.clearAll);

  const handleHome = () => {
    clearAll();
    navigate('/normal-run');
  };

  const avgSplitSec =
    result.splits.length > 0
      ? result.splits.reduce((sum, s) => sum + s.splitPaceSecPerKm, 0) / result.splits.length
      : 0;

  return (
    <div className="absolute inset-0 bg-[hsl(var(--secondary-1))] overflow-y-auto">
      <div className="flex flex-col gap-4 px-6 pb-10">
        {/* 헤더 */}
        <div className="relative flex flex-col items-center pt-10 pb-2">
          <button
            className="absolute top-10 right-0 p-2"
            aria-label="즐겨찾기 추가 (준비 중)"
            disabled
          >
            <Bookmark size={26} className="text-[hsl(var(--secondary-5))]" />
          </button>
          <div className="p-4 rounded-full bg-primary/20 border border-primary/30 mb-4">
            <Award size={40} className="text-primary" />
          </div>
          <h1 className="h2-b text-[hsl(var(--text-inverse))] mb-1">러닝 완료!</h1>
          <p className="body-r text-[hsl(var(--secondary-5))]">수고하셨습니다</p>
        </div>

        {/* 지도 영역 — GPS 샘플 2개 이상일 때 경로 표시 */}
        <div className="h-40 rounded-2xl overflow-hidden border border-[hsl(var(--secondary-3))]">
          {result.gpsSamples.length >= 2 ? (
            <NormalRunResultMap gpsSamples={result.gpsSamples} />
          ) : (
            <div className="h-full bg-[hsl(var(--secondary-2))] flex items-center justify-center">
              <div className="flex flex-col items-center gap-2 text-[hsl(var(--secondary-5))]">
                <MapPin size={24} />
                <span className="caption-r">GPS 데이터 없음</span>
              </div>
            </div>
          )}
        </div>

        {/* 거리 */}
        <div className="bg-[hsl(var(--secondary-2))] rounded-2xl p-6 flex flex-col items-center relative overflow-hidden">
          <div className="absolute top-0 right-0 w-24 h-24 bg-primary/10 rounded-full blur-2xl -mr-6 -mt-6" />
          <span className="text-6xl font-bold text-[hsl(var(--text-inverse))] tabular-nums relative z-base">
            {(result.distanceM / 1000).toFixed(2)}
          </span>
          <span className="caption-r text-[hsl(var(--secondary-5))] uppercase tracking-widest mt-1 relative z-base">
            킬로미터
          </span>
        </div>

        {/* 시간 + 평균 페이스 */}
        <div className="flex gap-3">
          <div className="flex-1 flex flex-col items-center gap-1 bg-[hsl(var(--secondary-2))] rounded-2xl py-4 px-2">
            <span className="caption-r text-[hsl(var(--secondary-5))]">시간</span>
            <span className="h3-b text-[hsl(var(--text-inverse))] tabular-nums">
              {formatDuration(result.durationSec)}
            </span>
          </div>
          <div className="flex-1 flex flex-col items-center gap-1 bg-[hsl(var(--secondary-2))] rounded-2xl py-4 px-2">
            <span className="caption-r text-[hsl(var(--secondary-5))]">평균 페이스</span>
            <span className="h3-b text-[hsl(var(--text-inverse))] tabular-nums">
              {formatPace(result.avgPaceSecPerKm)}
            </span>
          </div>
        </div>

        {/* 심박수 변동 */}
        <HeartRateSection samples={result.healthSamples} />

        {/* 구간별 페이스 */}
        {result.splits.length > 0 && (
          <div className="bg-[hsl(var(--secondary-2))] rounded-2xl p-4">
            <div className="flex items-center gap-2 mb-4">
              <Timer size={16} className="text-primary" />
              <span className="caption-b text-[hsl(var(--text-inverse))]">구간별 페이스 (1km)</span>
            </div>
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
                  {result.splits.map((split) => (
                    <tr key={split.splitIndex}>
                      <td className="px-4 py-3 caption-m text-[hsl(var(--text-inverse))]">
                        {split.splitIndex}km
                      </td>
                      <td className="px-4 py-3 caption-m text-primary font-bold">
                        {formatPace(split.splitPaceSecPerKm)}
                      </td>
                      <td className="px-4 py-3">
                        <div
                          className={cn(
                            'w-2 h-2 rounded-full',
                            split.splitPaceSecPerKm < avgSplitSec
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
          </div>
        )}

        {/* 버튼 영역 */}
        <div className="flex flex-col gap-3 pt-2">
          <button
            disabled
            className="w-full py-4 rounded-2xl body-b flex items-center justify-center gap-2 bg-[hsl(var(--secondary-3))] text-[hsl(var(--secondary-5))] cursor-not-allowed"
          >
            <Bookmark size={18} />
            즐겨찾기 추가
          </button>
          <button
            onClick={handleHome}
            className="w-full py-4 rounded-2xl bg-primary text-[hsl(var(--text-inverse))] body-b flex items-center justify-center gap-2"
          >
            <Home size={18} />
            홈
          </button>
        </div>
      </div>
    </div>
  );
}
