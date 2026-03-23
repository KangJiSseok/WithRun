import { AnimatePresence, motion } from 'motion/react';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import {
  ONBOARDING_INTRO_DURATION_MS,
} from '@/constants/Onboarding';
import { useOnboardingStore } from '@/stores/Onboarding';
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

export function OnboardingScreen() {
  const navigate = useNavigate();
  const markOnboardingCompleted = useOnboardingStore(
    (state) => state.markOnboardingCompleted,
  );
  const [phase, setPhase] = useState<OnboardingPhase>('intro');

  useEffect(() => {
    if (phase !== 'intro') {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      // 네이티브 스플래시 뒤에 짧은 브랜드 인트로만 보여주고 로그인 단계로 넘긴다.
      setPhase('login');
    }, ONBOARDING_INTRO_DURATION_MS);

    return () => window.clearTimeout(timeoutId);
  }, [phase]);

  function handleLoginContinue() {
    setPhase('form');
  }

  function handleCompleteOnboarding() {
    // 첫 실행 여부를 저장해두고, 이후 앱 진입 시에는 홈으로 바로 보낸다.
    markOnboardingCompleted();
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
              <OnboardingBackground isMoving={phase !== 'form'} />
            </div>

            <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center">
              <OnboardingCharacter state={getCharacterState(phase)} />
            </div>

            <AnimatePresence mode="wait">
              {/* 현재 단계에 따라 로그인 시트 또는 온보딩 폼 시트를 번갈아 보여준다. */}
              {phase === 'login' ? (
                <OnboardingLoginStep
                  key="onboarding-login"
                  onContinue={handleLoginContinue}
                />
              ) : (
                <OnboardingFormStep
                  key="onboarding-form"
                  onComplete={handleCompleteOnboarding}
                />
              )}
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
