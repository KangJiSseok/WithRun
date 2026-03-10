import { useEffect, useState } from 'react';
import { RunningGuideModal } from '@/shared/components/running-guide-modal';

// isGuideOpen: 가이드가 지금 열려 있는지 저장하는 값
// setIsGuideOpen: 그 값을 바꾸는 버튼 같은 함수

export function NormalPage() {
  const [isGuideOpen, setIsGuideOpen] = useState(false);

  useEffect(() => {
    const hasSeenGuide = localStorage.getItem('running-guide-seen');

    if (!hasSeenGuide) {
      setIsGuideOpen(true);
    }
  }, []);

  return (
    <>
      <div>일반 페이지</div>
      <RunningGuideModal open={isGuideOpen} />
    </>
  );
}
