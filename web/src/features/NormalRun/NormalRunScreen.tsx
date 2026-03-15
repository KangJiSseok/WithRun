import { Bookmark, Filter, MapPin, Search, User } from 'lucide-react';
import { useEffect, useState } from 'react';

import { RunningCourseCard, type RunningCourse, type RunningCourseTab } from './RunningCourseCard';
import { RunningFilterModal, type RunningFilterState } from './RunningFilterModal';
import { RunningGuideModal } from '@/shared/components/RunningGuideModal';
import { Button } from '@/shared/components/shadcn/button';
import { Input } from '@/shared/components/shadcn/input';
import { Tabs, TabsList, TabsTrigger } from '@/shared/components/shadcn/tabs';

const COURSES: RunningCourse[] = [
  {
    id: 1,
    title: '한강 공원 루프',
    distance: '5.2 km',
    elevation: '12m',
    difficulty: '쉬움',
    purpose: ['다이어트', '건강유지'],
    likes: 1240,
    courseType: ['한강변', '공원'],
    path: 'M 52 160 C 90 125, 132 108, 164 108 S 238 124, 258 160',
    color: '#FF4444',
    description: '초보자에게 편안하고 일몰 러닝에 좋은 완만한 코스입니다.',
    tab: 'official',
  },
  {
    id: 2,
    title: '남산 산책로',
    distance: '3.8 km',
    elevation: '154m',
    difficulty: '어려움',
    purpose: ['체력향상', '대회준비'],
    likes: 856,
    courseType: ['산/둘레길', '도심'],
    path: 'M 52 160 L 108 126 L 156 150 L 220 96 L 258 112',
    color: '#FF6B6B',
    description: '도전적인 오르막 구간이 있어 심폐 능력 향상에 좋아요.',
    tab: 'official',
  },
  {
    id: 3,
    title: '올림픽 공원 서킷',
    distance: '7.5 km',
    elevation: '35m',
    difficulty: '보통',
    purpose: ['체력향상'],
    likes: 2103,
    courseType: ['공원', '트랙'],
    path: 'M 70 84 H 232 V 160 H 70 Z',
    color: '#FF8A80',
    description: '완만한 평탄 코스로 속도 유지 훈련에 적합한 루프입니다.',
    tab: 'official',
  },
  {
    id: 101,
    title: '성수동 카페거리 런',
    distance: '4.2 km',
    elevation: '8m',
    difficulty: '쉬움',
    purpose: ['다이어트', '건강유지'],
    likes: 542,
    courseType: ['도심'],
    path: 'M 52 156 L 108 112 L 160 150 L 214 106 L 258 146',
    color: '#4BAFFF',
    description: '도심 속 감성을 즐기며 부담 없이 달리기 좋은 코스입니다.',
    tab: 'community',
  },
  {
    id: 102,
    title: '서울숲 힐링 코스',
    distance: '3.5 km',
    elevation: '15m',
    difficulty: '쉬움',
    purpose: ['대회준비', '체력향상'],
    likes: 980,
    courseType: ['공원', '도심'],
    path: 'M 52 140 C 90 82, 134 82, 168 140 S 230 136, 258 112',
    color: '#2E7D32',
    description: '숲길과 도심을 오가며 편안한 리듬으로 달리기 좋습니다.',
    tab: 'community',
  },
  {
    id: 103,
    title: '여의도 한강 벚꽃길',
    distance: '6.0 km',
    elevation: '10m',
    difficulty: '보통',
    purpose: ['다이어트'],
    likes: 3201,
    courseType: ['한강변', '공원'],
    path: 'M 52 168 Q 156 82 258 154',
    color: '#FF4081',
    description: '한강의 풍경과 함께 호흡을 맞추기 좋은 경쾌한 코스입니다.',
    tab: 'community',
  },
];

const INITIAL_FILTERS: RunningFilterState = {
  purpose: '전체',
  distance: [],
  courseType: [],
  difficulty: [],
};

export function NormalRunScreen() {
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
      <div className="flex h-full flex-col gap-[var(--space-6)] overflow-y-auto pb-[var(--space-6)]">
        <header className="flex flex-col gap-[var(--space-6)]">
          <div className="flex items-center justify-between">
            <Button
              type="button"
              variant="outline"
              size="icon"
              className="h-12 w-12 rounded-full border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))]"
            >
              <User size={20} />
            </Button>

            <Button
              type="button"
              variant="outline"
              size="icon"
              className="h-12 w-12 rounded-full border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))]"
            >
              <Bookmark size={20} />
            </Button>
          </div>

          <div className="flex items-center gap-[var(--space-3)]">
            <MapPin size={28} className="text-[hsl(var(--primary-1))]" />
            <h1 className="h2-b text-[hsl(var(--text-primary))]">
              일반 러닝 코스
            </h1>
          </div>

          <div className="flex items-center gap-[var(--space-3)]">
            <div className="relative flex-1">
              <Search
                size={18}
                className="pointer-events-none absolute left-[var(--space-4)] top-1/2 -translate-y-1/2 text-[hsl(var(--text-secondary))]"
              />
              <Input
                value={searchKeyword}
                onChange={(event) => setSearchKeyword(event.target.value)}
                placeholder="코스 이름 검색..."
                className="h-14 rounded-[var(--radius-xl)] border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] pl-11 text-[hsl(var(--text-primary))] placeholder:text-[hsl(var(--text-secondary))]"
              />
            </div>

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
                className="rounded-none border-b-2 border-transparent px-0 pb-[var(--space-2)] pt-0 body-l-b text-[hsl(var(--text-secondary))] shadow-none data-[state=active]:border-[hsl(var(--primary-1))] data-[state=active]:bg-transparent data-[state=active]:text-[hsl(var(--primary-1))]"
              >
                공식 코스
              </TabsTrigger>
              <TabsTrigger
                value="community"
                className="rounded-none border-b-2 border-transparent px-0 pb-[var(--space-2)] pt-0 body-l-b text-[hsl(var(--text-secondary))] shadow-none data-[state=active]:border-[hsl(var(--primary-1))] data-[state=active]:bg-transparent data-[state=active]:text-[hsl(var(--primary-1))]"
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
      {/* TODO : Drawer로 리팩토링 하기 */}
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
