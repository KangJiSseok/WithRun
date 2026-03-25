import { AnimatePresence, motion } from 'motion/react';
import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import {
  ONBOARDING_INTRO_DURATION_MS,
} from '@/constants/Onboarding';
import { useAuthStore } from '@/stores/Auth';
import type {
  OnboardingCharacterState,
  OnboardingPhase,
} from '@/types/Onboarding';
import { OnboardingBackground } from './OnboardingBackground';
import { OnboardingCharacter } from './OnboardingCharacter';
import { OnboardingFormStep } from './OnboardingFormStep';
import { OnboardingIntro } from './OnboardingIntro';
import { OnboardingLoginStep } from './OnboardingLoginStep';

function getCharacterState(
  phase: OnboardingPhase,
): OnboardingCharacterState {
  // 화면 단계와 캐릭터 상태를 분리해두면 나중에 내부 구현을 Rive로 바꿔도 연결이 단순하다.
  switch (phase) {
    case 'intro':
      return 'intro';
    case 'login':
      return 'login';
    case 'form':
      return 'form';
    default:
      return 'intro';
  }
}

function resolveInitialIntroState(
  isAuthenticated: boolean,
  hasAccessToken: boolean,
  hasLoginError: boolean,
): boolean {
  return isAuthenticated || hasAccessToken || hasLoginError;
}

function resolvePhase(
  isAuthenticated: boolean,
  hasLoginError: boolean,
  hasSeenIntro: boolean,
): OnboardingPhase {
  // 인증이 끝난 사용자는 바로 form 으로 보내고, 그 외에는 intro/login 만 번갈아 보여준다.
  if (isAuthenticated) {
    return 'form';
  }

  if (hasLoginError || hasSeenIntro) {
    return 'login';
  }

  return 'intro';
}

export function OnboardingScreen() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const status = useAuthStore((state) => state.status);
  const accessToken = useAuthStore((state) => state.accessToken);
  const profileCompleted = useAuthStore(
    (state) => state.user?.profileCompleted,
  );
  const loginError = searchParams.get('error');
  const isAuthenticated = status === 'authenticated';
  const [hasSeenIntro, setHasSeenIntro] = useState<boolean>(() =>
    resolveInitialIntroState(
      isAuthenticated,
      accessToken !== null,
      loginError !== null,
    ),
  );
  const phase = resolvePhase(isAuthenticated, loginError !== null, hasSeenIntro);

  useEffect(() => {
    if (isAuthenticated && profileCompleted) {
      navigate('/home', { replace: true });
    }
  }, [isAuthenticated, navigate, profileCompleted]);

  useEffect(() => {
    if (hasSeenIntro || isAuthenticated || loginError) {
      return;
    }

    // intro 는 한 번만 자동 재생하고, 이후에는 login 단계부터 바로 이어서 보여준다.
    const timeoutId = window.setTimeout(() => {
      setHasSeenIntro(true);
    }, ONBOARDING_INTRO_DURATION_MS);

    return () => window.clearTimeout(timeoutId);
  }, [hasSeenIntro, isAuthenticated, loginError]);

  function handleCompleteOnboarding() {
    navigate('/home', { replace: true });
  }

  return (
    <div className="relative min-h-screen overflow-hidden bg-[#A9D179]">
      <AnimatePresence>
        {phase !== 'intro' ? (
          <motion.div
            key="onboarding-shell"
            initial={{ clipPath: 'circle(0% at 50% 50%)' }}
            animate={{ clipPath: 'circle(150% at 50% 50%)' }}
            transition={{ duration: 1, ease: [0.4, 0, 0.2, 1] }}
            className="absolute inset-0 z-10 overflow-hidden"
          >
            <div className="absolute inset-0 z-0">
              <OnboardingBackground isMoving />
            </div>

            <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center">
              <OnboardingCharacter state={getCharacterState(phase)} />
            </div>

            <AnimatePresence mode="wait">
              {phase === 'login' ? (
                <OnboardingLoginStep
                  key="onboarding-login"
                  error={loginError}
                />
              ) : null}
              {phase === 'form' ? (
                <OnboardingFormStep
                  key="onboarding-form"
                  onComplete={handleCompleteOnboarding}
                />
              ) : null}
            </AnimatePresence>
          </motion.div>
        ) : null}
      </AnimatePresence>

      <AnimatePresence>
        {phase === 'intro' ? <OnboardingIntro /> : null}
      </AnimatePresence>
    </div>
  );
}
