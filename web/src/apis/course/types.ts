import type {
  CourseDifficulty,
  CourseStatus,
  CourseType,
  RouteType,
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
  routeType: RouteType;
  difficulty: { data: CourseDifficulty; label: string } | null;
  distanceM: number;
  elevationGainM: number;
  snapshotImageUrl: string | null;
  startLatitude: number;
  startLongitude: number;
  endLatitude: number;
  endLongitude: number;
  coordinates: CoordinatePoint[];
  courseTypes: { data: CourseType; label: string }[];
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
  routeType: RouteType;
  distanceM: number;
  elevationGainM: number;
  startLatitude: number;
  startLongitude: number;
  endLatitude: number;
  endLongitude: number;
  distanceFromUserM: number;
  distanceFromTargetM: number;
  difficulty: { data: string; label: string } | null;
  courseTypes: { data: string; label: string }[];
  isRecommended: boolean;
  snapshotImageUrl: string | null;
  likeCount: number;
  bookmarkCount: number;
};

export type PreferredDistanceM = { min: number; max: number };

export type GetNearbyCoursesParams = {
  latitude: number;
  longitude: number;
  targetLatitude: number;
  targetLongitude: number;
  radiusM: number;
  preferredDistanceMs: PreferredDistanceM[];
  sortBy?: 'DISTANCE' | 'POPULAR';
  page?: number;
  size?: number;
};

export type GetNearbyCoursesData = {
  items: CourseItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};
