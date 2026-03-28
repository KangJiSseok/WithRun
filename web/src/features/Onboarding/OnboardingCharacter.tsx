import { motion } from 'motion/react';

type OnboardingCharacterProps = {
  state: OnboardingCharacterState;
};

type OnboardingCharacterState = 'intro' | 'login' | 'form';

export function OnboardingCharacter({ state }: OnboardingCharacterProps) {
  // 지금은 임시 JSX 캐릭터지만, 나중에 내부만 Rive로 교체할 수 있도록
  // 온보딩 레이아웃과 상태 흐름에서 분리된 래퍼 컴포넌트로 두었다.
  const containerVariants = {
    intro: { y: 64, scale: 0.62 },
    login: { y: 28, scale: 0.62 },
    form: { y: 88, scale: 0.62 },
  };

  const bounceVariants = {
    intro: { y: [0, -18, 0], transition: { repeat: Infinity, duration: 0.42 } },
    login: { y: [0, -10, 0], transition: { repeat: Infinity, duration: 1.3 } },
    form: { y: [0, -14, 0], transition: { repeat: Infinity, duration: 0.46 } },
  };

  const frontArmVariants = {
    intro: {
      rotate: [-30, 30, -30],
      transition: { repeat: Infinity, duration: 0.42 },
    },
    login: {
      rotate: [0, 6, 0],
      transition: { repeat: Infinity, duration: 1.4 },
    },
    form: {
      rotate: [-18, 24, -18],
      transition: { repeat: Infinity, duration: 0.55 },
    },
  };

  const backArmVariants = {
    intro: {
      rotate: [30, -30, 30],
      transition: { repeat: Infinity, duration: 0.42 },
    },
    login: {
      rotate: [0, -6, 0],
      transition: { repeat: Infinity, duration: 1.4 },
    },
    form: {
      rotate: [18, -24, 18],
      transition: { repeat: Infinity, duration: 0.55 },
    },
  };

  const leftLegVariants = {
    intro: {
      rotate: [36, -36, 36],
      transition: { repeat: Infinity, duration: 0.42 },
    },
    login: {
      rotate: [0, 5, 0],
      transition: { repeat: Infinity, duration: 1.2 },
    },
    form: {
      rotate: [20, -20, 20],
      transition: { repeat: Infinity, duration: 0.55 },
    },
  };

  const rightLegVariants = {
    intro: {
      rotate: [-36, 36, -36],
      transition: { repeat: Infinity, duration: 0.42 },
    },
    login: {
      rotate: [0, -5, 0],
      transition: { repeat: Infinity, duration: 1.2 },
    },
    form: {
      rotate: [-20, 20, -20],
      transition: { repeat: Infinity, duration: 0.55 },
    },
  };

  return (
    <motion.div
      variants={containerVariants}
      initial="intro"
      animate={state}
      className="relative z-10 flex flex-col items-center justify-center"
    >
      <motion.div
        variants={bounceVariants}
        animate={state}
        className="relative h-64 w-40"
      >
        <div className="absolute left-1/2 top-0 z-30 h-24 w-24 -translate-x-1/2">
          <div className="absolute -left-2 top-2 h-10 w-10 rounded-full bg-gray-500" />
          <div className="absolute left-0 top-4 h-20 w-20 rounded-full bg-[#D96C6C]" />
          <div
            className="absolute -right-4 top-10 h-8 w-12 rounded-r-full bg-[#D96C6C]"
            style={{ transform: 'rotate(15deg)' }}
          />
          <div className="absolute -left-6 top-10 h-10 w-10 rounded-full bg-[#D96C6C]" />
          <div
            className="absolute -top-2 left-2 h-10 w-16 rounded-t-full rounded-bl-full bg-[#0000FF]"
            style={{ transform: 'rotate(-10deg)' }}
          />
          <div className="absolute left-6 top-8 h-3 w-3 rounded-full bg-black" />
          <div className="absolute left-14 top-10 h-3 w-3 rounded-full bg-black" />
          <div
            className="absolute left-8 top-14 h-5 w-10 rounded-full border-b-4 border-white"
            style={{ transform: 'rotate(-10deg)' }}
          />
        </div>

        <motion.div
          variants={backArmVariants}
          animate={state}
          className="absolute left-8 top-24 z-10 h-20 w-6 origin-top rounded-full bg-[#C45A5A]"
        />

        <div className="absolute left-1/2 top-[5.5rem] z-20 h-20 w-20 -translate-x-1/2 rounded-b-xl rounded-t-3xl bg-[#66FF66]" />
        <div className="absolute left-1/2 top-36 z-20 h-6 w-20 -translate-x-1/2 rounded-b-xl bg-[#FF99CC]" />

        <motion.div
          variants={frontArmVariants}
          animate={state}
          className="absolute right-8 top-24 z-30 h-20 w-6 origin-top rounded-full bg-[#D96C6C]"
        />

        <div className="absolute left-1/2 top-40 z-10 flex h-20 w-16 -translate-x-1/2 justify-between">
          <motion.div
            variants={leftLegVariants}
            animate={state}
            className="relative flex h-full w-7 origin-top flex-col items-center bg-[#3388FF]"
          >
            <div className="absolute -bottom-2 -left-2 h-6 w-12 rounded-full bg-[#E63939]" />
          </motion.div>
          <motion.div
            variants={rightLegVariants}
            animate={state}
            className="relative flex h-full w-7 origin-top flex-col items-center bg-[#F2E6E6]"
          >
            <div className="absolute -bottom-2 -left-2 h-6 w-12 rounded-full bg-[#E63939]" />
          </motion.div>
        </div>
      </motion.div>
    </motion.div>
  );
}
