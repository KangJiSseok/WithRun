import { useEffect, useState } from 'react';
import { RunningGuideModal } from '@/shared/components/running-guide-modal';

// showGuide: 가이드가 지금 열려 있는지 저장하는 값
// setShowGuide: 그 값을 바꾸는 버튼 같은 함수

export function NormalPage() {
  const [showGuide, setShowGuide] = useState(false);

  useEffect(() => {
    const hasSeenGuide = localStorage.getItem('running-guide-seen');

    if (!hasSeenGuide) {
      setShowGuide(true);
    }
  }, []);

  function handleCloseGuide() {
    localStorage.setItem('running-guide-seen', 'true');
    setShowGuide(false);
  }

  return (
    <>
      <div>일반 페이지</div>
      <RunningGuideModal
        isOpen={showGuide}
        onClose={handleCloseGuide}
        steps={[
          {
            title: '일반 러닝 코스',
            content:
              '전문가가 엄선한 공식 코스와 다른 러너들이 공유한 커뮤니티 코스를 만나보세요.',
          },
          {
            title: '나만의 페이스',
            content:
              "'일반 모드'로 나만의 페이스를 즐기며 코스를 완주해보세요.",
          },
          {
            title: '기록 달성 불가',
            content:
              '일반 러닝 코스에서는 리더보드에 기록을 세울 수 없습니다. 기록 경쟁을 원하시면 경쟁 러닝 코스를 이용해주세요.',
          },
        ]}
      />
    </>
  );
}