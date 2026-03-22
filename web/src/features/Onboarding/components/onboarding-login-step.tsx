import { AnimatePresence, motion } from 'motion/react';
import { useEffect, useState } from 'react';

import { ONBOARDING_LOGIN_COPY_TEXTS } from '../onboarding.constants';

type OnboardingLoginStepProps = {
  onContinue: () => void;
};

export function OnboardingLoginStep({
  onContinue,
}: OnboardingLoginStepProps) {
  const [copyIndex, setCopyIndex] = useState(0);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      setCopyIndex(
        (currentIndex) =>
          (currentIndex + 1) % ONBOARDING_LOGIN_COPY_TEXTS.length,
      );
    }, 3000);

    return () => window.clearInterval(intervalId);
  }, []);

  return (
    <motion.div
      initial={{ opacity: 0, y: 48 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -36 }}
      transition={{ duration: 0.45, delay: 0.15 }}
      className="absolute bottom-0 z-20 flex h-[18.5rem] w-full flex-col items-center justify-between rounded-t-[2rem] border-t border-white/50 bg-white/80 px-6 pb-8 pt-8 shadow-[0_-10px_40px_rgba(0,0,0,0.12)] backdrop-blur-xl"
    >
      <div className="mb-6 text-center">
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
      </div>

      <div className="w-full space-y-2">
        <button
          type="button"
          onClick={onContinue}
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#FEE500] py-3 text-base font-bold text-[#000000] shadow-sm transition-all hover:shadow-md"
        >
          카카오로 시작하기
        </button>
        <button
          type="button"
          onClick={onContinue}
          className="flex w-full items-center justify-center gap-2 rounded-xl border border-gray-300 bg-white py-3 text-base font-bold text-gray-700 shadow-sm transition-all hover:shadow-md"
        >
          구글로 시작하기
        </button>
      </div>
    </motion.div>
  );
}
