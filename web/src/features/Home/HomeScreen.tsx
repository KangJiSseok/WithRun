import { useEffect, useMemo, useState } from 'react';
import { HeartPulse, Route, Settings2 } from 'lucide-react';
import { toast } from 'sonner';

import type {
  Difficulty,
  Purpose,
  TimeSlot,
  UserRunningPreferenceResponse,
} from '@/apis/RunningPreferences';
import {
  getUserRunningPreferences,
  updateUserRunningPreferences,
} from '@/apis/RunningPreferences';
import {
  DEFAULT_ONBOARDING_FORM_STATE,
  ONBOARDING_COURSE_TYPE_OPTIONS,
  ONBOARDING_DIFFICULTY_OPTIONS,
  ONBOARDING_PURPOSE_OPTIONS,
  ONBOARDING_TIME_SLOT_OPTIONS,
} from '@/constants/Onboarding';
import { useAuthStore } from '@/stores/Auth';
import type { CourseType } from '@/shared/types/api.type';
import { cn } from '@/shared/utils';
import { getWeeklyCalendarApi } from '@/apis/userCalendar';
import { useHealthConnectionStatus } from './useHealthConnectionStatus';

type RunningPreferenceFormState = {
  purposes: Purpose[];
  timeSlots: TimeSlot[];
  preferredDistanceKm: number;
  preferredDifficulty: Difficulty | null;
  courseTypes: CourseType[];
};

type PreferenceChipGroupProps<TValue extends string> = {
  label: string;
  options: ReadonlyArray<{
    label: string;
    value: TValue;
  }>;
  selectedValues: TValue[];
  onToggle: (value: TValue) => void;
  className?: string;
};

type SingleSelectChipGroupProps<TValue extends string> = {
  label: string;
  options: ReadonlyArray<{
    label: string;
    value: TValue;
  }>;
  selectedValue: TValue | null;
  onSelect: (value: TValue) => void;
};

type SummaryCardProps = {
  title: string;
  value: string;
  unit: string;
};

function createDefaultPreferenceState(): RunningPreferenceFormState {
  return {
    purposes: DEFAULT_ONBOARDING_FORM_STATE.purposes,
    timeSlots: DEFAULT_ONBOARDING_FORM_STATE.timeSlots,
    preferredDistanceKm: DEFAULT_ONBOARDING_FORM_STATE.preferredDistanceKm,
    preferredDifficulty: DEFAULT_ONBOARDING_FORM_STATE.preferredDifficulty,
    courseTypes: DEFAULT_ONBOARDING_FORM_STATE.courseTypes,
  };
}

function createPreferenceState(
  response: UserRunningPreferenceResponse
): RunningPreferenceFormState {
  return {
    purposes: response.purposes,
    timeSlots: response.timeSlots,
    preferredDistanceKm: response.preferredDistanceKm ?? 5,
    preferredDifficulty: response.preferredDifficulty,
    courseTypes: response.courseTypes,
  };
}

function toggleSelection<TValue extends string>(
  selectedValues: TValue[],
  value: TValue
): TValue[] {
  return selectedValues.includes(value)
    ? selectedValues.filter((currentValue) => currentValue !== value)
    : [...selectedValues, value];
}

function formatDuration(durationSec: number): string {
  const hours = Math.floor(durationSec / 3600);
  const minutes = Math.floor((durationSec % 3600) / 60);
  const seconds = durationSec % 60;
  return `${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

function PreferenceChipGroup<TValue extends string>({
  label,
  options,
  selectedValues,
  onToggle,
  className,
}: PreferenceChipGroupProps<TValue>) {
  return (
    <section className="space-y-3">
      <h3 className="text-sm font-semibold text-[hsl(var(--text-secondary))]">
        {label}
      </h3>
      <div className={cn('flex flex-wrap gap-2', className)}>
        {options.map((option) => {
          const isSelected = selectedValues.includes(option.value);

          return (
            <button
              key={option.value}
              type="button"
              onClick={() => onToggle(option.value)}
              className={cn(
                'rounded-full border px-4 py-2 text-sm font-semibold transition-colors',
                isSelected
                  ? 'border-[hsl(var(--primary-1))] bg-[hsl(var(--primary-1))] text-[hsl(var(--text-inverse))]'
                  : 'border-[hsl(var(--border-default))] bg-[hsl(var(--surface-subtle))] text-[hsl(var(--text-secondary))]'
              )}
            >
              {option.label}
            </button>
          );
        })}
      </div>
    </section>
  );
}

function SingleSelectChipGroup<TValue extends string>({
  label,
  options,
  selectedValue,
  onSelect,
}: SingleSelectChipGroupProps<TValue>) {
  return (
    <section className="space-y-3">
      <h3 className="text-sm font-semibold text-[hsl(var(--text-secondary))]">
        {label}
      </h3>
      <div className="grid grid-cols-3 gap-2">
        {options.map((option) => {
          const isSelected = selectedValue === option.value;

          return (
            <button
              key={option.value}
              type="button"
              onClick={() => onSelect(option.value)}
              className={cn(
                'rounded-3xl border px-4 py-4 text-base font-semibold transition-colors',
                isSelected
                  ? 'border-[hsl(var(--primary-1))] bg-[hsl(var(--primary-1))] text-[hsl(var(--text-inverse))]'
                  : 'border-[hsl(var(--border-default))] bg-[hsl(var(--surface-subtle))] text-[hsl(var(--text-secondary))]'
              )}
            >
              {option.label}
            </button>
          );
        })}
      </div>
    </section>
  );
}

function SummaryCard({ title, value, unit }: SummaryCardProps) {
  return (
    <article className="rounded-[1.75rem] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-5 shadow-[var(--shadow-md)]">
      <div className="flex items-end gap-2">
        <span className="text-3xl font-black leading-none tracking-tight text-[hsl(var(--text-primary))]">
          {value}
        </span>
        <span className="pb-1 text-base font-medium text-[hsl(var(--text-secondary))]">
          {unit}
        </span>
      </div>
      <p className="mt-3 text-sm font-medium text-[hsl(var(--text-secondary))]">
        {title}
      </p>
    </article>
  );
}

export function HomeScreen() {
  const user = useAuthStore((state) => state.user);
  const { label: healthConnectionLabel, isConnected } =
    useHealthConnectionStatus();
  const [weeklySummary, setWeeklySummary] = useState<{
    runCount: number;
    distanceKm: number;
    caloriesKcal: number;
    durationSec: number;
  } | null>(null);
  const [preferences, setPreferences] = useState<RunningPreferenceFormState>(
    createDefaultPreferenceState()
  );
  const [isLoadingPreferences, setIsLoadingPreferences] = useState(true);
  const [isSavingPreferences, setIsSavingPreferences] = useState(false);

  const summaryCards = useMemo(
    () => [
      {
        title: '이번 주 러닝',
        value: weeklySummary ? String(weeklySummary.runCount) : '--',
        unit: '회',
      },
      {
        title: '총 거리',
        value: weeklySummary ? weeklySummary.distanceKm.toFixed(1) : '--',
        unit: 'km',
      },
      {
        title: '소모 칼로리',
        value: weeklySummary ? weeklySummary.caloriesKcal.toLocaleString('ko-KR') : '--',
        unit: 'kcal',
      },
      {
        title: '총 운동 시간',
        value: weeklySummary ? formatDuration(weeklySummary.durationSec) : '--',
        unit: '',
      },
    ],
    [weeklySummary]
  );

  const canSavePreferences =
    preferences.purposes.length > 0 &&
    preferences.timeSlots.length > 0 &&
    preferences.preferredDifficulty !== null &&
    preferences.courseTypes.length > 0;

  useEffect(() => {
    let isCancelled = false;

    void (async () => {
      try {
        const response = await getWeeklyCalendarApi();
        if (isCancelled) return;
        const { weeklyRunCount, weeklyDistanceM, weeklyCaloriesKcal, weeklyDurationSec } = response.data;
        setWeeklySummary({
          runCount: weeklyRunCount,
          distanceKm: weeklyDistanceM / 1000,
          caloriesKcal: weeklyCaloriesKcal,
          durationSec: weeklyDurationSec,
        });
      } catch {
        // 실패 시 카드는 '--' 유지
      }
    })();

    return () => { isCancelled = true; };
  }, []);

  useEffect(() => {
    let isCancelled = false;

    void (async () => {
      try {
        // 홈에서는 저장된 선호도를 먼저 hydrate 해서 온보딩 이후 수정 화면처럼 재사용한다.
        const response = await getUserRunningPreferences();

        if (isCancelled) {
          return;
        }

        setPreferences(createPreferenceState(response));
      } catch {
        if (isCancelled) {
          return;
        }

        toast.error('러닝 선호도 정보를 불러오지 못했어요.');
      } finally {
        if (!isCancelled) {
          setIsLoadingPreferences(false);
        }
      }
    })();

    return () => {
      isCancelled = true;
    };
  }, []);

  async function handleSavePreferences() {
    if (!canSavePreferences || isSavingPreferences) {
      return;
    }

    setIsSavingPreferences(true);

    try {
      await updateUserRunningPreferences({
        purposes: preferences.purposes,
        timeSlots: preferences.timeSlots,
        preferredDistanceKm: preferences.preferredDistanceKm,
        preferredDifficulty: preferences.preferredDifficulty!,
        courseTypes: preferences.courseTypes,
      });

      // 저장 직후 서버 값을 다시 읽어 화면 상태와 실제 저장값이 어긋나지 않게 맞춘다.
      const response = await getUserRunningPreferences();
      setPreferences(createPreferenceState(response));
      toast.success('러닝 선호도가 저장되었어요.');
    } catch {
      toast.error('러닝 선호도를 저장하지 못했어요. 다시 시도해주세요.');
    } finally {
      setIsSavingPreferences(false);
    }
  }

  return (
    <div className="-m-5 min-h-[calc(100dvh-5rem)] overflow-hidden bg-[hsl(var(--surface-subtle))] text-[hsl(var(--text-primary))]">
      <div className="relative min-h-full px-5 pb-10 pt-6">
        <div className="relative mx-auto flex max-w-[430px] flex-col gap-6">
          <section className="space-y-4">
            <div className="inline-flex items-center gap-2 rounded-full border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] px-4 py-2 text-xs font-bold tracking-[0.08em] text-[hsl(var(--text-secondary))]">
              <HeartPulse
                size={14}
                className={
                  isConnected
                    ? 'text-[hsl(var(--success))]'
                    : 'text-[hsl(var(--text-secondary))]'
                }
              />
              {healthConnectionLabel}
            </div>

            <div className="space-y-2">
              <h1 className="text-4xl font-black leading-tight tracking-tight text-[hsl(var(--text-primary))]">
                달려볼까요,
                <br />
                {user?.nickname ?? 'Runner'}님
              </h1>
              <p className="max-w-[14rem] text-base leading-7 text-[hsl(var(--text-secondary))]">
                오늘도 목표를 향해 달려보세요!
              </p>
            </div>
          </section>

          <section className="grid grid-cols-2 gap-3">
            <SummaryCard {...summaryCards[0]} />
            <SummaryCard {...summaryCards[1]} />
            <SummaryCard {...summaryCards[2]} />
            <SummaryCard {...summaryCards[3]} />
          </section>

          <section className="rounded-[2rem] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-5 shadow-[var(--shadow-md)]">
            <div className="mb-5 flex items-center gap-2 text-xl font-black text-[hsl(var(--text-primary))]">
              <Settings2 size={18} className="text-[hsl(var(--primary-1))]" />
              러닝 선호도 설정
            </div>

            {isLoadingPreferences ? (
              <div className="rounded-3xl border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-subtle))] px-4 py-10 text-center text-sm text-[hsl(var(--text-secondary))]">
                저장된 러닝 선호도를 불러오는 중이에요.
              </div>
            ) : (
              <div className="space-y-7">
                <PreferenceChipGroup
                  label="러닝 목적"
                  options={ONBOARDING_PURPOSE_OPTIONS}
                  selectedValues={preferences.purposes}
                  onToggle={(value) =>
                    setPreferences((currentState) => ({
                      ...currentState,
                      purposes: toggleSelection(currentState.purposes, value),
                    }))
                  }
                />

                <PreferenceChipGroup
                  label="선호 시간대"
                  options={ONBOARDING_TIME_SLOT_OPTIONS}
                  selectedValues={preferences.timeSlots}
                  onToggle={(value) =>
                    setPreferences((currentState) => ({
                      ...currentState,
                      timeSlots: toggleSelection(currentState.timeSlots, value),
                    }))
                  }
                />

                <section className="space-y-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-semibold text-[hsl(var(--text-secondary))]">
                      선호 러닝 거리
                    </h3>
                    <p className="text-3xl font-black text-[hsl(var(--primary-1))]">
                      {preferences.preferredDistanceKm.toFixed(1)}
                      <span className="ml-1 text-lg text-[hsl(var(--text-secondary))]">
                        km
                      </span>
                    </p>
                  </div>
                  <input
                    type="range"
                    min="1"
                    max="10"
                    step="0.5"
                    value={preferences.preferredDistanceKm}
                    onChange={(event) =>
                      setPreferences((currentState) => ({
                        ...currentState,
                        preferredDistanceKm: Number(event.target.value),
                      }))
                    }
                    className="h-2 w-full cursor-pointer appearance-none rounded-full bg-[hsl(var(--border-default))] accent-[hsl(var(--primary-1))]"
                  />
                  <div className="flex justify-between text-xs font-semibold text-[hsl(var(--text-secondary))]">
                    <span>1 km</span>
                    <span>10 km</span>
                  </div>
                </section>

                <PreferenceChipGroup
                  label="코스 타입"
                  options={ONBOARDING_COURSE_TYPE_OPTIONS}
                  selectedValues={preferences.courseTypes}
                  onToggle={(value) =>
                    setPreferences((currentState) => ({
                      ...currentState,
                      courseTypes: toggleSelection(
                        currentState.courseTypes,
                        value
                      ),
                    }))
                  }
                  className="gap-2.5"
                />

                <SingleSelectChipGroup
                  label="코스 난이도"
                  options={ONBOARDING_DIFFICULTY_OPTIONS}
                  selectedValue={preferences.preferredDifficulty}
                  onSelect={(value) =>
                    setPreferences((currentState) => ({
                      ...currentState,
                      preferredDifficulty: value,
                    }))
                  }
                />

                <button
                  type="button"
                  onClick={() => void handleSavePreferences()}
                  disabled={!canSavePreferences || isSavingPreferences}
                  className={cn(
                    'flex w-full items-center justify-center gap-2 rounded-[1.5rem] bg-[hsl(var(--primary-1))] px-4 py-4 text-lg font-black text-[hsl(var(--text-inverse))] transition-colors',
                    canSavePreferences && !isSavingPreferences
                      ? 'hover:bg-[hsl(var(--primary-1-hover))]'
                      : 'pointer-events-none opacity-45'
                  )}
                >
                  <Route size={18} />
                  {isSavingPreferences ? '저장 중...' : '저장하기'}
                </button>
              </div>
            )}
          </section>

          <div className="h-8" />
        </div>
      </div>
    </div>
  );
}
