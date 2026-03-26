import { AnimatePresence, motion } from 'motion/react';
import { AlertCircle, Chrome, LoaderCircle, MessageCircle } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { bridge } from '@/bridge';
import {
  buildSocialLoginUrl,
  buildWebSocialLoginUrl,
  type SocialProvider,
} from '@/apis/auth';
import { ONBOARDING_LOGIN_COPY_TEXTS } from '@/constants/Onboarding';
import { useAuthStore } from '@/stores/Auth';

const SKIP_MOCK_USER: AuthUser = {
  userId: 0,
  provider: 'kakao',
  profileCompleted: true,
  nickname: '게스트',
  birthDate: null,
  gender: null,
  height: null,
  weight: null,
};

type LoginErrorCode = 'cancelled' | 'access_denied' | 'server_error';

type OnboardingLoginStepProps = {
  error: string | null;
};

const LOGIN_ERROR_MESSAGES: Record<LoginErrorCode, string> = {
  cancelled: '로그인이 취소되었어요. 다시 연결해볼까요?',
  access_denied: '권한 동의가 필요해서 로그인을 완료하지 못했어요.',
  server_error: '로그인 처리 중 오류가 발생했어요. 잠시 후 다시 시도해주세요.',
};

function resolveLoginError(error: string | null): LoginErrorCode | null {
  if (!error) {
    return null;
  }

  if (
    error === 'cancelled' ||
    error === 'access_denied' ||
    error === 'server_error'
  ) {
    return error;
  }

  return 'server_error';
}

export function OnboardingLoginStep({ error }: OnboardingLoginStepProps) {
  const [copyIndex, setCopyIndex] = useState(0);
  const [pendingProvider, setPendingProvider] = useState<SocialProvider | null>(
    null
  );
  const loginError = useMemo(() => resolveLoginError(error), [error]);
  const navigate = useNavigate();
  const setAuthenticated = useAuthStore((state) => state.setAuthenticated);

  function handleSkip() {
    setAuthenticated({ accessToken: 'guest-skip', user: SKIP_MOCK_USER });
    navigate('/home', { replace: true });
  }

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      setCopyIndex(
        (currentIndex) =>
          (currentIndex + 1) % ONBOARDING_LOGIN_COPY_TEXTS.length
      );
    }, 3000);

    return () => window.clearInterval(intervalId);
  }, []);

  async function handleLogin(provider: SocialProvider) {
    setPendingProvider(provider);

    try {
      if (bridge.isWebViewBridgeAvailable) {
        // 모바일에서는 네이티브가 외부 브라우저와 딥링크 복귀를 책임지도록 위임한다.
        const authorizationUrl = buildSocialLoginUrl(provider);
        await bridge.startSocialLogin(authorizationUrl);
        return;
      }

      // 일반 웹에서는 로그인 시작 origin 을 함께 넘겨 callback 도메인을 동적으로 맞춘다.
      const authorizationUrl = buildWebSocialLoginUrl(provider);
      window.location.assign(authorizationUrl);
    } catch {
      setPendingProvider(null);
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 48 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -36 }}
      transition={{ duration: 0.45, delay: 0.15 }}
      className="absolute bottom-0 z-20 flex min-h-[21rem] w-full flex-col items-center justify-between rounded-t-[2rem] border-t border-white/50 bg-white/80 px-6 pb-8 pt-8 shadow-[0_-10px_40px_rgba(0,0,0,0.12)] backdrop-blur-xl"
    >
      <div className="mb-6 w-full text-center">
        <h1 className="text-3xl font-extrabold tracking-tight text-gray-900">
          With Run
        </h1>
        <div className="mt-2 flex h-8 items-center justify-center">
          <AnimatePresence mode="wait">
            <motion.p
              key={copyIndex}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="text-sm font-medium text-gray-700"
            >
              {ONBOARDING_LOGIN_COPY_TEXTS[copyIndex]}
            </motion.p>
          </AnimatePresence>
        </div>

        {loginError ? (
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-4 flex items-start gap-2 rounded-2xl border border-red-500/20 bg-red-500/8 px-4 py-3 text-left text-sm text-red-600"
          >
            <AlertCircle className="mt-0.5 shrink-0" size={16} />
            <p>{LOGIN_ERROR_MESSAGES[loginError]}</p>
          </motion.div>
        ) : null}
      </div>

      <div className="w-full space-y-2">
        <button
          type="button"
          onClick={() => void handleLogin('kakao')}
          disabled={pendingProvider !== null}
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#FEE500] py-3 text-base font-bold text-[#000000] shadow-sm transition-all hover:shadow-md disabled:cursor-not-allowed disabled:opacity-70"
        >
          {pendingProvider === 'kakao' ? (
            <LoaderCircle size={18} className="animate-spin" />
          ) : (
            <MessageCircle size={18} />
          )}
          카카오로 시작하기
        </button>
        <button
          type="button"
          onClick={() => void handleLogin('google')}
          disabled={pendingProvider !== null}
          className="flex w-full items-center justify-center gap-2 rounded-xl border border-gray-300 bg-white py-3 text-base font-bold text-gray-700 shadow-sm transition-all hover:shadow-md disabled:cursor-not-allowed disabled:opacity-70"
        >
          {pendingProvider === 'google' ? (
            <LoaderCircle size={18} className="animate-spin" />
          ) : (
            <Chrome size={18} />
          )}
          구글로 시작하기
        </button>
        <button
          type="button"
          onClick={handleSkip}
          className="w-full py-2 text-sm text-gray-400"
        >
          로그인 없이 둘러보기
        </button>
      </div>
    </motion.div>
  );
}
