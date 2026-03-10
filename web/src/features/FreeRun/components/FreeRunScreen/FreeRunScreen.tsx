import { RunCharacter } from '@/shared/components/RunCharacter';
import { useFreeRun } from '@/features/FreeRun/hooks/useFreeRun';
import { FreeRunMap } from '../FreeRunMap';

export function FreeRunScreen() {
  const { isRunning, startRun, stopRun } = useFreeRun();

  const handleCharacterClick = () => {
    if (isRunning) stopRun();
    else startRun();
  };

  return (
    <div className="absolute inset-0 overflow-hidden">
      <FreeRunMap />

      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <RunCharacter isRunning={isRunning} onClick={handleCharacterClick} />
      </div>
    </div>
  );
}
