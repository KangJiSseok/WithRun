import { Filter, Home } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import type { CourseItem } from '@/apis';
import {
  RunningCourseCard,
  RunningCourseFilterDrawer,
  RunningCourseSearchBar,
  type CourseFilterCategory,
  type RunningCourseFilterState,
} from '@/shared/components';
import { Button } from '@/shared/components/shadcn/button';
import { INITIAL_FILTERS } from '@/constants/NormalRun';
import { useNormalRunCourseFilter } from '@/hooks/NormalRun/useNormalRunCourseFilter';
import {
  useNormalRunCircleMap,
  type CircleMode,
} from '@/hooks/NormalRun/useNormalRunCircleMap';
import {
  useNormalRunMapCourses,
  DEFAULT_TARGET_DISTANCE_M,
} from '@/hooks/NormalRun/useNormalRunMapCourses';

export function NormalRunMapScreen() {
  const navigate = useNavigate();

  const [searchKeyword, setSearchKeyword] = useState('');
  const [showFilterDrawer, setShowFilterDrawer] = useState(false);
  const [circleMode, setCircleMode] = useState<CircleMode>('USER_LOCATION');
  const [filters, setFilters] =
    useState<RunningCourseFilterState>(INITIAL_FILTERS);
  const [appliedFilters, setAppliedFilters] =
    useState<RunningCourseFilterState>(INITIAL_FILTERS);

  // 지도 마커용 코스 상태 (useNormalRunCircleMap에 전달)
  const [mapCourses, setMapCourses] = useState<CourseItem[]>([]);

  // 거리 필터 → targetDistanceM
  const targetDistanceM = useMemo(() => {
    const d = appliedFilters.distance[0];
    if (!d) return DEFAULT_TARGET_DISTANCE_M;
    const km = parseInt(d);
    return isNaN(km) ? DEFAULT_TARGET_DISTANCE_M : km * 1000;
  }, [appliedFilters.distance]);

  // 지도 + 인터랙티브 원 훅 (mapCourses로 마커 갱신)
  const { containerRef, center, radiusM } = useNormalRunCircleMap(
    mapCourses,
    circleMode
  );

  // center/radiusM 변경 시 API 호출, 스크롤 다운 시 다음 페이지 로드
  const { courses, isLoading, hasMore, loadMore } = useNormalRunMapCourses(
    center,
    radiusM,
    targetDistanceM
  );

  // courses 변경 시 지도 마커 갱신
  useEffect(() => {
    setMapCourses(courses);
  }, [courses]);

  const { courseFilters } = useNormalRunCourseFilter();

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

  // courses/nearby는 OFFICIAL 코스만 반환 → status 필터 불필요
  const filteredCourses = courses.filter((course) => {
    const keyword = searchKeyword.trim().toLowerCase();
    if (keyword && !course.title.toLowerCase().includes(keyword)) return false;
    return true;
  });

  function handleScroll(e: React.UIEvent<HTMLDivElement>) {
    const el = e.currentTarget;
    if (
      el.scrollHeight - el.scrollTop - el.clientHeight < 80 &&
      hasMore &&
      !isLoading
    ) {
      loadMore();
    }
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

  return (
    <div className="relative h-dvh w-full overflow-hidden">
      {/* 카카오맵 컨테이너 */}
      <div ref={containerRef} className="h-full w-full" />

      {/* 홈 버튼 (지도 좌상단) */}
      <div className="absolute left-4 top-4 z-10">
        <Button
          type="button"
          variant="outline"
          size="icon"
          onClick={() => navigate('/home')}
          className="h-12 w-12 rounded-full border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))] shadow-lg"
        >
          <Home size={20} />
        </Button>
      </div>

      {/* 탐색 모드 세그먼트 버튼 (지도 우상단) */}
      <div className="absolute right-4 top-4 z-10 flex overflow-hidden rounded-xl shadow-lg">
        {(
          [
            { mode: 'USER_LOCATION', label: '내 위치' },
            { mode: 'AREA_NEARBY', label: '지역 근처' },
            { mode: 'AREA_EXPLORE', label: '지역 탐색' },
          ] as { mode: CircleMode; label: string }[]
        ).map(({ mode, label }) => (
          <Button
            key={mode}
            type="button"
            size="sm"
            onClick={() => setCircleMode(mode)}
            className={
              circleMode === mode
                ? 'rounded-none px-3 py-2 body-s-b bg-[hsl(var(--primary-1))] text-white'
                : 'rounded-none px-3 py-2 body-s-b border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))]'
            }
          >
            {label}
          </Button>
        ))}
      </div>

      {/* 바텀시트 */}
      <div className="absolute bottom-0 left-0 right-0 z-10 flex max-h-[60vh] flex-col rounded-t-[var(--radius-2xl)] bg-[hsl(var(--surface-default))] shadow-[0_-4px_20px_rgba(0,0,0,0.12)]">
        {/* 핸들 바 */}
        <div className="flex justify-center py-[var(--space-3)]">
          <div className="h-1 w-10 rounded-full bg-[hsl(var(--border-default))]" />
        </div>

        {/* 고정 헤더: 검색 + 필터 버튼 */}
        <div className="flex items-center gap-(--space-3) px-(--space-5) pb-(--space-3)">
          <RunningCourseSearchBar
            searchKeyword={searchKeyword}
            handleSearchKeyword={setSearchKeyword}
          />
          <Button
            type="button"
            variant="outline"
            size="icon"
            onClick={() => setShowFilterDrawer(true)}
            className="h-14 w-14 shrink-0 rounded-xl border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))]"
          >
            <Filter size={20} />
          </Button>
        </div>

        {/* 스크롤 가능한 코스 리스트 */}
        <div
          className="flex-1 overflow-y-auto px-(--space-5) py-(--space-4)"
          onScroll={handleScroll}
        >
          {isLoading && courses.length === 0 ? (
            <div className="flex h-24 items-center justify-center">
              <p className="body-r text-[hsl(var(--text-secondary))]">
                코스를 불러오는 중입니다...
              </p>
            </div>
          ) : filteredCourses.length === 0 ? (
            <div className="flex h-24 flex-col items-center justify-center rounded-xl border border-[hsl(var(--border-default))] px-[var(--space-5)] text-center">
              <p className="body-l-b text-[hsl(var(--text-primary))]">
                검색 결과가 없습니다
              </p>
              <p className="body-r text-[hsl(var(--text-secondary))]">
                지도에서 탐색 범위를 넓혀보세요.
              </p>
            </div>
          ) : (
            <div className="flex flex-col gap-[var(--space-4)]">
              {filteredCourses.map((course) => (
                <RunningCourseCard
                  key={course.courseId}
                  course={course}
                  isSaved={course.isBookmarked ?? false}
                  onClick={() => navigate(`/normal-run/${course.courseId}`)}
                  onToggleSave={() => {}}
                />
              ))}
              {isLoading && (
                <p className="py-4 text-center body-r text-[hsl(var(--text-secondary))]">
                  불러오는 중...
                </p>
              )}
            </div>
          )}
        </div>
      </div>

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
    </div>
  );
}
