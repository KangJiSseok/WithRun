import { Filter, MapPin } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import {
  RunningFilterModal,
  type RunningFilterState,
} from './RunningFilterModal';
import { RunningGuideModal } from '@/shared/components/RunningGuideModal';
import { Button } from '@/shared/components/shadcn/button';
import { Tabs, TabsList, TabsTrigger } from '@/shared/components/shadcn/tabs';
import { RunningCourseCard } from '@/shared/components/RunningCourseCard';
import RunningCourseSearchBar from '@/shared/components/RunningCourseSearchBar/RunningCourseSearchBar';
import { useNormalRunCourses } from '@/hooks/NormalRun/useNormalRunCourses';
import { INITIAL_FILTERS } from '@/constants/NormalRun';
import type { ActiveCourseTab, CourseDifficulty, CourseType } from '@/shared/types/api.type';

const DIFFICULTY_LABEL: Record<CourseDifficulty, string> = {
  EASY: '쉬움',
  MEDIUM: '보통',
  HARD: '어려움',
};

const COURSE_TYPE_LABEL: Record<CourseType, string> = {
  URBAN: '도심',
  PARK: '공원',
  RIVERSIDE: '한강변',
  MOUNTAIN_TRAIL: '산/둘레길',
  TRACK: '트랙',
  OTHER: '기타',
};

export function NormalRunScreen() {
  const navigate = useNavigate();
  const { normalRunCourses, isLoading } = useNormalRunCourses();

  const [showGuide, setShowGuide] = useState(() => {
    return !localStorage.getItem('running-guide-seen');
  });
  const [activeTab, setActiveTab] = useState<ActiveCourseTab>('OFFICIAL');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [showFilterModal, setShowFilterModal] = useState(false);
  const [filters, setFilters] = useState<RunningFilterState>(INITIAL_FILTERS);
  // TODO : 북마크 API 연결 후 제거 (API 연결 전 UI 토글용 임시 오버라이드)
  const [bookmarkOverrides, setBookmarkOverrides] = useState<Record<number, boolean>>({});

  // API의 isBookmarked 값에 사용자 토글 오버라이드를 합성해 최종 북마크 Set 생성
  const savedCourses = useMemo(
    () =>
      new Set(
        normalRunCourses
          .filter((c) => bookmarkOverrides[c.courseId] ?? c.isBookmarked ?? false)
          .map((c) => c.courseId),
      ),
    [normalRunCourses, bookmarkOverrides],
  );

  function handleCloseGuide() {
    localStorage.setItem('running-guide-seen', 'true');
    setShowGuide(false);
  }

  function handleSearchKeyword(value: string) {
    setSearchKeyword(value);
  }

  // TODO : 북마크 API 연결 로직으로 바꿔야함(지금은 UI만 변경되도록 만들어짐)
  function handleToggleSave(courseId: number) {
    setBookmarkOverrides((prev) => ({
      ...prev,
      [courseId]: !savedCourses.has(courseId),
    }));
  }

  function handleFilterChange(key: keyof RunningFilterState, value: string) {
    setFilters((prev) => {
      if (value === '전체') {
        return { ...prev, [key]: [] };
      }

      const nextValues = new Set(prev[key]);
      if (nextValues.has(value)) {
        nextValues.delete(value);
      } else {
        nextValues.add(value);
      }

      return { ...prev, [key]: Array.from(nextValues) };
    });
  }

  function handleResetFilters() {
    setFilters(INITIAL_FILTERS);
  }

  const filteredCourses = normalRunCourses.filter((course) => {
    if (course.status !== activeTab) return false;

    const keyword = searchKeyword.trim().toLowerCase();
    if (keyword && !course.title.toLowerCase().includes(keyword)) return false;

    if (filters.distance.length > 0) {
      const distanceKm = course.distanceM / 1000;
      const matchesDistance = filters.distance.some((f) => {
        if (f === '5km 미만') return distanceKm < 5;
        if (f === '5km - 10km') return distanceKm >= 5 && distanceKm <= 10;
        return f === '10km 이상' && distanceKm > 10;
      });
      if (!matchesDistance) return false;
    }

    if (filters.courseType.length > 0) {
      const labels = course.courseTypes.map((t) => COURSE_TYPE_LABEL[t]);
      if (!labels.some((label) => filters.courseType.includes(label))) return false;
    }

    if (filters.difficulty.length > 0 && course.difficulty != null) {
      if (!filters.difficulty.includes(DIFFICULTY_LABEL[course.difficulty])) return false;
    }

    return true;
  });

  return (
    <>
      <div className="flex flex-col gap-[var(--space-6)] pb-[var(--space-6)]">
        <header className="flex flex-col gap-[var(--space-6)]">
          <div className="flex items-center gap-[var(--space-3)]">
            <MapPin size={28} className="text-[hsl(var(--primary-1))]" />
            <h1 className="h2-b text-[hsl(var(--text-primary))]">
              일반 러닝 코스
            </h1>
          </div>

          <div className="flex items-center gap-[var(--space-3)]">
            <RunningCourseSearchBar
              searchKeyword={searchKeyword}
              handleSearchKeyword={handleSearchKeyword}
            />
            <Button
              type="button"
              variant="outline"
              size="icon"
              onClick={() => setShowFilterModal(true)}
              className="h-14 w-14 rounded-[var(--radius-xl)] border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))]"
            >
              <Filter size={20} />
            </Button>
          </div>

          <Tabs
            value={activeTab}
            onValueChange={(value) => setActiveTab(value as ActiveCourseTab)}
            className="w-full"
          >
            <TabsList className="h-auto w-full justify-start gap-[var(--space-4)] rounded-none border-b border-[hsl(var(--border-default))] bg-transparent p-0">
              <TabsTrigger
                value="OFFICIAL"
                className="rounded-none border-b-2 border-transparent px-0 pb-[var(--space-2)] pt-0 body-l-b text-[hsl(var(--text-secondary))] shadow-none data-[state=active]:border-transparent data-[state=active]:border-b-2 data-[state=active]:border-b-[hsl(var(--primary-1))] data-[state=active]:bg-transparent data-[state=active]:text-[hsl(var(--primary-1))]"
              >
                공식 코스
              </TabsTrigger>
              <TabsTrigger
                value="COMMUNITY"
                className="rounded-none border-b-2 border-transparent px-0 pb-[var(--space-2)] pt-0 body-l-b text-[hsl(var(--text-secondary))] shadow-none data-[state=active]:border-transparent data-[state=active]:border-b-2 data-[state=active]:border-b-[hsl(var(--primary-1))] data-[state=active]:bg-transparent data-[state=active]:text-[hsl(var(--primary-1))]"
              >
                커뮤니티 코스
              </TabsTrigger>
            </TabsList>
          </Tabs>
        </header>

        <section className="flex flex-col gap-[var(--space-4)]">
          {isLoading ? (
            <div className="flex h-40 items-center justify-center">
              <p className="body-r text-[hsl(var(--text-secondary))]">
                코스를 불러오는 중입니다...
              </p>
            </div>
          ) : (
            <>
              {filteredCourses.map((course) => (
                <RunningCourseCard
                  key={course.courseId}
                  course={course}
                  isSaved={savedCourses.has(course.courseId)}
                  onClick={() => navigate(`/normal/${course.courseId}`)}
                  onToggleSave={handleToggleSave}
                />
              ))}

              {filteredCourses.length === 0 && (
                <div className="flex h-40 flex-col items-center justify-center rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] px-[var(--space-5)] text-center">
                  <p className="body-l-b text-[hsl(var(--text-primary))]">
                    검색 결과가 없습니다
                  </p>
                  <p className="body-r text-[hsl(var(--text-secondary))]">
                    코스 이름을 바꿔 검색하거나 필터를 초기화해보세요.
                  </p>
                </div>
              )}
            </>
          )}
        </section>
      </div>

      <RunningGuideModal
        isOpen={showGuide}
        onClose={handleCloseGuide}
        steps={[
          {
            title: '일반 러닝 코스',
            content:
              '공식 코스와 커뮤니티 코스를 둘 다 찾아보고 지금 바로 출발해보세요.',
          },
          {
            title: '목표에 맞게',
            content:
              "'일반 모드'에서 내 목적에 맞는 코스를 골라 천천히 페이스를 맞출 수 있습니다.",
          },
          {
            title: '기록 경쟁은',
            content:
              '일반 모드는 기록 경쟁 점수 중심이 아니라, 완주 경험 중심으로 달릴 수 있어요.',
          },
        ]}
      />

      <RunningFilterModal
        isOpen={showFilterModal}
        values={filters}
        onClose={() => setShowFilterModal(false)}
        onApply={() => setShowFilterModal(false)}
        onReset={handleResetFilters}
        onChange={handleFilterChange}
      />
    </>
  );
}
