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

    if (error) {
      // 백엔드 실패 이유는 onboarding 화면이 그대로 보여주는 UX 로 다시 합친다.
      clearAuth();
      navigate(`/onboarding?error=${encodeURIComponent(error)}`, {
        replace: true,
      });
      return;
    }

    let isCancelled = false;

    bootstrapAuth();

    void (async () => {
      try {
        // callback 진입 시에는 항상 refresh cookie 기준으로 세션을 다시 세운다.
        const session = await bootstrapAuthSession();

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
        navigate('/onboarding?error=server_error', { replace: true });
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
