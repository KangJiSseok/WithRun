import { X, Activity, Timer, MapPin } from 'lucide-react';
import { useRef } from 'react';
import { useNavigate } from 'react-router-dom';

import { cn } from '@/shared/utils';
import { formatDuration, formatPace, formatStartTime } from '@/shared/utils';
import { useNormalRunResultMap } from '@/hooks/NormalRun/useNormalRunResultMap';
import type { RunningSessionDetailResponse } from '@/apis/runningSessions';
import type { GpsSample, HealthSample, SplitResult } from '@/apis/runningSessions';

// GPS 경로 지도 (div 하나 만들고 ref 연결)
function DetailMap({ gpsSamples }: { gpsSamples: GpsSample[] }) {
  // div 가리키는 ref 만들기 (지도 라이브러리에서 이 div에 지도를 그릴 예정)
  const containerRef = useRef<HTMLDivElement>(null);
  useNormalRunResultMap(containerRef, gpsSamples);
  // 빈 div 반환 (지도 라이브러리가 이 div에 지도를 그릴 예정)
  return <div ref={containerRef} className="w-full h-full" />;
}

// 심박수 폴리라인 섹션
function HeartRateSection({ samples }: { samples: HealthSample[] }) {
  // healthSamples에서 heartRate만 추출해서 배열로 만들기 (0 이하 값은 제외)
  const rates = samples.map((s) => s.heartRate).filter((r) => r > 0);
  // 워치 연동 안되어있거나, 심박수 데이터 없으면 null 반환해서 이 섹션 자체를 숨김
  if (rates.length === 0) return null;

  const min = Math.min(...rates);
  const max = Math.max(...rates);
  const avg = Math.round(rates.reduce((a, b) => a + b, 0) / rates.length);

  // SVG 좌표 변환
  const W = 300;
  const H = 64;
  const PAD = 4;
  const points =
    rates.length >= 2
      ? rates
          .map((hr, i) => {
            // x: 시간축 (왼쪽 -> 오른쪽, 균등 간격)
            const x = PAD + (i / (rates.length - 1)) * (W - PAD * 2);
            // y: 심박수축 (높을수록 위쪽)
            // SVG는 y가 위 -> 아래라 H에서 빼기 
            const y = H - PAD - ((hr - min) / (max - min || 1)) * (H - PAD * 2);
            return `${x.toFixed(1)},${y.toFixed(1)}`;
          })
          .join(' ')
      : null;

  return (
    <div className="flex flex-col gap-[var(--space-3)] rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-[var(--space-4)]">
      <div className="flex items-center gap-[var(--space-2)]">
        <Activity size={16} className="text-[hsl(var(--primary))]" />
        <span className="caption-b text-[hsl(var(--text-primary))]">심박수 변동</span>
      </div>
      <div className="flex gap-[var(--space-3)]">
        {[
          { label: '최소', value: String(min), highlight: false },
          { label: '평균', value: String(avg), highlight: true },
          { label: '최대', value: String(max), highlight: false },
        ].map(({ label, value, highlight }) => (
          <div
            key={label}
            className="flex-1 flex flex-col items-center gap-[var(--space-1)] bg-[hsl(var(--surface-subtle))] rounded-[var(--radius-lg)] py-[var(--space-3)]"
          >
            <span className="caption-r text-[hsl(var(--text-secondary))]">{label}</span>
            <span className={cn('h3-b', highlight ? 'text-[hsl(var(--primary))]' : 'text-[hsl(var(--text-primary))]')}>
              {value}
            </span>
            <span className="caption-r text-[hsl(var(--text-secondary))]">bpm</span>
          </div>
        ))}
      </div>
      {points && (
        <svg viewBox={`0 0 ${W} ${H}`} className="w-full" style={{ height: 64 }} preserveAspectRatio="none">
          {/* polyline으로 연결 */}
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

// 구간별 페이스 섹션
// SplitResult 타입의 배열을 받음
// splitIndex -> 몇 번째 1km 구간인지 (1부터 시작)
// splitPaceSecPerKm -> 해당 구간의 페이스 (초/킬로미터)
function SplitsSection({ splits }: { splits: SplitResult[] }) {
  // 구간 데이터 없으면 섹션 전체 숨김
  if (splits.length === 0) return null;
  // 평균 페이스 계산 (전체 구간의 페이스 평균) - 각 구간의 페이스 합 / 구간 수
  const avgSplitSec =
    splits.reduce((sum, s) => sum + s.splitPaceSecPerKm, 0) / splits.length;

  return (
    <div className="flex flex-col gap-[var(--space-3)] rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-[var(--space-4)]">
      <div className="flex items-center gap-[var(--space-2)]">
        <Timer size={16} className="text-[hsl(var(--primary))]" />
        <span className="caption-b text-[hsl(var(--text-primary))]">구간별 페이스 (1km)</span>
      </div>
      <div className="rounded-[var(--radius-lg)] overflow-hidden border border-[hsl(var(--border-default))]">
        <table className="w-full">
          <thead className="bg-[hsl(var(--surface-subtle))]">
            <tr>
              <th className="px-[var(--space-3)] py-[var(--space-2)] text-left caption-r text-[hsl(var(--text-secondary))] font-normal">구간</th>
              <th className="px-[var(--space-3)] py-[var(--space-2)] text-left caption-r text-[hsl(var(--text-secondary))] font-normal">페이스</th>
              <th className="px-[var(--space-3)] py-[var(--space-2)] text-left caption-r text-[hsl(var(--text-secondary))] font-normal">상태</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[hsl(var(--border-default))]">
            {splits.map((split) => (
              <tr key={split.splitIndex}>
                <td className="px-[var(--space-3)] py-[var(--space-3)] caption-m text-[hsl(var(--text-primary))]">
                  {/* 구간이 1km 미만이면 미터 단위로 표시 */}
                  {split.splitDistanceM < 1000
                    ? `${split.splitDistanceM}m`
                    : `${split.splitIndex}km`}
                </td>
                <td className="px-[var(--space-3)] py-[var(--space-3)] caption-m text-[hsl(var(--primary))] font-bold">
                  {formatPace(split.splitPaceSecPerKm)}
                </td>
                <td className="px-[var(--space-3)] py-[var(--space-3)]">
                  <div
                    className={cn(
                      'w-2 h-2 rounded-full',
                      split.splitPaceSecPerKm < avgSplitSec
                        // 평균 페이스보다 빠르면 초록, 느리면 노랑
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
  );
}

type CalendarDetailScreenProps = {
  detail: RunningSessionDetailResponse;
};

export function CalendarDetailScreen({ detail }: CalendarDetailScreenProps) {
  const navigate = useNavigate();

  // "2026-03-12T06:30:00" → "2026년 3월 12일"
  const dateLabel = (() => {
    const parts = detail.startedAt.slice(0, 10).split('-');
    return `${parts[0]}년 ${Number(parts[1])}월 ${Number(parts[2])}일`;
  })();

  // "2026-03-12T06:30:00" → "06:30"
  const timeLabel = formatStartTime(detail.startedAt);

  return (
    <div className="absolute inset-0 bg-[hsl(var(--background))] overflow-y-auto">
      <div className="flex flex-col gap-[var(--space-4)] px-[var(--space-5)] pb-[var(--space-10)]">
        {/* 헤더 */}
        <div className="flex flex-col gap-[var(--space-2)] pt-[var(--space-5)]">
          {/* X 버튼 라인 */}
          <div className="flex justify-end">
            <button
              type="button"
              onClick={() => navigate('/calendar')}
              className="w-8 h-8 flex items-center justify-center rounded-full bg-[hsl(var(--surface-subtle))]"
              aria-label="닫기"
            >
              <X size={18} className="text-[hsl(var(--text-primary))]" />
            </button>
          </div>
          {/* 날짜 + 시간 */}
          <div className="flex flex-col">
            <span className="body-b text-[hsl(var(--text-primary))]">{dateLabel}</span>
            <span className="caption-r text-[hsl(var(--text-secondary))]">{timeLabel}</span>
          </div>
        </div>

        {/* 지도 */}
        <div className="h-40 rounded-[var(--radius-xl)] overflow-hidden border border-[hsl(var(--border-default))]">
          {detail.gpsSamples.length >= 2 ? (
            <DetailMap gpsSamples={detail.gpsSamples} />
          ) : (
            <div className="h-full bg-[hsl(var(--surface-subtle))] flex items-center justify-center">
              <div className="flex flex-col items-center gap-[var(--space-2)] text-[hsl(var(--text-secondary))]">
                <MapPin size={24} />
                <span className="caption-r">GPS 데이터 없음</span>
              </div>
            </div>
          )}
        </div>

        {/* 거리 */}
        <div className="rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-[var(--space-6)] flex flex-col items-center">
          <span className="text-6xl font-bold text-[hsl(var(--text-primary))] tabular-nums">
            {(detail.distanceM / 1000).toFixed(2)}
          </span>
          <span className="caption-r text-[hsl(var(--text-secondary))] uppercase tracking-widest mt-1">
            킬로미터
          </span>
        </div>

        {/* 시간 + 페이스 */}
        <div className="flex gap-[var(--space-3)]">
          <div className="flex-1 flex flex-col items-center gap-[var(--space-1)] rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] py-[var(--space-4)] px-[var(--space-2)]">
            <span className="caption-r text-[hsl(var(--text-secondary))]">시간</span>
            <span className="h3-b text-[hsl(var(--text-primary))] tabular-nums">
              {formatDuration(detail.durationSec)}
            </span>
          </div>
          <div className="flex-1 flex flex-col items-center gap-[var(--space-1)] rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] py-[var(--space-4)] px-[var(--space-2)]">
            <span className="caption-r text-[hsl(var(--text-secondary))]">평균 페이스</span>
            <span className="h3-b text-[hsl(var(--text-primary))] tabular-nums">
              {formatPace(detail.avgPaceSecPerKm)}
            </span>
          </div>
        </div>

        {/* 칼로리 + 고도 */}
        <div className="flex gap-[var(--space-3)]">
          <div className="flex-1 flex flex-col items-center gap-[var(--space-1)] rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] py-[var(--space-4)] px-[var(--space-2)]">
            <span className="caption-r text-[hsl(var(--text-secondary))]">칼로리</span>
            <span className="h3-b text-[hsl(var(--text-primary))] tabular-nums">
              {detail.caloriesKcal}
            </span>
            <span className="caption-r text-[hsl(var(--text-secondary))]">kcal</span>
            {detail.caloriesKcal === 0 && (
              <span className="caption-r text-[hsl(var(--text-secondary))]">워치 미연동</span>
            )}
          </div>
          <div className="flex-1 flex flex-col items-center gap-[var(--space-1)] rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] py-[var(--space-4)] px-[var(--space-2)]">
            <span className="caption-r text-[hsl(var(--text-secondary))]">누적 고도</span>
            <span className="h3-b text-[hsl(var(--text-primary))] tabular-nums">
              {detail.elevationGainM}
            </span>
            <span className="caption-r text-[hsl(var(--text-secondary))]">m</span>
          </div>
        </div>

        {/* 심박수 변동 */}
        <HeartRateSection samples={detail.healthSamples} />

        {/* 구간별 페이스 */}
        <SplitsSection splits={detail.splits} />
      </div>
    </div>
  );
}
