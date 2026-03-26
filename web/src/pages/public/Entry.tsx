import { Navigate } from 'react-router-dom';

import { useAuthStore } from '@/stores/Auth';

export function EntryPage() {
  const status = useAuthStore((state) => state.status);
  const accessToken = useAuthStore((state) => state.accessToken);
  const user = useAuthStore((state) => state.user);
  const profileCompleted = user?.profileCompleted;

  if (status === 'idle' || status === 'bootstrapping') {
    // 새로고침 직후에도 마지막 사용자 스냅샷이 남아 있으면,
    // entry 에서 불필요하게 로딩에 묶지 말고 바로 목적지로 보낸다.
    if (user !== null) {
      return (
        <Navigate
          to={profileCompleted ? '/home' : '/onboarding'}
          replace
        />
      );
    }

    if (accessToken) {
      // access token 만 남아 있고 사용자 스냅샷이 비어 있는 케이스에서만
      // bootstrap 결과를 기다리며 짧은 로딩을 보여준다.
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

    return <Navigate to="/onboarding" replace />;
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
