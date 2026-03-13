// 공식 코스 등록 폼 Drawer (바텀 시트)
// absolute 포지셔닝으로 MobileLayout 프레임 안에서 렌더링
// slide-up 애니메이션 적용
// 추후 API 연동 시 handleSubmit에서 POST /courses 호출로 교체

import { useEffect, useState } from 'react';
import { Button } from '@/shared/components/shadcn/button';
import { cn } from '@/shared/utils';

type CourseShareModalProps = {
  isOpen: boolean;
  onClose: () => void;
};

const DIFFICULTY_OPTIONS = ['쉬움', '보통', '어려움'] as const;
const SURFACE_OPTIONS = ['포장도로', '비포장도로', '복합'] as const;
const COURSE_TYPE_OPTIONS = ['한강변', '공원', '산·둘레길', '트랙', '도심', '기타'] as const;

export function CourseShareModal({ isOpen, onClose }: CourseShareModalProps) {
  const [courseTitle, setCourseTitle] = useState(
    `나의 러닝 ${new Date().toLocaleDateString()}`,
  );
  const [difficulty, setDifficulty] = useState<string>('보통');
  const [surface, setSurface] = useState<string>('포장도로');
  const [courseTypes, setCourseTypes] = useState<string[]>([]);

  // Drawer가 열린 동안 결과화면 스크롤 잠금
  useEffect(() => {
    const scrollContainer = document.querySelector<HTMLElement>('.result-scroll-container');
    if (!scrollContainer) return;
    if (isOpen) {
      scrollContainer.style.overflow = 'hidden';
    } else {
      scrollContainer.style.overflow = '';
    }
    return () => {
      scrollContainer.style.overflow = '';
    };
  }, [isOpen]);

  function toggleCourseType(type: string) {
    setCourseTypes((prev) =>
      prev.includes(type) ? prev.filter((t) => t !== type) : [...prev, type],
    );
  }

  // 추후 API 연동 시 POST /courses 호출로 교체
  function handleSubmit() {
    onClose();
  }

  if (!isOpen) return null;

  return (
    <div
      className="absolute inset-0 z-modal bg-black/50"
      onClick={onClose}
    >
      <div
        className={cn(
          'absolute bottom-0 left-0 right-0',
          'flex flex-col rounded-t-3xl',
          'bg-[hsl(var(--secondary-2))] border-t border-[hsl(var(--secondary-3))]',
          'max-h-[90%]',
          'animate-slide-up',
        )}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Drawer 핸들 */}
        <div className="flex justify-center pt-3 pb-1 shrink-0">
          <div className="w-10 h-1.5 rounded-full bg-[hsl(var(--secondary-4))]" />
        </div>

        {/* 헤더 */}
        <div className="px-6 pt-2 pb-0 shrink-0">
          <h3 className="h3-b text-[hsl(var(--text-inverse))]">공식 코스 등록</h3>
        </div>

        <div className="overflow-y-auto px-6 pb-8 pt-4 space-y-[var(--space-5)]">
          {/* 코스 제목 */}
          <div className="space-y-[var(--space-2)]">
            <label className="caption-b text-[hsl(var(--text-inverse))]">코스 제목</label>
            <input
              value={courseTitle}
              onChange={(e) => setCourseTitle(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-[hsl(var(--secondary-3))] text-[hsl(var(--text-inverse))] body-r border border-[hsl(var(--secondary-4))] focus:outline-none focus:border-primary"
            />
          </div>

          {/* 난이도 */}
          <div className="space-y-[var(--space-2)]">
            <label className="caption-b text-[hsl(var(--text-inverse))]">난이도</label>
            <div className="flex gap-2">
              {DIFFICULTY_OPTIONS.map((option) => (
                <button
                  key={option}
                  onClick={() => setDifficulty(option)}
                  className={cn(
                    'flex-1 py-2 rounded-xl caption-m transition-colors border',
                    difficulty === option
                      ? 'bg-primary text-[hsl(var(--text-inverse))] border-transparent'
                      : 'bg-[hsl(var(--secondary-3))] text-[hsl(var(--secondary-5))] border-[hsl(var(--secondary-4))]',
                  )}
                >
                  {option}
                </button>
              ))}
            </div>
          </div>

          {/* 노면 */}
          <div className="space-y-[var(--space-2)]">
            <label className="caption-b text-[hsl(var(--text-inverse))]">노면</label>
            <div className="flex gap-2">
              {SURFACE_OPTIONS.map((option) => (
                <button
                  key={option}
                  onClick={() => setSurface(option)}
                  className={cn(
                    'flex-1 py-2 rounded-xl caption-m transition-colors border',
                    surface === option
                      ? 'bg-primary text-[hsl(var(--text-inverse))] border-transparent'
                      : 'bg-[hsl(var(--secondary-3))] text-[hsl(var(--secondary-5))] border-[hsl(var(--secondary-4))]',
                  )}
                >
                  {option}
                </button>
              ))}
            </div>
          </div>

          {/* 코스 타입 */}
          <div className="space-y-[var(--space-2)]">
            <label className="caption-b text-[hsl(var(--text-inverse))]">
              코스 타입{' '}
              <span className="caption-r text-[hsl(var(--secondary-5))]">(중복 가능)</span>
            </label>
            <div className="flex flex-wrap gap-2">
              {COURSE_TYPE_OPTIONS.map((option) => (
                <button
                  key={option}
                  onClick={() => toggleCourseType(option)}
                  className={cn(
                    'px-3 py-2 rounded-xl caption-m transition-colors border',
                    courseTypes.includes(option)
                      ? 'bg-primary text-[hsl(var(--text-inverse))] border-transparent'
                      : 'bg-[hsl(var(--secondary-3))] text-[hsl(var(--secondary-5))] border-[hsl(var(--secondary-4))]',
                  )}
                >
                  {option}
                </button>
              ))}
            </div>
          </div>

          {/* 업로드 버튼 */}
          <Button
            onClick={handleSubmit}
            className="w-full py-4 rounded-2xl bg-primary text-[hsl(var(--text-inverse))] body-b h-auto"
          >
            커뮤니티에 업로드
          </Button>
        </div>
      </div>
    </div>
  );
}
