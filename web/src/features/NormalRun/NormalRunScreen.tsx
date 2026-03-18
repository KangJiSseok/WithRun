import { Filter, MapPin } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import {
  RunningFilterModal,
  type RunningFilterState,
} from './RunningFilterModal';
import { RunningGuideModal } from '@/shared/components/RunningGuideModal';
import { Button } from '@/shared/components/shadcn/button';
import { Tabs, TabsList, TabsTrigger } from '@/shared/components/shadcn/tabs';
import {
  RunningCourseCard,
  type RunningCourseTab,
} from '@/shared/components/RunningCourseCard';

// 임시 목데이터
import { COURSES, INITIAL_FILTERS } from '@/constants/NormalRun';
import RunningCourseSearchBar from '@/shared/components/RunningCourseSearchBar/RunningCourseSearchBar';

export function NormalRunScreen() {
  const navigate = useNavigate();
  const [showGuide, setShowGuide] = useState(() => {
    const hasSeenGuide = localStorage.getItem('running-guide-seen');

    return !hasSeenGuide;
  });
  const [activeTab, setActiveTab] = useState<RunningCourseTab>('official');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [showFilterModal, setShowFilterModal] = useState(false);
  const [filters, setFilters] = useState<RunningFilterState>(INITIAL_FILTERS);
  const [savedCourses, setSavedCourses] = useState<Set<number>>(() => {
    const saved = localStorage.getItem('saved-running-courses');

    return saved ? new Set(JSON.parse(saved)) : new Set();
  });

  useEffect(() => {
    localStorage.setItem(
      'saved-running-courses',
      JSON.stringify(Array.from(savedCourses))
    );
  }, [savedCourses]);

  function handleCloseGuide() {
    localStorage.setItem('running-guide-seen', 'true');
    setShowGuide(false);
  }

  // 코스 검색어 변경 함수
  function handleSearchKeyword(value: string) {
    setSearchKeyword(value);
  }

  // 코스 북마크 등록 함수
  // TODO : API 연결 로직으로 바꿔야함(지금은 UI만 변경되도록 만들어짐)
  function handleToggleSave(courseId: number) {
    setSavedCourses((prev) => {
      const next = new Set(prev);

      if (next.has(courseId)) {
        next.delete(courseId);
      } else {
        next.add(courseId);
      }

      return next;
    });
  }

  // 코스 필터 변경시 사용할 함수
  function handleFilterChange(key: keyof RunningFilterState, value: string) {
    if (key === 'purpose') {
      setFilters((prev) => ({ ...prev, purpose: value }));
      return;
    }

    setFilters((prev) => {
      if (value === '전체') {
        return {
          ...prev,
          [key]: [],
        };
      }

      const filterKey = key as Exclude<keyof RunningFilterState, 'purpose'>;
      const nextValues = new Set(prev[filterKey]);

      if (nextValues.has(value)) {
        nextValues.delete(value);
      } else {
        nextValues.add(value);
      }

      return {
        ...prev,
        [filterKey]: Array.from(nextValues),
      };
    });
  }

  function handleResetFilters() {
    setFilters(INITIAL_FILTERS);
  }

  // 필터링 목 함수(추후 삭제)
  const filteredCourses = COURSES.filter((course) => {
    const matchesTab = course.tab === activeTab;
    const keyword = searchKeyword.trim().toLowerCase();
    const matchesSearch =
      keyword === '' || course.title.toLowerCase().includes(keyword);
    const distance = Number.parseFloat(course.distance);

    const matchesPurpose =
      filters.purpose === '전체' || course.purpose.includes(filters.purpose);

    const matchesDistance =
      filters.distance.length === 0 ||
      filters.distance.some((distanceFilter) => {
        if (distanceFilter === '5km 미만') {
          return distance < 5;
        }

        if (distanceFilter === '5km - 10km') {
          return distance >= 5 && distance <= 10;
        }

        return distanceFilter === '10km 이상' && distance > 10;
      });

    const matchesCourseType =
      filters.courseType.length === 0 ||
      course.courseType.some((type) => filters.courseType.includes(type));

    const matchesDifficulty =
      filters.difficulty.length === 0 ||
      filters.difficulty.includes(course.difficulty);

    return (
      matchesTab &&
      matchesSearch &&
      matchesPurpose &&
      matchesDistance &&
      matchesCourseType &&
      matchesDifficulty
    );
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

          {/* 검색창 */}
          <div className="flex items-center gap-[var(--space-3)]">
            <RunningCourseSearchBar
              searchKeyword={searchKeyword}
              handleSearchKeyword={handleSearchKeyword}
            />

            {/* 필터 모달 활성화 버튼 */}
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
            onValueChange={(value) => setActiveTab(value as RunningCourseTab)}
            className="w-full"
          >
            <TabsList className="h-auto w-full justify-start gap-[var(--space-4)] rounded-none border-b border-[hsl(var(--border-default))] bg-transparent p-0">
              <TabsTrigger
                value="official"
                className="rounded-none border-b-2 border-transparent px-0 pb-[var(--space-2)] pt-0 body-l-b text-[hsl(var(--text-secondary))] shadow-none data-[state=active]:border-transparent data-[state=active]:border-b-2 data-[state=active]:border-b-[hsl(var(--primary-1))] data-[state=active]:bg-transparent data-[state=active]:text-[hsl(var(--primary-1))]"
              >
                공식 코스
              </TabsTrigger>
              <TabsTrigger
                value="community"
                className="rounded-none border-b-2 border-transparent px-0 pb-[var(--space-2)] pt-0 body-l-b text-[hsl(var(--text-secondary))] shadow-none data-[state=active]:border-transparent data-[state=active]:border-b-2 data-[state=active]:border-b-[hsl(var(--primary-1))] data-[state=active]:bg-transparent data-[state=active]:text-[hsl(var(--primary-1))]"
              >
                커뮤니티 코스
              </TabsTrigger>
            </TabsList>
          </Tabs>
        </header>

        {/* 필터된 코스 렌더링 섹션 */}
        <section className="flex flex-col gap-[var(--space-4)]">
          {filteredCourses.map((course) => (
            // 러닝 코스 카드 UI
            <RunningCourseCard
              key={course.id}
              course={course}
              isSaved={savedCourses.has(course.id)}
              onClick={() => navigate(`/normal/${course.id}`)}
              onToggleSave={handleToggleSave}
            />
          ))}

          {/* 필터된 코스 카드 없을시 폴백 UI */}
          {filteredCourses.length === 0 ? (
            <div className="flex h-40 flex-col items-center justify-center rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] px-[var(--space-5)] text-center">
              <p className="body-l-b text-[hsl(var(--text-primary))]">
                검색 결과가 없습니다
              </p>
              <p className="body-r text-[hsl(var(--text-secondary))]">
                코스 이름을 바꿔 검색하거나 필터를 초기화해보세요.
              </p>
            </div>
          ) : null}
        </section>
      </div>

      {/* 러닝 서비스 가이드 모달 */}
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

      {/* 일반 러닝 필터 Drawer */}
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
