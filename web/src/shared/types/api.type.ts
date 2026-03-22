// API 공용 response 스키마
export type ApiResponse<T = undefined> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
  traceId: string;
};

// 코스 종류
export type CourseStatus = 'OFFICIAL' | 'COMMUNITY' | 'PRIVATE';
// 탭에서 표시할 코스 상태 (PRIVATE 제외)
export type ActiveCourseTab = Exclude<CourseStatus, 'PRIVATE'>;
// 코스 난이도
export type CourseDifficulty = 'EASY' | 'MEDIUM' | 'HARD';
// 코스 유형
export type CourseType =
  | 'RIVERSIDE'
  | 'PARK'
  | 'MOUNTAIN_TRAIL'
  | 'TRACK'
  | 'URBAN'
  | 'OTHER';
// 러닝 모드
export type RunningMode = 'FREE' | 'COURSE' | 'GHOST';
// 코스 등록 모드 (개인 저장 or 커뮤니티 공개)
// 백엔드 survey API에서 선택지로 반환 예정
export type CourseRegistrationMode = 'PRIVATE' | 'COMMUNITY';
