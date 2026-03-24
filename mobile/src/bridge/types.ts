// 웹-모바일 공유 타입 (모바일 팀 관리)
// bridge.ts 변경 시 여기도 함께 업데이트

// @webview-bridge/types 인라인 (외부 의존성 없이 타입만 정의)
type _OnlyJSON<T> = {
  [P in keyof T as T[P] extends string | number | boolean | null | undefined
    ? P
    : never]: T[P]
}
type _BridgeStore<T> = {
  getState: () => T
  setState: (newState: Partial<_OnlyJSON<T>>) => void
  subscribe: (listener: (newState: T, prevState: T) => void) => () => void
}

// ─── 공유 데이터 타입 ────────────────────────────────────────────

export type WatchHealthData = {
  timestamp: number
  heartRate: number
  caloriesKcal: number
  cadenceSpm: number   // Samsung Health SPM (모바일 가속도계보다 정확)
  distanceM: number    // 만보계 기반 누적 거리(m)
}

export type GpsLocation = {
  latitude: number
  longitude: number
  accuracy: number | null
  altitude: number | null
  heading: number | null
  speedMps: number | null    // GPS Doppler 직접 측정값 (가속도계 추정보다 정확)
  cadenceSpm: number | null  // 모바일 가속도계 기반 케이던스 (워치 SPM 대체)
}

// ─── 브릿지 타입 (bridge.ts와 동기화 필요) ───────────────────────

type _AppBridgeMethods = {
  whoRU(): Promise<string>
  notifyCourseDeviation(count: 1 | 2 | 3): Promise<void>
  notifyCourseDeviationForceStop(): Promise<void>
}

// bridge() 반환 타입과 구조적으로 동일
export type AppBridge = _BridgeStore<_AppBridgeMethods>

// postMessageSchema() 반환 타입과 구조적으로 동일
export type AndroidPostMessageSchema = {
  watchMessage: { validate: (data: unknown) => { message: string } }
  gpsLocation: { validate: (data: unknown) => GpsLocation }
  watchHealthData: { validate: (data: unknown) => WatchHealthData }
}
