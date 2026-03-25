import { AnimatePresence, motion } from 'motion/react';
import {
  ChevronLeft,
  ChevronRight,
  Ruler,
  Weight,
} from 'lucide-react';
import { useState, type ReactNode } from 'react';
import { toast } from 'sonner';

import {
  DEFAULT_ONBOARDING_FORM_STATE,
  ONBOARDING_COURSE_TYPE_OPTIONS,
  ONBOARDING_DIFFICULTY_OPTIONS,
  ONBOARDING_PURPOSE_OPTIONS,
  ONBOARDING_STEPS,
  ONBOARDING_TIME_SLOT_OPTIONS,
} from '@/constants/Onboarding';
import { getCurrentUser } from '@/apis/auth';
import { updateUserRunningPreferences } from '@/apis/RunningPreferences';
import { updateUserProfile } from '@/apis/UserProfile';
import { cn } from '@/shared/utils';
import { useAuthStore } from '@/stores/Auth';
import type {
  OnboardingFormState,
  OnboardingOption,
  OnboardingStepId,
} from '@/types/Onboarding';

type OnboardingFormStepProps = {
  onComplete: () => void;
};

type StepFrameProps = {
  stepKey: string;
  children: ReactNode;
};

type OptionButtonProps = {
  isSelected: boolean;
  label: string;
  onClick: () => void;
  className?: string;
};

function StepFrame({ stepKey, children }: StepFrameProps) {
  return (
    <motion.div
      key={stepKey}
      initial={{ opacity: 0, x: 48 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -48 }}
      transition={{ duration: 0.28 }}
      className="flex h-full flex-col justify-center"
    >
      {children}
    </motion.div>
  );
}

function OptionButton({
  isSelected,
  label,
  onClick,
  className,
}: OptionButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'rounded-2xl border px-3 py-3 text-sm font-bold transition-all',
        isSelected
          ? 'border-[#FF5959] bg-[#FF5959]/10 text-[#FF5959]'
          : 'border-white/50 bg-white/75 text-gray-600',
        className,
      )}
    >
      {label}
    </button>
  );
}

type SingleChoiceGridProps<TValue extends string> = {
  options: OnboardingOption<TValue>[];
  selectedValue: TValue | null;
  onSelect: (value: TValue) => void;
  className?: string;
  itemClassName?: string;
};

type MultiChoiceGridProps<TValue extends string> = {
  options: OnboardingOption<TValue>[];
  selectedValues: TValue[];
  onToggle: (value: TValue) => void;
  className?: string;
  itemClassName?: string;
};

function SingleChoiceGrid<TValue extends string>({
  options,
  selectedValue,
  onSelect,
  className,
  itemClassName,
}: SingleChoiceGridProps<TValue>) {
  return (
    <div className={cn('grid gap-2', className)}>
      {options.map((option) => {
        const isSelected = selectedValue === option.value;

        return (
          <OptionButton
            key={option.value}
            isSelected={isSelected}
            label={option.label}
            onClick={() => onSelect(option.value)}
            className={cn(
              option.accentClassName && isSelected ? option.accentClassName : '',
              itemClassName,
            )}
          />
        );
      })}
    </div>
  );
}

function MultiChoiceGrid<TValue extends string>({
  options,
  selectedValues,
  onToggle,
  className,
  itemClassName,
}: MultiChoiceGridProps<TValue>) {
  return (
    <div className={cn('grid gap-2', className)}>
      {options.map((option) => {
        const isSelected = selectedValues.includes(option.value);

        return (
          <OptionButton
            key={option.value}
            isSelected={isSelected}
            label={option.label}
            onClick={() => onToggle(option.value)}
            className={cn(
              option.accentClassName && isSelected ? option.accentClassName : '',
              itemClassName,
            )}
          />
        );
      })}
    </div>
  );
}

function updateField<K extends keyof OnboardingFormState>(
  previousState: OnboardingFormState,
  field: K,
  value: OnboardingFormState[K],
): OnboardingFormState {
  return {
    ...previousState,
    [field]: value,
  };
}

function toggleSelection<TValue extends string>(
  selectedValues: TValue[],
  value: TValue,
): TValue[] {
  return selectedValues.includes(value)
    ? selectedValues.filter((currentValue) => currentValue !== value)
    : [...selectedValues, value];
}

function isStepComplete(
  stepId: OnboardingStepId,
  formState: OnboardingFormState,
): boolean {
  switch (stepId) {
    case 'nickname':
      return formState.nickname.trim().length > 0;
    case 'gender':
      return formState.gender !== null;
    case 'birthdate':
      return formState.birthdate.length > 0;
    case 'physical':
      return formState.height > 0 && formState.weight > 0;
    case 'purposes':
      return formState.purposes.length > 0;
    case 'timeSlots':
      return formState.timeSlots.length > 0;
    case 'distance':
      return (
        formState.preferredDistanceKm >= 1 &&
        formState.preferredDistanceKm <= 10
      );
    case 'difficulty':
      return formState.preferredDifficulty !== null;
    case 'courseTypes':
      return formState.courseTypes.length > 0;
    default:
      return false;
  }
}

export function OnboardingFormStep({
  onComplete,
}: OnboardingFormStepProps) {
  const accessToken = useAuthStore((state) => state.accessToken);
  const setAuthenticated = useAuthStore((state) => state.setAuthenticated);
  const [currentStepIndex, setCurrentStepIndex] = useState(0);
  const [formState, setFormState] = useState(DEFAULT_ONBOARDING_FORM_STATE);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const currentStep = ONBOARDING_STEPS[currentStepIndex];
  const isFirstStep = currentStepIndex === 0;
  const isLastStep = currentStepIndex === ONBOARDING_STEPS.length - 1;
  const isCurrentStepValid = isStepComplete(currentStep.id, formState);

  function setField<K extends keyof OnboardingFormState>(
    field: K,
    value: OnboardingFormState[K],
  ) {
    setFormState((previousState) => updateField(previousState, field, value));
  }

  function handlePrevious() {
    if (isFirstStep) {
      return;
    }

    setCurrentStepIndex((previousStepIndex) => previousStepIndex - 1);
  }

  async function handleSubmit() {
    if (!accessToken) {
      toast.error('로그인 세션을 다시 확인해주세요.');
      return;
    }

    setIsSubmitting(true);

    try {
      // 백엔드 계약에 맞춰 프로필과 러닝 선호도를 순서대로 저장한다.
      await updateUserProfile({
        nickname: formState.nickname.trim(),
        birthDate: formState.birthdate,
        gender: formState.gender!,
        height: formState.height,
        weight: formState.weight,
      });

      await updateUserRunningPreferences({
        purposes: formState.purposes,
        timeSlots: formState.timeSlots,
        preferredDistanceKm: formState.preferredDistanceKm,
        preferredDifficulty: formState.preferredDifficulty!,
        courseTypes: formState.courseTypes,
      });

      const latestAccessToken = useAuthStore.getState().accessToken;

      if (!latestAccessToken) {
        throw new Error('Missing access token after onboarding save');
      }

      // 저장 후 /auth/me 를 다시 읽어 profileCompleted=true 인 최신 사용자 스냅샷으로 store 를 갱신한다.
      const user = await getCurrentUser(latestAccessToken);
      setAuthenticated({ accessToken: latestAccessToken, user });
      toast.success('온보딩 정보가 저장되었어요.');
      onComplete();
    } catch {
      toast.error('온보딩 정보를 저장하지 못했어요. 다시 시도해주세요.');
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleNext() {
    if (!isCurrentStepValid || isSubmitting) {
      return;
    }

    if (isLastStep) {
      void handleSubmit();
      return;
    }

    setCurrentStepIndex((previousStepIndex) => previousStepIndex + 1);
  }

  function renderCurrentStep(stepId: OnboardingStepId) {
    // 프로토타입의 질문 흐름을 step id 기준으로 나눠 두면 순서 변경이나 문항 추가가 쉬워진다.
    switch (stepId) {
      case 'nickname':
        return (
          <StepFrame stepKey={stepId}>
            <div className="mb-4 text-center">
              <h2 className="text-xl font-bold text-gray-900">
                {currentStep.title}
              </h2>
            </div>

            <input
              type="text"
              value={formState.nickname}
              onChange={(event) => setField('nickname', event.target.value)}
              placeholder="닉네임 입력"
              className="w-full border-b-2 border-gray-300 bg-transparent py-2 text-center text-2xl font-bold text-gray-900 outline-none transition-colors placeholder:text-gray-400 focus:border-[#3388FF]"
              autoFocus
            />
          </StepFrame>
        );
      case 'gender':
        return (
          <StepFrame stepKey={stepId}>
            <div className="mb-4 text-center">
              <h2 className="text-xl font-bold text-gray-900">
                {currentStep.title}
              </h2>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <OptionButton
                isSelected={formState.gender === 'MALE'}
                label="남성"
                onClick={() => setField('gender', 'MALE')}
                className={
                  formState.gender === 'MALE'
                    ? 'text-[#3388FF]'
                    : undefined
                }
              />
              <OptionButton
                isSelected={formState.gender === 'FEMALE'}
                label="여성"
                onClick={() => setField('gender', 'FEMALE')}
                className={
                  formState.gender === 'FEMALE'
                    ? 'border-[#FF99CC] bg-pink-50 text-[#FF99CC]'
                    : undefined
                }
              />
            </div>
          </StepFrame>
        );
      case 'birthdate':
        return (
          <StepFrame stepKey={stepId}>
            <div className="mb-4 text-center">
              <h2 className="text-xl font-bold text-gray-900">
                {currentStep.title}
              </h2>
            </div>

            <input
              type="date"
              value={formState.birthdate}
              onChange={(event) => setField('birthdate', event.target.value)}
              className="w-full rounded-xl border-2 border-gray-200 bg-white/60 p-3 font-medium text-gray-800 outline-none transition-colors focus:border-[#3388FF]"
            />
          </StepFrame>
        );
      case 'physical':
        return (
          <StepFrame stepKey={stepId}>
            <div className="mb-4 text-center">
              <h2 className="text-xl font-bold text-gray-900">
                {currentStep.title}
              </h2>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="flex flex-col rounded-2xl border border-white/50 bg-white/80 p-3 shadow-sm backdrop-blur-sm">
                <div className="mb-2 flex items-center justify-between">
                  <div className="flex items-center gap-1 text-sm text-gray-700">
                    <Ruler size={16} />
                    <span className="font-medium">키</span>
                  </div>
                  <div className="text-lg font-bold text-[#3388FF]">
                    {formState.height}
                    <span className="ml-0.5 text-xs text-gray-400">cm</span>
                  </div>
                </div>
                <input
                  type="range"
                  min="140"
                  max="220"
                  value={formState.height}
                  onChange={(event) =>
                    setField('height', Number(event.target.value))
                  }
                  className="h-1.5 w-full cursor-pointer appearance-none rounded-lg bg-gray-200 accent-[#3388FF]"
                />
              </div>

              <div className="flex flex-col rounded-2xl border border-white/50 bg-white/80 p-3 shadow-sm backdrop-blur-sm">
                <div className="mb-2 flex items-center justify-between">
                  <div className="flex items-center gap-1 text-sm text-gray-700">
                    <Weight size={16} />
                    <span className="font-medium">몸무게</span>
                  </div>
                  <div className="text-lg font-bold text-[#35A55B]">
                    {formState.weight}
                    <span className="ml-0.5 text-xs text-gray-400">kg</span>
                  </div>
                </div>
                <input
                  type="range"
                  min="40"
                  max="150"
                  value={formState.weight}
                  onChange={(event) =>
                    setField('weight', Number(event.target.value))
                  }
                  className="h-1.5 w-full cursor-pointer appearance-none rounded-lg bg-gray-200 accent-[#35A55B]"
                />
              </div>
            </div>
          </StepFrame>
        );
      case 'purposes':
        return (
          <StepFrame stepKey={stepId}>
            <div className="mb-4 text-center">
              <h2 className="text-xl font-bold text-gray-900">
                {currentStep.title}
              </h2>
            </div>

            <MultiChoiceGrid
              options={ONBOARDING_PURPOSE_OPTIONS}
              selectedValues={formState.purposes}
              onToggle={(value) =>
                setField(
                  'purposes',
                  toggleSelection(formState.purposes, value),
                )
              }
              className="grid-cols-2"
              itemClassName="min-h-20"
            />
          </StepFrame>
        );
      case 'timeSlots':
        return (
          <StepFrame stepKey={stepId}>
            <div className="mb-4 text-center">
              <h2 className="text-xl font-bold text-gray-900">
                {currentStep.title}
              </h2>
            </div>

            <MultiChoiceGrid
              options={ONBOARDING_TIME_SLOT_OPTIONS}
              selectedValues={formState.timeSlots}
              onToggle={(value) =>
                setField(
                  'timeSlots',
                  toggleSelection(formState.timeSlots, value),
                )
              }
              className="grid-cols-2"
              itemClassName="min-h-16"
            />
          </StepFrame>
        );
      case 'distance':
        return (
          <StepFrame stepKey={stepId}>
            <div className="mb-4 text-center">
              <h2 className="text-xl font-bold text-gray-900">
                {currentStep.title}
              </h2>
            </div>

            <div className="rounded-2xl border border-white/50 bg-white/80 p-4 shadow-sm backdrop-blur-sm">
              <div className="mb-3 flex items-center justify-between">
                <span className="text-sm font-medium text-gray-700">거리</span>
                <div className="text-xl font-bold text-[#3388FF]">
                  {formState.preferredDistanceKm.toFixed(1)}
                  <span className="ml-0.5 text-sm text-gray-400">km</span>
                </div>
              </div>
              <input
                type="range"
                min="1"
                max="10"
                step="0.5"
                value={formState.preferredDistanceKm}
                onChange={(event) =>
                  setField('preferredDistanceKm', Number(event.target.value))
                }
                className="h-2 w-full cursor-pointer appearance-none rounded-lg bg-gray-200 accent-[#3388FF]"
              />
              <div className="mt-3 flex justify-between text-xs font-medium text-gray-400">
                <span>1 km</span>
                <span>10 km</span>
              </div>
            </div>
          </StepFrame>
        );
      case 'difficulty':
        return (
          <StepFrame stepKey={stepId}>
            <div className="mb-4 text-center">
              <h2 className="text-xl font-bold text-gray-900">
                {currentStep.title}
              </h2>
            </div>

            <SingleChoiceGrid
              options={ONBOARDING_DIFFICULTY_OPTIONS}
              selectedValue={formState.preferredDifficulty}
              onSelect={(value) => setField('preferredDifficulty', value)}
              className="grid-cols-3"
            />
          </StepFrame>
        );
      case 'courseTypes':
        return (
          <StepFrame stepKey={stepId}>
            <div className="mb-4 text-center">
              <h2 className="text-xl font-bold text-gray-900">
                {currentStep.title}
              </h2>
            </div>

            <MultiChoiceGrid
              options={ONBOARDING_COURSE_TYPE_OPTIONS}
              selectedValues={formState.courseTypes}
              onToggle={(value) =>
                setField(
                  'courseTypes',
                  toggleSelection(formState.courseTypes, value),
                )
              }
              className="grid-cols-3"
              itemClassName="px-1 py-3 text-xs"
            />
          </StepFrame>
        );
      default:
        return null;
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 48 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: 48 }}
      transition={{ duration: 0.35 }}
      className="absolute bottom-0 z-20 flex h-[28rem] w-full max-w-md flex-col justify-between rounded-t-[2rem] border-t border-white/50 bg-white/90 px-6 pb-6 pt-6 shadow-[0_-10px_40px_rgba(0,0,0,0.12)] backdrop-blur-xl"
    >
      <div className="mb-3 h-1.5 w-full shrink-0 overflow-hidden rounded-full bg-gray-200/60">
        <motion.div
          // 전체 문항 중 현재 위치를 진행 바로 보여준다.
          className="h-full bg-[#3388FF]"
          initial={{ width: 0 }}
          animate={{
            width: `${((currentStepIndex + 1) / ONBOARDING_STEPS.length) * 100}%`,
          }}
          transition={{ duration: 0.3 }}
        />
      </div>

      <div className="relative flex-1 overflow-y-auto pr-1">
        <AnimatePresence mode="wait">
          {renderCurrentStep(currentStep.id)}
        </AnimatePresence>
      </div>

      <div className="mt-3 flex shrink-0 gap-3">
        <button
          type="button"
          onClick={handlePrevious}
          disabled={isFirstStep || isSubmitting}
          className={cn(
            'flex flex-1 items-center justify-center gap-1 rounded-xl border border-gray-200 bg-white/80 py-2.5 text-base font-bold text-gray-700 shadow-sm transition-colors',
            (isFirstStep || isSubmitting) && 'pointer-events-none opacity-45',
          )}
        >
          <ChevronLeft size={18} />
          이전
        </button>
        <button
          type="button"
          onClick={handleNext}
          disabled={!isCurrentStepValid || isSubmitting}
          className={cn(
            'flex flex-1 items-center justify-center gap-1 rounded-xl bg-[#3388FF] py-2.5 text-base font-bold text-white shadow-sm transition-colors hover:bg-blue-600',
            (!isCurrentStepValid || isSubmitting) &&
              'pointer-events-none opacity-45',
          )}
        >
          {isSubmitting ? '저장 중...' : isLastStep ? '저장하고 시작하기' : '다음'}
          {!isLastStep && !isSubmitting ? <ChevronRight size={18} /> : null}
        </button>
      </div>
    </motion.div>
  );
}
