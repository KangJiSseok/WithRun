import { motion } from 'motion/react';
import { ArrowRight, Chrome, MessageCircle, TriangleAlert } from 'lucide-react';
import { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';

import { bridge } from '@/bridge';
import {
  buildSocialLoginUrl,
  buildWebSocialLoginUrl,
  type SocialProvider,
} from '@/apis/auth';

type LoginErrorCode = 'cancelled' | 'access_denied' | 'server_error';

const LOGIN_ERROR_MESSAGES: Record<LoginErrorCode, string> = {
  cancelled: '로그인이 취소되었어요. 다시 연결해볼까요?',
  access_denied: '권한 동의가 필요해서 로그인을 완료하지 못했어요.',
  server_error: '로그인 처리 중 오류가 발생했어요. 잠시 후 다시 시도해주세요.',
};

function resolveLoginError(error: string | null): LoginErrorCode | null {
  if (!error) {
    return null;
  }

  if (error === 'cancelled' || error === 'access_denied' || error === 'server_error') {
    return error;
  }

  return 'server_error';
}

export function LoginScreen() {
  const [searchParams] = useSearchParams();

  const loginError = useMemo(
    () => resolveLoginError(searchParams.get('error')),
    [searchParams],
  );

  async function handleLogin(provider: SocialProvider) {
    if (bridge.isWebViewBridgeAvailable) {
      // 모바일 WebView 는 기존 네이티브 브릿지 경로를 유지해 앱 딥링크 복귀 흐름을 그대로 사용한다.
      const authorizationUrl = buildSocialLoginUrl(provider);
      await bridge.startSocialLogin(authorizationUrl);
      return;
    }

    // 일반 웹은 현재 origin 을 백엔드에 알려줘야 Vercel/SSAFY 중 시작한 도메인으로 다시 돌아올 수 있다.
    const authorizationUrl = buildWebSocialLoginUrl(provider);
    window.location.assign(authorizationUrl);
  }

  return (
    <div className="flex min-h-screen flex-col overflow-hidden bg-[#101116] text-white">
      <div className="relative flex flex-1 flex-col justify-between overflow-hidden px-6 pb-10 pt-16">
        <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top,_rgba(255,93,93,0.18),_transparent_30%),radial-gradient(circle_at_bottom,_rgba(59,130,246,0.22),_transparent_35%)]" />

        <div className="relative space-y-6">
          <div className="inline-flex items-center rounded-full border border-white/10 bg-white/5 px-4 py-2 text-xs font-semibold tracking-[0.28em] text-white/60">
            WITH RUN
          </div>

          <div className="space-y-4">
            <h1 className="max-w-xs text-4xl font-black leading-tight">
              오늘의 페이스를
              <br />
              다시 연결해볼까요?
            </h1>
            <p className="max-w-xs text-sm leading-6 text-white/70">
              백엔드 OAuth 세션을 기준으로 로그인 상태를 복구하고,
              온보딩과 홈 탭까지 자연스럽게 이어집니다.
            </p>
          </div>

          {loginError ? (
            <motion.div
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              className="flex items-start gap-3 rounded-2xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-100"
            >
              <TriangleAlert className="mt-0.5 shrink-0" size={18} />
              <p>{LOGIN_ERROR_MESSAGES[loginError]}</p>
            </motion.div>
          ) : null}
        </div>

        <div className="relative space-y-3 rounded-[2rem] border border-white/10 bg-white/6 p-5 backdrop-blur-xl">
          <p className="text-sm font-medium text-white/60">
            소셜 로그인으로 러닝 기록과 선호도를 이어서 사용할 수 있어요.
          </p>

          <button
            type="button"
            onClick={() => void handleLogin('kakao')}
            className="flex w-full items-center justify-between rounded-2xl bg-[#FEE500] px-4 py-4 text-left text-sm font-black text-[#111111] transition-transform hover:scale-[1.01]"
          >
            <span className="flex items-center gap-3">
              <MessageCircle size={18} />
              카카오로 시작하기
            </span>
            <ArrowRight size={18} />
          </button>

          <button
            type="button"
            onClick={() => void handleLogin('google')}
            className="flex w-full items-center justify-between rounded-2xl border border-white/15 bg-white px-4 py-4 text-left text-sm font-black text-[#111111] transition-transform hover:scale-[1.01]"
          >
            <span className="flex items-center gap-3">
              <Chrome size={18} />
              구글로 시작하기
            </span>
            <ArrowRight size={18} />
          </button>
        </div>
      </div>
    </div>
  );
}
