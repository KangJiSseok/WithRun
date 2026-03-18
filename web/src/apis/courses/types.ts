import type { CourseStatus, CourseDifficulty, CourseType } from '../types';

// 코스 리스트
export type CourseItem = {
  courseId: number;
  title: string;
  status: CourseStatus;
  distanceM: number;
  elevationGainM: number;
  startLatitude: number;
  startLongitude: number;
  endLatitude: number;
  endLongitude: number;
  distanceFromUserM: number;
  difficulty: CourseDifficulty | null;
  courseTypes: CourseType[];
  snapshotImageUrl: string | null;
};

export type GetNearbyCoursesParams = {
  latitude: number;
  longitude: number;
  radiusM: number;
  targetDistanceM: number;
  pageSize?: number;
  cursor?: string;
};

export type GetNearbyCoursesData = {
  items: CourseItem[];
  hasMore: boolean;
  nextCursor: string | null;
  suggestedNextRadiusM: number | null;
};
