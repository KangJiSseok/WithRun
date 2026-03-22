import type {
  CourseDifficulty,
  CourseStatus,
  CourseType,
} from '@/shared/types/api.type';

// 코스 설문 선택지 옵션 (GET /meta/course/register)
export type CourseSurveyOption = {
  data: string;   // 서버 enum 값 (예: "RIVERSIDE", "EASY", "COMMUNITY")
  label: string;  // 화면 표시 한국어 (예: "강변", "쉬움", "커뮤니티 코스")
};

// 코스 설문 선택지 응답 (GET /meta/course/register)
export type GetCourseSurveysResponse = {
  courseTypes: CourseSurveyOption[];
  difficulties: CourseSurveyOption[];
  mode: CourseSurveyOption[];
};


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
  pageSize?: number;
  cursor?: string;
};

export type GetNearbyCoursesData = {
  items: CourseItem[];
  hasMore: boolean;
  nextCursor: string | null;
  suggestedNextRadiusM: number | null;
};
