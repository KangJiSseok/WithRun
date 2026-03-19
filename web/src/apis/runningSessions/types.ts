import type { RunningMode, CourseDifficulty, CourseType, CourseStatus } from '@/shared/types/api.type';

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
