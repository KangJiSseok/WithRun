import type { RunningMode } from '@/shared/types/api.type';

// 설문조사 옵션 (GET /courses/surveys)
export type CourseSurveyOption = {
  index: number;
  label: string;
};

// 설문조사 옵션 (GET /courses/surveys)
export type GetCourseSurveysResponse = {
  courseTypes: CourseSurveyOption[];
  difficulties: CourseSurveyOption[];
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
