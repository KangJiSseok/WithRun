import { Navigate } from 'react-router-dom';

import { useOnboardingStore } from '@/stores/Onboarding';

export function EntryPage() {
  const hasHydrated = useOnboardingStore((state) => state.hasHydrated);
  const hasCompletedOnboarding = useOnboardingStore(
    (state) => state.hasCompletedOnboarding,
  );

  if (!hasHydrated) {
    return null;
  }

  return (
    <Navigate
      // 앱 첫 진입점에서는 온보딩 완료 여부만 보고 다음 화면으로 바로 보낸다.
      to={hasCompletedOnboarding ? '/home' : '/onboarding'}
      replace
    />
  );
}
