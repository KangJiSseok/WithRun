import { useEffect, useState } from 'react';
import { FreeRunScreen } from '@/features/FreeRun';
import { RunningGuideModal } from '@/shared/components/running-guide-modal';

export function FreePage() {
  const [showGuide, setShowGuide] = useState(false);

  useEffect(() => {
    const hasSeenGuide = localStorage.getItem('freerun-guide-seen');
    if (!hasSeenGuide) {
      setShowGuide(true);
    }
  }, []);

  function handleCloseGuide() {
    localStorage.setItem('freerun-guide-seen', 'true');
    setShowGuide(false);
  }

  return (
    <>
      <FreeRunScreen />
      <RunningGuideModal
        isOpen={showGuide}
        onClose={handleCloseGuide}
        steps={[
          {
            title: '자유 러닝',
            content: '코스 제한 없이 원하는 곳 어디서든 자유롭게 달릴 수 있습니다.',
          },
          {
            title: '러닝 시작',
            content: '화면 중앙의 버튼을 탭하면 러닝이 시작됩니다.',
          },
          {
            title: '러닝 분석',
            content: '러닝이 끝나면 페이스, 소모 칼로리, 고도 등 상세한 데이터를 분석해드립니다.',
          },
        ]}
      />
    </>
  );
}
