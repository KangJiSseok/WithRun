import { Navigate, Outlet } from 'react-router-dom';

import { useAuthStore } from '@/stores/Auth';

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

  if (status === 'authenticated') {
    return <Navigate to="/home" replace />;
  }

  return <Outlet />;
}

export function OnboardingRoute() {
  const status = useAuthStore((state) => state.status);
  const accessToken = useAuthStore((state) => state.accessToken);
  const user = useAuthStore((state) => state.user);
  const profileCompleted = user?.profileCompleted;

  if (status === 'idle' || status === 'bootstrapping') {
    if (accessToken && user === null) {
      return <AuthPendingScreen />;
    }
    if (!accessToken) {
      return <Navigate to="/login" replace />;
    }
    // accessToken 있고 user snapshot 있음 → profileCompleted 체크로 fall-through
  }

  if (status === 'unauthenticated') {
    return <Navigate to="/login" replace />;
  }

  if (profileCompleted) {
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
    if (profileCompleted) {
      return <Outlet />;
    }

    if (accessToken && user === null) {
      return <AuthPendingScreen />;
    }

    return <Navigate to="/login" replace />;
  }

  if (status === 'unauthenticated') {
    return <Navigate to="/login" replace />;
  }

  if (!profileCompleted) {
    return <Navigate to="/onboarding" replace />;
  }

  return <Outlet />;
}
