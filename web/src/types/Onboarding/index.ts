import type { AuthGender } from '@/apis/auth';
import type {
  Difficulty,
  Purpose,
  TimeSlot,
} from '@/apis/RunningPreferences';
import type { CourseType } from '@/shared/types/api.type';

// 온보딩은 intro -> login -> form 흐름으로 고정해 캐릭터/배경 연출과 단계 분기를 함께 맞춘다.
export type OnboardingPhase = 'intro' | 'login' | 'form';

export type OnboardingCharacterState = 'intro' | 'login' | 'form';

export type OnboardingStepId =
  | 'nickname'
  | 'gender'
  | 'birthdate'
  | 'physical'
  | 'purposes'
  | 'timeSlots'
  | 'distance'
  | 'difficulty'
  | 'courseTypes';

export type OnboardingOption<TValue extends string = string> = {
  label: string;
  value: TValue;
  accentClassName?: string;
};

export type OnboardingStep = {
  id: OnboardingStepId;
  title: string;
  description?: string;
};

export type OnboardingFormState = {
  nickname: string;
  gender: AuthGender | null;
  birthdate: string;
  height: number;
  weight: number;
  purposes: Purpose[];
  timeSlots: TimeSlot[];
  preferredDistanceKm: number;
  preferredDifficulty: Difficulty | null;
  courseTypes: CourseType[];
};
