// 공식 코스 등록 폼 모달 (바텀 시트 형태)
// absolute 포지셔닝으로 MobileLayout 프레임 안에서 렌더링
// CourseRegisterModal에서 '코스 등록하기' 클릭 시 열림
// 추후 API 연동 시 handleSubmit에서 POST /courses 호출로 교체

import { useState } from 'react';
import { X } from 'lucide-react';
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
    // absolute inset-0: 외부 컨테이너 (MobileLayout 프레임 안)
    <div
      className="absolute inset-0 z-modal bg-black/50"
      onClick={onClose}
    >
      <div
        className="absolute bottom-0 left-0 right-0 flex flex-col gap-[var(--space-5)] rounded-t-3xl bg-[hsl(var(--secondary-2))] border-t border-[hsl(var(--secondary-3))] p-[var(--space-6)] max-h-[90%] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 헤더 */}
        <div className="flex items-center justify-between">
          <h3 className="h3-b text-[hsl(var(--text-inverse))]">공식 코스 등록</h3>
          <button
            onClick={onClose}
            className="p-2 text-[hsl(var(--secondary-5))] hover:text-[hsl(var(--text-inverse))] transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        <div className="space-y-[var(--space-5)]">
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
