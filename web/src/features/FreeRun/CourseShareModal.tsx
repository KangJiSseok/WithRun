// 공식 코스 등록 폼 Drawer (바텀 시트)
// shadcn Drawer (vaul) 사용 - 스와이프 제스처 및 스크롤 잠금 처리
// Drawer가 열릴 때 GET /api/courses/surveys로 코스 타입·난이도 선택지를 조회해 렌더링

import { useState } from 'react';
import { Button } from '@/shared/components/shadcn/button';
import {
  Drawer,
  DrawerContent,
  DrawerHeader,
  DrawerTitle,
} from '@/shared/components/shadcn/drawer';
import { cn } from '@/shared/utils';
import type { CourseDifficulty, CourseType } from '@/shared/types/api.type';
import { useCourseSurveys } from '@/hooks/FreeRun/useCourseSurveys';
import { registerCourse } from '@/apis/runningSessions';
import { MOCK_RUNNING_SESSION_ID } from '@/shared/apis/FreeRun/courseApi';
import { useFreeRunResultStore } from '@/stores/FreeRun/useFreeRunResultStore';

type CourseShareModalProps = {
  isOpen: boolean;
  onClose: () => void;
};

// 서버 enum 라벨 -> 화면 표시 한국어 매핑
const COURSE_TYPE_LABEL: Record<string, string> = {
  RIVERSIDE: '강변',
  PARK: '공원',
  MOUNTAIN_TRAIL: '산·둘레길',
  TRACK: '트랙',
  URBAN: '도심',
  OTHER: '기타',
};

const DIFFICULTY_LABEL: Record<string, string> = {
  EASY: '쉬움',
  MEDIUM: '보통',
  HARD: '어려움',
};

export function CourseShareModal({ isOpen, onClose }: CourseShareModalProps) {
  const [courseTitle, setCourseTitle] = useState(
    `나의 러닝 ${new Date().toLocaleDateString()}`,
  );
  // 사용자가 명시적으로 선택한 난이도 (null = 미선택, API 기본값 사용)
  const [difficultyOverride, setDifficultyOverride] = useState<CourseDifficulty | null>(null);
  // 선택된 코스 타입 목록 (서버 enum 값, 복수 선택 가능)
  const [courseTypes, setCourseTypes] = useState<CourseType[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // isOpen: Drawer가 열렸는지 여부를 전달
  // 훅 내부에서 isOpen이 true가 될 때 GET /courses/surveys API 호출
  // surveyData - API 응답 (courseTypes, difficulties 배열)
  // isLoading - 로딩 중 여부 (로딩 중일 때 "로딩 중..." 표시)
  const { surveyData, isLoading } = useCourseSurveys(isOpen);

  // 추후 변경: store에서 실제 runningSessionId 읽어오기
  // const runningSessionId = useFreeRunResultStore((s) => s.runningSessionId) ?? MOCK_RUNNING_SESSION_ID;
  const runningSessionId = useFreeRunResultStore((s) => s.runningSessionId) ?? MOCK_RUNNING_SESSION_ID;
  const setCourseId = useFreeRunResultStore((s) => s.setCourseId);

  // 사용자가 선택한 값이 있으면 그 값, 없으면 API 첫 번째 항목을 기본값으로 사용
  const difficulty = difficultyOverride ?? (surveyData?.difficulties[0]?.label as CourseDifficulty | undefined);

  // 코스 타입 다중 선택 토글 함수
  function toggleCourseType(label: CourseType) {
    setCourseTypes((prev) =>
      prev.includes(label) ? prev.filter((t) => t !== label) : [...prev, label],
    );
  }

  async function handleSubmit() {
    if (!difficulty) return;
    setIsSubmitting(true);
    try {
      const res = await registerCourse(runningSessionId, {
        title: courseTitle,
        difficulty,
        courseTypes,
      });
      setCourseId(res.data.courseId);
      onClose();
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Drawer open={isOpen} onOpenChange={(open: boolean) => !open && onClose()}>
      <DrawerContent
        className="bg-[hsl(var(--secondary-2))] border-t border-[hsl(var(--secondary-3))] max-h-[90dvh]"
      >
        <DrawerHeader className="text-left px-6 pt-4 pb-0">
          <DrawerTitle className="h3-b text-[hsl(var(--text-inverse))]">공식 코스 등록</DrawerTitle>
        </DrawerHeader>

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

          {isLoading && (
            <div className="flex justify-center py-4">
              <span className="body-r text-[hsl(var(--secondary-5))]">로딩 중...</span>
            </div>
          )}

          {!isLoading && surveyData && (
            <>
              {/* 난이도 */}
              <div className="space-y-[var(--space-2)]">
                <label className="caption-b text-[hsl(var(--text-inverse))]">난이도</label>
                <div className="flex gap-2">
                  {surveyData.difficulties.map(({ label }) => (
                    <button
                      key={label}
                      onClick={() => setDifficultyOverride(label as CourseDifficulty)}
                      className={cn(
                        'flex-1 py-2 rounded-xl caption-m transition-colors border',
                        difficulty === label
                          ? 'bg-primary text-[hsl(var(--text-inverse))] border-transparent'
                          : 'bg-[hsl(var(--secondary-3))] text-[hsl(var(--secondary-5))] border-[hsl(var(--secondary-4))]',
                      )}
                    >
                      {DIFFICULTY_LABEL[label] ?? label}
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
                  {surveyData.courseTypes.map(({ label }) => (
                    <button
                      key={label}
                      onClick={() => toggleCourseType(label as CourseType)}
                      className={cn(
                        'px-3 py-2 rounded-xl caption-m transition-colors border',
                        courseTypes.includes(label as CourseType)
                          ? 'bg-primary text-[hsl(var(--text-inverse))] border-transparent'
                          : 'bg-[hsl(var(--secondary-3))] text-[hsl(var(--secondary-5))] border-[hsl(var(--secondary-4))]',
                      )}
                    >
                      {COURSE_TYPE_LABEL[label] ?? label}
                    </button>
                  ))}
                </div>
              </div>
            </>
          )}

          {/* 업로드 버튼 */}
          <Button
            onClick={handleSubmit}
            disabled={isSubmitting}
            className="w-full py-4 rounded-2xl bg-primary text-[hsl(var(--text-inverse))] body-b h-auto"
          >
            {isSubmitting ? '업로드 중...' : '커뮤니티에 업로드'}
          </Button>
        </div>
      </DrawerContent>
    </Drawer>
  );
}
