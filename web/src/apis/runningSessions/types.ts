import type { RunningMode } from '../types';

export type CreateRunningSessionRequest = {
  userId: number;
  mode: RunningMode;
  courseId?: number;
  ghostTargetRunningSessionId?: number;
  startedAt: string; // ISO 8601 (e.g. "2026-03-17T06:30:00") -> new Date().toISOString()
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
