import type { RunningFilterState } from '@/features/NormalRun';

export * from './mocks';

export const INITIAL_FILTERS: RunningFilterState = {
  purpose: '전체',
  distance: [],
  courseType: [],
  difficulty: [],
};
