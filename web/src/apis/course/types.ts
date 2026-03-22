import type {
  CourseStatus,
  CourseDifficulty,
  CourseType,
} from '@/shared/types/api.type';

export type CoordinatePoint = {
  latitude: number;
  longitude: number;
  elevationM: number;
};

// 코스 상세
export type CourseDetail = {
  courseId: number;
  title: string;
  status: CourseStatus;
  difficulty: CourseDifficulty | null;
  distanceM: number;
  elevationGainM: number;
  snapshotImageUrl: string | null;
  startLatitude: number;
  startLongitude: number;
  endLatitude: number;
  endLongitude: number;
  coordinates: CoordinatePoint[];
  courseTypes: CourseType[];
  likeCount: number;
  isLiked: boolean;
  isBookmarked: boolean;
  averageRating: number | null;
};

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
  // 아래 3개는 백엔드에서 누락된 필드값이라 임시로 옵셔널 처리함
  likeCount?: number;
  isLiked?: boolean;
  isBookmarked?: boolean;
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

