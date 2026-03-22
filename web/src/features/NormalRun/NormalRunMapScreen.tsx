import { Filter, Home, MapPin } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
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

const HANDLE_HEIGHT = 28;

const MODE_BUTTONS: { mode: CircleMode; label: string; dx: number; dy: number }[] = [
  { mode: 'USER_LOCATION', label: '내 위치', dx: -62, dy: -36 },
  { mode: 'AREA_NEARBY', label: '지역 근처', dx: -70, dy: 19 },
  { mode: 'AREA_EXPLORE', label: '지역 탐색', dx: -36, dy: 62 },
];

export function NormalRunMapScreen() {
  const navigate = useNavigate();

  const [searchKeyword, setSearchKeyword] = useState('');
  const [showFilterDrawer, setShowFilterDrawer] = useState(false);
  const [circleMode, setCircleMode] = useState<CircleMode>('USER_LOCATION');
  const [filters, setFilters] =
    useState<RunningCourseFilterState>(INITIAL_FILTERS);
  const [appliedFilters, setAppliedFilters] =
    useState<RunningCourseFilterState>(INITIAL_FILTERS);
  const [isSheetExpanded, setIsSheetExpanded] = useState(true);
  const [radialOpen, setRadialOpen] = useState(false);

  const sheetRef = useRef<HTMLDivElement>(null);
  const [sheetHeight, setSheetHeight] = useState(0);

  // 지도 마커용 코스 상태 (useNormalRunCircleMap에 전달)
  const [mapCourses, setMapCourses] = useState<CourseItem[]>([]);

  // 거리 필터 → targetDistanceM
  const targetDistanceM = useMemo(() => {
    const d = appliedFilters.distance[0];
    if (!d) return DEFAULT_TARGET_DISTANCE_M;
    const km = parseInt(d);
    return isNaN(km) ? DEFAULT_TARGET_DISTANCE_M : km * 1000;
  }, [appliedFilters.distance]);

  const handleMapTouched = useCallback(() => {
    setIsSheetExpanded(false);
  }, []);

  // 지도 + 인터랙티브 원 훅 (mapCourses로 마커 갱신)
  const { containerRef, center, radiusM } = useNormalRunCircleMap(
    mapCourses,
    circleMode,
    handleMapTouched
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

  // 바텀시트 높이 측정
  useEffect(() => {
    if (!sheetRef.current) return;
    const observer = new ResizeObserver(([entry]) => {
      setSheetHeight(entry.contentRect.height);
    });
    observer.observe(sheetRef.current);
    return () => observer.disconnect();
  }, []);

  // 코스 로드 완료 시 바텀시트 자동 올림
  useEffect(() => {
    if (!isLoading && courses.length > 0) {
      setIsSheetExpanded(true);
    }
  }, [isLoading, courses.length]);

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

      {/* 플로팅 검색바 (좌상단) */}
      <div
        className="absolute left-4 top-4 z-10 flex items-center gap-2"
        style={{ right: '104px' }}
      >
        <Button
          type="button"
          variant="outline"
          size="icon"
          onClick={() => navigate('/home')}
          className="h-10 w-10 shrink-0 rounded-full border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))] shadow-lg"
        >
          <Home size={18} />
        </Button>
        <RunningCourseSearchBar
          searchKeyword={searchKeyword}
          handleSearchKeyword={setSearchKeyword}
          compact
        />
      </div>

      {/* Radial Menu (우상단) */}
      <div className="absolute right-4 top-4 z-20">
        {radialOpen && (
          <div
            className="fixed inset-0 z-[-1]"
            onClick={() => setRadialOpen(false)}
          />
        )}

        {/* Trigger 버튼 */}
        <button
          onClick={() => setRadialOpen((v) => !v)}
          className={`flex h-10 w-10 items-center justify-center rounded-full border shadow-lg ${
            radialOpen
              ? 'border-[hsl(var(--primary-1))] bg-[hsl(var(--primary-1))] text-white'
              : 'border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))]'
          }`}
        >
          <MapPin size={18} />
        </button>

        {/* 모드 버튼들 (AnimatePresence) */}
        <AnimatePresence>
          {radialOpen &&
            MODE_BUTTONS.map(({ mode, label, dx, dy }) => (
              <motion.button
                key={mode}
                initial={{ x: 0, y: 0, opacity: 0, scale: 0 }}
                animate={{ x: dx, y: dy, opacity: 1, scale: 1 }}
                exit={{ x: 0, y: 0, opacity: 0, scale: 0 }}
                transition={{ type: 'spring', damping: 20, stiffness: 300 }}
                className={`absolute right-0 top-0 flex h-8 items-center justify-center whitespace-nowrap rounded-full border px-2 text-xs font-medium shadow-lg ${
                  circleMode === mode
                    ? 'border-[hsl(var(--primary-1))] bg-[hsl(var(--primary-1))] text-white'
                    : 'border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))]'
                }`}
                onClick={() => {
                  setCircleMode(mode);
                  setRadialOpen(false);
                }}
              >
                {label}
              </motion.button>
            ))}
        </AnimatePresence>
      </div>

      {/* 필터 버튼 (우상단, Radial 아래 독립 배치) */}
      <div className="absolute right-4 top-[64px] z-20">
        <button
          onClick={() => setShowFilterDrawer(true)}
          className="flex h-10 w-10 items-center justify-center rounded-full border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))] shadow-lg"
        >
          <Filter size={18} />
        </button>
      </div>

      {/* 드래그 가능한 바텀시트 */}
      <motion.div
        ref={sheetRef}
        drag="y"
        dragConstraints={{ top: 0, bottom: sheetHeight - HANDLE_HEIGHT }}
        dragElastic={0.05}
        animate={{ y: isSheetExpanded ? 0 : sheetHeight - HANDLE_HEIGHT }}
        transition={{ type: 'spring', damping: 30, stiffness: 300 }}
        onDragEnd={(_, info) => {
          const mid = (sheetHeight - HANDLE_HEIGHT) / 2;
          if (info.offset.y > mid || info.velocity.y > 500) {
            setIsSheetExpanded(false);
          } else {
            setIsSheetExpanded(true);
          }
        }}
        className="absolute bottom-0 left-0 right-0 z-10 flex max-h-[60dvh] flex-col rounded-t-[var(--radius-2xl)] bg-[hsl(var(--surface-default))] shadow-[0_-4px_20px_rgba(0,0,0,0.12)]"
      >
        {/* 핸들 바 (터치 타겟) */}
        <div className="flex shrink-0 cursor-grab justify-center py-[var(--space-3)] active:cursor-grabbing">
          <div className="h-1 w-10 rounded-full bg-[hsl(var(--border-default))]" />
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
      </motion.div>

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
