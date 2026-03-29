import { Filter, Home } from 'lucide-react';
import { motion } from 'motion/react';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import type { CourseItem } from '@/apis';
import {
  BOTTOM_NAV_HEIGHT,
  CoursePageNav,
  RunCircleRadialMenu,
  RunningCourseCard,
  RunningCourseFilterDrawer,
  RunningCourseSearchBar,
} from '@/shared/components';
import { Button } from '@/shared/components/shadcn/button';
import {
  useRunCircleMap,
  type CircleMode,
} from '@/shared/hooks/useRunCircleMap';
import { useBookmarkToggle, useLikeToggle } from '@/shared/hooks';
import { useRunMapFilter } from '@/shared/hooks/useRunMapFilter';
import { useRunBottomSheet } from '@/shared/hooks/useRunBottomSheet';
import { useGhostRunMapCourses } from '@/hooks/GhostRun/useGhostRunMapCourses';

export function GhostRunScreen() {
  const navigate = useNavigate();
  const { isBookmarked, toggle: toggleBookmark } = useBookmarkToggle();
  const { isLiked, getLikeCount, toggle: toggleLike } = useLikeToggle();
  const [searchKeyword, setSearchKeyword] = useState('');
  const [circleMode, setCircleMode] = useState<CircleMode>('USER_LOCATION');
  const [mapCourses, setMapCourses] = useState<CourseItem[]>([]);

  const {
    showFilterDrawer,
    setShowFilterDrawer,
    filters,
    preferredDistanceMs,
    categories,
    handleFilterChange,
    handleDistanceRangeChange,
    handleResetFilters,
    applyFilters,
  } = useRunMapFilter();

  const {
    sheetRef,
    headerRef,
    sheetHeight,
    headerHeight,
    isSheetExpanded,
    isDragging,
    dragControls,
    collapse,
    expand,
    startDrag,
    handleDragStart,
    handleDragEnd,
  } = useRunBottomSheet();

  const { containerRef, center, radiusM } = useRunCircleMap(
    mapCourses,
    circleMode,
    collapse,
    '--info'
  );

  const { courses, isLoading, currentPage, totalPages, goToPage } =
    useGhostRunMapCourses(center, radiusM, preferredDistanceMs);

  useEffect(() => {
    setMapCourses(courses);
  }, [courses]);

  useEffect(() => {
    if (!isLoading && courses.length > 0) {
      expand();
    }
  }, [isLoading, courses.length, expand]);

  useEffect(() => {
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = '';
    };
  }, []);

  const filteredCourses = courses.filter((course) => {
    const keyword = searchKeyword.trim().toLowerCase();
    return !keyword || course.title.toLowerCase().includes(keyword);
  });

  return (
    <div className="relative h-dvh w-full overflow-hidden">
      {/* 카카오맵 컨테이너 */}
      <div
        ref={containerRef}
        className="h-full w-full"
        style={{ touchAction: 'none' }}
      />

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
          className="h-10 w-10 shrink-0 rounded-full border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))] shadow-[var(--shadow-md)]"
        >
          <Home size={18} />
        </Button>
        <RunningCourseSearchBar
          searchKeyword={searchKeyword}
          handleSearchKeyword={setSearchKeyword}
          compact
        />
      </div>

      {/* 필터 버튼 */}
      <div className="absolute right-4 top-4 z-20">
        <button
          onClick={() => setShowFilterDrawer(true)}
          className="flex h-10 w-10 items-center justify-center rounded-full border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))] shadow-[var(--shadow-md)]"
        >
          <Filter size={18} />
        </button>
      </div>

      {/* Radial Menu */}
      <RunCircleRadialMenu
        circleMode={circleMode}
        onCircleModeChange={setCircleMode}
      />

      {/* 드래그 가능한 바텀시트 */}
      <motion.div
        ref={sheetRef}
        drag="y"
        dragControls={dragControls}
        dragListener={false}
        dragConstraints={{
          top: 0,
          bottom: sheetHeight - headerHeight - BOTTOM_NAV_HEIGHT,
        }}
        dragElastic={0}
        dragMomentum={false}
        animate={{
          y: isSheetExpanded
            ? 0
            : sheetHeight - headerHeight - BOTTOM_NAV_HEIGHT,
        }}
        transition={{ type: 'spring', damping: 30, stiffness: 300 }}
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
        className="absolute bottom-0 left-0 right-0 z-10 flex max-h-[60dvh] flex-col rounded-t-[var(--radius-2xl)] bg-[hsl(var(--surface-default))] shadow-[var(--shadow-soft-xl)]"
      >
        {/* 시트 헤더 */}
        <div
          ref={headerRef}
          className="cursor-grab active:cursor-grabbing"
          style={{ touchAction: 'none' }}
          onPointerDown={startDrag}
        >
          {/* 드래그 바 */}
          <div className="flex justify-center pt-3 pb-2">
            <div className="h-1 w-10 rounded-full bg-[hsl(var(--border-default))]" />
          </div>

          {/* 헤더 라벨 */}
          <div className="px-4 pb-3">
            <span className="body-l-b text-[hsl(var(--text-primary))]">
              고스트런 코스
            </span>
          </div>
        </div>

        {/* 스크롤 가능한 코스 리스트 */}
        <div
          className={`flex-1 overflow-y-auto px-(--space-5) py-(--space-4) ${isDragging ? 'pointer-events-none' : ''}`}
          style={{ touchAction: 'pan-y', paddingBottom: 16 }}
        >
          {isLoading && courses.length === 0 ? (
            <div className="flex h-24 items-center justify-center">
              <p className="body-r text-[hsl(var(--text-secondary))]">
                코스를 불러오는 중입니다...
              </p>
            </div>
          ) : filteredCourses.length === 0 ? (
            <div className="flex h-24 flex-col items-center justify-center rounded-xl border border-[hsl(var(--border-default))] px-(--space-5) text-center">
              <p className="body-l-b text-[hsl(var(--text-primary))]">
                검색 결과가 없습니다
              </p>
              <p className="body-r text-[hsl(var(--text-secondary))]">
                지도에서 탐색 범위를 넓혀보세요.
              </p>
            </div>
          ) : (
            <div className="flex flex-col gap-(--space-4)">
              {filteredCourses.map((course) => (
                <RunningCourseCard
                  key={course.courseId}
                  course={{
                    ...course,
                    isLiked: isLiked(course.courseId, course.isLiked),
                    likeCount: getLikeCount(course.courseId, course.likeCount),
                  }}
                  isSaved={isBookmarked(course.courseId, course.isBookmarked)}
                  onClick={() => navigate(`/ghost-run/${course.courseId}`)}
                  onToggleSave={() => toggleBookmark(course.courseId, course.isBookmarked)}
                  onToggleLike={() => toggleLike(course.courseId, course.isLiked, course.likeCount)}
                />
              ))}
            </div>
          )}
        </div>

        {/* 페이지 네비게이션 */}
        {totalPages > 0 && (
          <div
            className="shrink-0 py-2"
            style={{ paddingBottom: BOTTOM_NAV_HEIGHT + 8 }}
          >
            <CoursePageNav
              currentPage={currentPage + 1}
              totalPages={totalPages}
              onPageChange={(p: number) => goToPage(p - 1)}
            />
          </div>
        )}
      </motion.div>

      <RunningCourseFilterDrawer
        isOpen={showFilterDrawer}
        categories={categories}
        values={filters}
        onClose={() => setShowFilterDrawer(false)}
        onApply={applyFilters}
        onReset={handleResetFilters}
        onChange={handleFilterChange}
        onDistanceRangeChange={handleDistanceRangeChange}
      />
    </div>
  );
}
