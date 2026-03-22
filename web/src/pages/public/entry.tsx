import { Navigate } from 'react-router-dom';

import { hasCompletedOnboarding } from '@/features/Onboarding';

export function EntryPage() {
  return (
    <Navigate
      // 앱 첫 진입점에서는 온보딩 완료 여부만 보고 다음 화면으로 바로 보낸다.
      to={hasCompletedOnboarding() ? '/home' : '/onboarding'}
      replace
    />
  );
}
