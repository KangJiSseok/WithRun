import { motion } from 'motion/react';

type PinkTreeProps = {
  cx: number;
  cy: number;
  scale?: number;
};

function PinkTree({ cx, cy, scale = 1 }: PinkTreeProps) {
  return (
    <g transform={`translate(${cx}, ${cy}) scale(${scale})`}>
      <path
        d="M -25 0 L -25 -8 L -15 -4 L -10 -12 L -5 -4 L 0 -15 L 5 -6 L 15 -12 L 20 -4 L 25 -8 L 25 0 Z"
        fill="#4DD0E1"
      />
      <path d="M -8 0 L 8 0 L 4 -120 L -4 -120 Z" fill="#3E2723" />
      <line
        x1="-6"
        y1="-30"
        x2="6"
        y2="-30"
        stroke="#5D4037"
        strokeWidth="3"
      />
      <line
        x1="-5"
        y1="-60"
        x2="5"
        y2="-60"
        stroke="#5D4037"
        strokeWidth="3"
      />
      <circle cx="-35" cy="-100" r="45" fill="#880E4F" />
      <circle cx="35" cy="-100" r="45" fill="#880E4F" />
      <circle cx="0" cy="-150" r="55" fill="#880E4F" />
      <circle cx="-30" cy="-105" r="45" fill="#D81B60" />
      <circle cx="30" cy="-105" r="45" fill="#D81B60" />
      <circle cx="0" cy="-145" r="55" fill="#D81B60" />
      <circle cx="-35" cy="-110" r="35" fill="#EC407A" />
      <circle cx="35" cy="-110" r="35" fill="#EC407A" />
      <circle cx="0" cy="-150" r="45" fill="#EC407A" />
      <circle cx="-45" cy="-115" r="4" fill="#FCE4EC" />
      <circle cx="-20" cy="-95" r="3" fill="#FCE4EC" />
      <circle cx="30" cy="-125" r="4" fill="#FCE4EC" />
      <circle cx="45" cy="-100" r="3" fill="#FCE4EC" />
      <circle cx="-10" cy="-165" r="4" fill="#FCE4EC" />
      <circle cx="15" cy="-140" r="3" fill="#FCE4EC" />
    </g>
  );
}

type OnboardingBackgroundProps = {
  isMoving: boolean;
};

export function OnboardingBackground({
  isMoving,
}: OnboardingBackgroundProps) {
  return (
    <div className="absolute inset-0 overflow-hidden bg-[#A9D179]">
      <svg
        width="100%"
        height="100%"
        preserveAspectRatio="xMidYMid slice"
        viewBox="-50 100 500 1000"
        className="absolute inset-0 h-full w-full"
      >
        <ellipse cx="80" cy="150" rx="40" ry="15" fill="#9CBF6F" opacity="0.6" />
        <ellipse cx="320" cy="350" rx="60" ry="20" fill="#9CBF6F" opacity="0.6" />
        <ellipse cx="90" cy="600" rx="45" ry="18" fill="#9CBF6F" opacity="0.6" />
        <ellipse cx="310" cy="720" rx="35" ry="12" fill="#9CBF6F" opacity="0.6" />

        <path d="M -100 350 L 500 350 L 500 550 L -100 550 Z" fill="#D25048" />
        <line x1="-100" y1="350" x2="500" y2="350" stroke="#FFFFFF" strokeWidth="6" />
        <line x1="-100" y1="550" x2="500" y2="550" stroke="#FFFFFF" strokeWidth="6" />

        <motion.line
          x1="-100"
          y1="400"
          x2="500"
          y2="400"
          stroke="#FFFFFF"
          strokeWidth="3"
          strokeDasharray="40 40"
          animate={
            isMoving ? { strokeDashoffset: [0, 80] } : { strokeDashoffset: 0 }
          }
          transition={{
            repeat: isMoving ? Infinity : 0,
            repeatType: 'loop',
            duration: 0.6,
            ease: 'linear',
          }}
        />
        <motion.line
          x1="-100"
          y1="450"
          x2="500"
          y2="450"
          stroke="#FFFFFF"
          strokeWidth="3"
          strokeDasharray="40 40"
          animate={
            isMoving ? { strokeDashoffset: [0, 80] } : { strokeDashoffset: 0 }
          }
          transition={{
            repeat: isMoving ? Infinity : 0,
            repeatType: 'loop',
            duration: 0.6,
            ease: 'linear',
          }}
        />
        <motion.line
          x1="-100"
          y1="500"
          x2="500"
          y2="500"
          stroke="#FFFFFF"
          strokeWidth="3"
          strokeDasharray="40 40"
          animate={
            isMoving ? { strokeDashoffset: [0, 80] } : { strokeDashoffset: 0 }
          }
          transition={{
            repeat: isMoving ? Infinity : 0,
            repeatType: 'loop',
            duration: 0.6,
            ease: 'linear',
          }}
        />

        <PinkTree cx={80} cy={300} scale={1.2} />
        <PinkTree cx={320} cy={280} scale={1} />
        <PinkTree cx={90} cy={720} scale={1.3} />
        <PinkTree cx={310} cy={680} scale={1.1} />

        <circle cx="90" cy="250" r="8" fill="#4A7C59" opacity="0.8" />
        <circle cx="100" cy="260" r="5" fill="#7BB062" opacity="0.8" />
        <circle cx="310" cy="300" r="10" fill="#386646" opacity="0.8" />
        <circle cx="295" cy="310" r="6" fill="#4A7C59" opacity="0.8" />
        <circle cx="85" cy="520" r="12" fill="#7BB062" opacity="0.8" />
        <circle cx="105" cy="535" r="7" fill="#386646" opacity="0.8" />
      </svg>
    </div>
  );
}
