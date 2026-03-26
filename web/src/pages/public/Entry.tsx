import { Navigate } from 'react-router-dom';

import { useAuthStore } from '@/stores/Auth';

export function EntryPage() {
  const status = useAuthStore((state) => state.status);
  const accessToken = useAuthStore((state) => state.accessToken);
  const profileCompleted = useAuthStore(
    (state) => state.user?.profileCompleted,
  );

  if (status === 'idle' || status === 'bootstrapping') {
    if (!accessToken) {
      return <Navigate to="/onboarding" replace />;
    }

    // access token 흔적이 있으면 bootstrap 결과가 나올 때까지 entry 에서만 짧게 로딩을 보여준다.
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#101116] px-6 text-center text-white">
        <div className="space-y-3">
          <div className="mx-auto h-12 w-12 animate-spin rounded-full border-4 border-white/15 border-t-[#FF5858]" />
          <div className="space-y-1">
            <p className="text-lg font-bold">세션을 확인하고 있어요</p>
            <p className="text-sm text-white/60">
              잠시 후 온보딩 또는 홈으로 이동합니다.
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (status === 'unauthenticated') {
    return <Navigate to="/onboarding" replace />;
  }

  return (
    <Navigate
      to={profileCompleted ? '/home' : '/onboarding'}
      replace
    />
  );
}
