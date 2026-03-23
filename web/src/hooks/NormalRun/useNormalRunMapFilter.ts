import { useMemo, useState } from 'react';

import {
  type CourseFilterCategory,
  type RunningCourseFilterState,
} from '@/shared/components';
import { INITIAL_FILTERS } from '@/constants/NormalRun';
import { useNormalRunCourseFilter } from './useNormalRunCourseFilter';

export const useNormalRunMapFilter = () => {
  const [showFilterDrawer, setShowFilterDrawer] = useState(false);
  const [filters, setFilters] =
    useState<RunningCourseFilterState>(INITIAL_FILTERS);
  const [appliedFilters, setAppliedFilters] =
    useState<RunningCourseFilterState>(INITIAL_FILTERS);

  const { courseFilters } = useNormalRunCourseFilter();

  const preferredDistanceMs = useMemo(() => {
    const { min, max } = appliedFilters.distanceRange;
    return [{ min: Math.max(1, min), max }];
  }, [appliedFilters.distanceRange]);

  const categories = useMemo<CourseFilterCategory[]>(() => {
    if (!courseFilters) return [];
    return [
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

  function handleFilterChange(key: 'courseType' | 'difficulty', value: string) {
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

  function handleDistanceRangeChange(range: { min: number; max: number }) {
    setFilters((prev) => ({ ...prev, distanceRange: range }));
  }

  function handleResetFilters() {
    setFilters(INITIAL_FILTERS);
    setAppliedFilters(INITIAL_FILTERS);
  }

  function applyFilters() {
    setAppliedFilters(filters);
    setShowFilterDrawer(false);
  }

  return {
    showFilterDrawer,
    setShowFilterDrawer,
    filters,
    appliedFilters,
    preferredDistanceMs,
    categories,
    handleFilterChange,
    handleDistanceRangeChange,
    handleResetFilters,
    applyFilters,
  };
};
