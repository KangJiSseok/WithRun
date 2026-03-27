import { useNavigate } from 'react-router-dom';

import { OnboardingBackground } from './OnboardingBackground';
import { OnboardingCharacter } from './OnboardingCharacter';
import { OnboardingFormStep } from './OnboardingFormStep';

export function OnboardingScreen() {
  const navigate = useNavigate();

  return (
    <div className="relative min-h-screen overflow-hidden bg-[#A9D179]">
      <div className="absolute inset-0 z-0">
        <OnboardingBackground isMoving />
      </div>

      <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center">
        <OnboardingCharacter state="form" />
      </div>

      <OnboardingFormStep onComplete={() => navigate('/home', { replace: true })} />
    </div>
  );
}
