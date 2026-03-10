import { useNavigate } from 'react-router-dom';
import { RunCharacter } from '@/shared/components/RunCharacter';
import { FreeRunMap } from '../FreeRunMap';

export function FreeRunScreen() {
  const navigate = useNavigate();

  return (
    <div className="absolute inset-0 overflow-hidden">
      <FreeRunMap />
      {/* 카카오맵 SDK는 내부적으로 z-index: 0~4 정도의 레이어를 쌓아두기에 올려줘야 함 */}
      <div className="absolute inset-0 flex flex-col items-center justify-center z-10">
        <RunCharacter isRunning={false} onClick={() => navigate('/freerun')} />
      </div>
    </div>
  );
}
