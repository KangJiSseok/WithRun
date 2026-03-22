import type { RunningMode, CourseDifficulty, CourseType, CourseStatus, CourseRegistrationMode } from '@/shared/types/api.type';

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

// GPS 샘플 (1초 단위 수집)
export type GpsSample = {
  latitude: number;
  longitude: number;
  altitudeM: number | null;
  accuracyM: number | null;
  bearingDeg: number | null;
  sampledAt: string; // ISO 8601
};

// 헬스 샘플 (워치/모바일 1초 단위 수집)
export type HealthSample = {
  sampledAt: string; // ISO 8601
  heartRate: number;
  speedMps: number;
  paceSecPerKm: number;
  distanceM: number;
  caloriesKcal: number;
  cadenceSpm: number;
};

// 1km 단위 구간 기록 (TODO: 추후 구현)
export type SplitData = {
  userId: number;
  splitIndex: number;
  splitDistanceM: number;
  splitDurationSec: number;
  splitPaceSecPerKm: number;
  avgHeartRate: number;
  elevationGainM: number;
};

// PATCH /running-sessions/{id} 요청
export type CompleteRunningSessionRequest = {
  userId: number;
  distanceM: number;
  caloriesKcal: number;
  endLatitude: number; // -90 ~ 90
  endLongitude: number; // -180 ~ 180
  avgSpeedMps: number;
  durationSec: number;
  avgPaceSecPerKm: number;
  elevationGainM: number;
  distanceGapM: number;
  gpsSamples: GpsSample[];
  healthSamples: HealthSample[];
  splits: SplitData[];
};

// 고스트러닝 결과 타입
export type GhostRunningResult = {
  ghostRunningResultId: number;
  runningSessionId: number;
  ghostTargetRunningSessionId: number;
  targetUserId: number;
  resultStatus: 'WIN' | 'LOSE' | 'DRAW';
  point: number;
  timeGapSec: number;
  distanceGapM: number;
  createdAt: string;
};

export type SplitResult = {
  splitIndex: number;
  splitDistanceM: number;
  splitDurationSec: number;
  splitPaceSecPerKm: number;
  avgHeartRate: number;
  elevationGainM: number;
};

// PATCH /running-sessions/{id} 응답
// 자유러닝에서는 ghostRunningResult: null
export type CompleteRunningSessionResponse = {
  runningSessionId: number;
  userId: number;
  snapshotImageUrl: string | null;
  startedAt: string;
  endedAt: string;
  distanceM: number;
  durationSec: number;
  avgSpeedMps: number;
  avgPaceSecPerKm: number;
  caloriesKcal: number;
  elevationGainM: number;
  gpsSamples: GpsSample[];
  healthSamples: HealthSample[];
  splits: SplitResult[];
  ghostRunningResult: GhostRunningResult | null;
};

export type RegisterCourseRequest = {
  title: string;
  difficulty: CourseDifficulty;
  courseTypes: CourseType[];
  mode: CourseRegistrationMode;
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
