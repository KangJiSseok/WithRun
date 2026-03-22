import { ONBOARDING_STORAGE_KEY } from './onboarding.constants';

export function hasCompletedOnboarding(): boolean {
  // 온보딩 완료 여부는 브라우저 저장소에 간단히 남겨 두고 첫 진입 분기에서 재사용한다.
  return localStorage.getItem(ONBOARDING_STORAGE_KEY) === 'true';
}

export function markOnboardingCompleted(): void {
  localStorage.setItem(ONBOARDING_STORAGE_KEY, 'true');
}

export function clearOnboardingCompletion(): void {
  localStorage.removeItem(ONBOARDING_STORAGE_KEY);
}
