import { Filter, MapPin } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import {
  RunningCourseCard,
  RunningCourseFilterDrawer,
  RunningCourseSearchBar,
  TabTitle,
  type CourseFilterCategory,
  type RunningCourseFilterState,
} from '@/shared/components';
import { RunningGuideModal } from '@/shared/components/RunningGuideModal';
import { Button } from '@/shared/components/shadcn/button';
import { Tabs, TabsList, TabsTrigger } from '@/shared/components/shadcn/tabs';
import { useNormalRunCourseFilter } from '@/hooks/NormalRun/useNormalRunCourseFilter';
import {
  useNormalRunCourses,
  DEFAULT_TARGET_DISTANCE_M,
} from '@/hooks/NormalRun/useNormalRunCourses';
import { INITIAL_FILTERS } from '@/constants/NormalRun';
import type { ActiveCourseTab } from '@/shared/types/api.type';

export function NormalRunScreen() {
  const navigate = useNavigate();

  const [showGuide, setShowGuide] = useState(() => {
    return !localStorage.getItem('running-guide-seen');
  });
  const [activeTab, setActiveTab] = useState<ActiveCourseTab>('OFFICIAL');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [showFilterDrawer, setShowFilterDrawer] = useState(false);
  // 드로어 내 선택 중인 임시 상태 (버튼 클릭마다 업데이트, API 미호출)
  const [filters, setFilters] =
    useState<RunningCourseFilterState>(INITIAL_FILTERS);
  // 필터 적용 버튼 클릭 시 확정되는 상태 (API 호출 트리거)
  const [appliedFilters, setAppliedFilters] =
    useState<RunningCourseFilterState>(INITIAL_FILTERS);
  // TODO : 북마크 API 연결 후 제거 (API 연결 전 UI 토글용 임시 오버라이드)
  const [bookmarkOverrides, setBookmarkOverrides] = useState<
    Record<number, boolean>
  >({});

  // 거리 선택 → targetDistanceM 파싱 (예: "5" → 5000m)
  const targetDistanceM = useMemo(() => {
    const d = appliedFilters.distance[0];
    if (!d) return DEFAULT_TARGET_DISTANCE_M;
    const km = parseInt(d);
    return isNaN(km) ? DEFAULT_TARGET_DISTANCE_M : km * 1000;
  }, [appliedFilters.distance]);

  const { normalRunCourses, isLoading } = useNormalRunCourses(targetDistanceM);
  const { courseFilters } = useNormalRunCourseFilter();

  // API 응답 기반 동적 filter categories 구성
  const categories = useMemo<CourseFilterCategory[]>(() => {
    if (!courseFilters) return [];
    return [
      {
        key: 'distance',
        label: '거리',
        options: [
          { value: '전체', label: '전체' },
          ...courseFilters.courseDistanceTypes.map((o) => ({
            value: o.data,
            label: o.label,
          })),
        ],
      },
      {
        key: 'courseType',
        label: '코스 타입',
        options: [
          { value: '전체', label: '전체' },
          ...courseFilters.courseTypes.map((o) => ({
            value: o.data,
            label: o.label,
          })),
        ],
      },
      {
        key: 'difficulty',
        label: '난이도',
        options: [
          { value: '전체', label: '전체' },
          ...courseFilters.difficulties.map((o) => ({
            value: o.data,
            label: o.label,
          })),
        ],
      },
    ];
  }, [courseFilters]);

  // API의 isBookmarked 값에 사용자 토글 오버라이드를 합성해 최종 북마크 Set 생성
  const savedCourses = useMemo(
    () =>
      new Set(
        normalRunCourses
          .filter(
            (c) => bookmarkOverrides[c.courseId] ?? c.isBookmarked ?? false
          )
          .map((c) => c.courseId)
      ),
    [normalRunCourses, bookmarkOverrides]
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

  function handleFilterChange(
    key: keyof RunningCourseFilterState,
    value: string
  ) {
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
    setAppliedFilters(INITIAL_FILTERS);
  }

  // 탭 + 키워드 검색만 적용 (courseType, difficulty는 향후 백엔드 지원 시 연결)
  const filteredCourses = normalRunCourses.filter((course) => {
    if (course.status !== activeTab) return false;

    const keyword = searchKeyword.trim().toLowerCase();
    if (keyword && !course.title.toLowerCase().includes(keyword)) return false;

    return true;
  });

  return (
    <>
      <div className="flex flex-col gap-[var(--space-6)] pb-[var(--space-6)]">
        <header className="flex flex-col gap-[var(--space-6)]">
          <TabTitle icon={MapPin} title="일반 러닝 코스" />

          <div className="flex items-center gap-[var(--space-3)]">
            <RunningCourseSearchBar
              searchKeyword={searchKeyword}
              handleSearchKeyword={handleSearchKeyword}
            />
            <Button
              type="button"
              variant="outline"
              size="icon"
              onClick={() => setShowFilterDrawer(true)}
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
                  onClick={() => navigate(`/normal-run/${course.courseId}`)}
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

      <RunningCourseFilterDrawer
        isOpen={showFilterDrawer}
        categories={categories}
        values={filters}
        onClose={() => setShowFilterDrawer(false)}
        onApply={() => {
          setAppliedFilters(filters);
          setShowFilterDrawer(false);
        }}
        onReset={handleResetFilters}
        onChange={handleFilterChange}
      />
    </>
  );
}
