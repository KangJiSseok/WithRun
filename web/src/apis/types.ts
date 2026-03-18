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
