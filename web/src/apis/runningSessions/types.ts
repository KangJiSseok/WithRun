import type { RunningMode, CourseDifficulty, CourseType, CourseStatus } from '@/shared/types/api.type';

// GET /running-sessions/history/past - 과거 러닝 세션 항목
export type RunningHistoryItem = {
  runningSessionId: number;
  startedAt: string;
  distanceM: number;
  durationSec: number | null;
  caloriesKcal: number;
  snapshotImageUrl: string | null;
  elevationGainM: number;
  isCompleted: boolean;
  runningMode: string;
  ghostResultStatus: string | null;
  timeSlot: string;
};

// GET /running-sessions/history/past 응답
export type GetRunningHistoryResponse = {
  items: RunningHistoryItem[];
  hasMore: boolean;
  nextCursor: string | null;
};

// GET /running-sessions/history/past 쿼리 파라미터
export type GetRunningHistoryParams = {
  year?: number;
  month?: number;
  day?: number;
  cursor?: string;
  pageSize?: number;
};

export type CreateRunningSessionRequest = {
  userId: number;
  mode: RunningMode;
  courseId?: number;
  ghostTargetRunningSessionId?: number;
  startLatitude: number; // -90 ~ 90
  startLongitude: number; // -180 ~ 180
};

export type RunningSessionData = {
  runningSessionId: number;
  mode: RunningMode;
  courseId: number | null;
  ghostTargetRunningSessionId: number | null;
  distanceM: number;
  caloriesKcal: number;
  isCompleted: boolean;
};

export type RegisterCourseRequest = {
  title: string;
  difficulty: CourseDifficulty;
  courseTypes: CourseType[];
  snapshotImageUrl?: string;
};

export type RegisterCourseResponse = {
  courseId: number;
  title: string;
  status: CourseStatus;
  difficulty: CourseDifficulty;
  distanceM: number;
  elevationGainM: number;
  snapshotImageUrl: string | null;
  startLatitude: number;
  startLongitude: number;
  endLatitude: number | null;
  endLongitude: number | null;
  coordinates: { latitude: number; longitude: number; elevation: number }[];
};
