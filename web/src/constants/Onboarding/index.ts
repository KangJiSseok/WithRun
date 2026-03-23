import type {
  OnboardingFormState,
  OnboardingOption,
  OnboardingStep,
} from '@/types/Onboarding';

export const ONBOARDING_STORAGE_KEY = 'withrun:onboarding-complete';

export const ONBOARDING_INTRO_DURATION_MS = 1400;

export const ONBOARDING_LOGIN_COPY_TEXTS = [
  '오늘도 나를 위해, 가볍게 뛰어볼까요?',
  '당신의 페이스에 맞춘 완벽한 러닝 메이트.',
  '혼자 뛰어도 외롭지 않게, With Run과 함께.',
];

export const ONBOARDING_STEPS: OnboardingStep[] = [
  { id: 'nickname', title: '어떻게 불러드릴까요?' },
  { id: 'gender', title: '성별을 알려주세요' },
  { id: 'birthdate', title: '생년월일을 알려주세요' },
  { id: 'physical', title: '신체 정보를 알려주세요' },
  { id: 'goal', title: '러닝의 목표가 무엇인가요?' },
  { id: 'preference', title: '언제 뛰는 것을 선호하시나요?' },
  { id: 'distance', title: '선호 러닝 거리' },
  { id: 'difficulty', title: '코스 난이도' },
  { id: 'courseType', title: '코스 타입' },
];

export const ONBOARDING_GOAL_OPTIONS: OnboardingOption[] = [
  { label: '다이어트', value: 'diet', accentClassName: 'text-[#3388FF]' },
  { label: '건강유지', value: 'wellness', accentClassName: 'text-[#35A55B]' },
  { label: '대회준비', value: 'race', accentClassName: 'text-[#7C3AED]' },
  { label: '체력증진', value: 'fitness', accentClassName: 'text-[#F97316]' },
];

export const ONBOARDING_PREFERENCE_OPTIONS: OnboardingOption[] = [
  { label: '아침', value: 'morning' },
  { label: '점심', value: 'noon' },
  { label: '저녁', value: 'evening' },
  { label: '새벽', value: 'dawn' },
];

export const ONBOARDING_DIFFICULTY_OPTIONS: OnboardingOption[] = [
  { label: '쉬움', value: 'easy' },
  { label: '보통', value: 'medium' },
  { label: '어려움', value: 'hard' },
];

export const ONBOARDING_COURSE_TYPE_OPTIONS: OnboardingOption[] = [
  { label: '한강변', value: 'riverside' },
  { label: '공원', value: 'park' },
  { label: '산/둘레길', value: 'trail' },
  { label: '트랙', value: 'track' },
  { label: '도심', value: 'urban' },
];

export const DEFAULT_ONBOARDING_FORM_STATE: OnboardingFormState = {
  nickname: '',
  gender: null,
  birthdate: '',
  height: 170,
  weight: 65,
  goal: null,
  preference: null,
  distance: 5,
  difficulty: null,
  courseType: null,
};
