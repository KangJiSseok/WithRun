export type OnboardingPhase = 'intro' | 'login' | 'form';

export type OnboardingCharacterState = 'intro' | 'login' | 'form';

export type OnboardingStepId =
  | 'nickname'
  | 'gender'
  | 'birthdate'
  | 'physical'
  | 'goal'
  | 'preference'
  | 'distance'
  | 'difficulty'
  | 'courseType';

export type OnboardingOption = {
  label: string;
  value: string;
  accentClassName?: string;
};

export type OnboardingStep = {
  id: OnboardingStepId;
  title: string;
  description?: string;
};

export type OnboardingFormState = {
  nickname: string;
  gender: 'male' | 'female' | null;
  birthdate: string;
  height: number;
  weight: number;
  goal: string | null;
  preference: string | null;
  distance: number;
  difficulty: string | null;
  courseType: string | null;
};
