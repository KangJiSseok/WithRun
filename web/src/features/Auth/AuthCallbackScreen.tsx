import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { useAuthStore } from '@/stores/Auth';
import { bootstrapAuthSession } from '@/hooks/Auth';

export function AuthCallbackScreen() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const bootstrapAuth = useAuthStore((state) => state.bootstrapAuth);
  const setAuthenticated = useAuthStore((state) => state.setAuthenticated);
  const clearAuth = useAuthStore((state) => state.clearAuth);

  useEffect(() => {
    const error = searchParams.get('error');
    const authCode = searchParams.get('code');

    if (error) {
      clearAuth();
      navigate(`/login?error=${encodeURIComponent(error)}`, {
        replace: true,
      });
      return;
    }

    let isCancelled = false;

    bootstrapAuth();

    void (async () => {
      try {
        // 모바일은 code 교환, 웹은 refresh cookie 재발급으로 같은 callback 화면에서 세션을 복구한다.
        const session = await bootstrapAuthSession(undefined, authCode);

        if (isCancelled) {
          return;
        }

        setAuthenticated(session);
        navigate(session.user.profileCompleted ? '/home' : '/onboarding', {
          replace: true,
        });
      } catch {
        if (isCancelled) {
          return;
        }

        clearAuth();
        navigate('/login?error=server_error', { replace: true });
      }
    })();

    return () => {
      isCancelled = true;
    };
  }, [bootstrapAuth, clearAuth, navigate, searchParams, setAuthenticated]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#101116] px-6 text-center text-white">
      <div className="space-y-3">
        <div className="mx-auto h-12 w-12 animate-spin rounded-full border-4 border-white/15 border-t-[#FF5858]" />
        <div className="space-y-1">
          <p className="text-lg font-bold">로그인 정보를 확인하고 있어요</p>
          <p className="text-sm text-white/60">
            세션을 복구한 뒤 바로 다음 화면으로 이동합니다.
          </p>
        </div>
      </div>
    </div>
  );
}
