import { Navigate, Outlet } from 'react-router-dom';

import { useAuthStore } from '@/stores/Auth';

function resolveAuthenticatedPath(profileCompleted: boolean | undefined): string {
  return profileCompleted ? '/home' : '/onboarding';
}

function AuthPendingScreen() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[#101116] px-6 text-center text-white">
      <div className="space-y-3">
        <div className="mx-auto h-12 w-12 animate-spin rounded-full border-4 border-white/15 border-t-[#FF5858]" />
        <div className="space-y-1">
          <p className="text-lg font-bold">로그인 상태를 확인하고 있어요</p>
          <p className="text-sm text-white/60">
            잠시만 기다리면 바로 다음 화면으로 이동합니다.
          </p>
        </div>
      </div>
    </div>
  );
}

export function PublicOnlyRoute() {
  const status = useAuthStore((state) => state.status);
  const profileCompleted = useAuthStore(
    (state) => state.user?.profileCompleted,
  );

  if (status === 'authenticated') {
    // 이미 로그인된 사용자는 login 화면 대신 현재 프로필 완료 상태에 맞는 목적지로 보낸다.
    return (
      <Navigate
        to={resolveAuthenticatedPath(profileCompleted)}
        replace
      />
    );
  }

  return <Outlet />;
}

export function RequireOnboardingRoute() {
  const status = useAuthStore((state) => state.status);
  const profileCompleted = useAuthStore(
    (state) => state.user?.profileCompleted,
  );

  if (status === 'authenticated' && profileCompleted) {
    return <Navigate to="/home" replace />;
  }

  return <Outlet />;
}

export function RequireAuthRoute() {
  const status = useAuthStore((state) => state.status);
  const accessToken = useAuthStore((state) => state.accessToken);
  const user = useAuthStore((state) => state.user);
  const profileCompleted = user?.profileCompleted;

  if (status === 'idle' || status === 'bootstrapping') {
    // 새로고침 직후에는 localStorage 에 남아 있는 사용자 정보가 있으면 화면을 먼저 보여주고,
    // 완전히 비어 있는 경우에만 온보딩으로 돌려보낸다.
    if (profileCompleted) {
      return <Outlet />;
    }

    if (accessToken && user === null) {
      return <AuthPendingScreen />;
    }

    return <Navigate to="/onboarding" replace />;
  }

  if (status === 'unauthenticated') {
    return <Navigate to="/onboarding" replace />;
  }

  if (!profileCompleted) {
    return <Navigate to="/onboarding" replace />;
  }

  return <Outlet />;
}
