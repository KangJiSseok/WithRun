export type ExerciseState = 'PREPARING' | 'ACTIVE' | 'PAUSED' | 'ENDED' | 'UNKNOWN'

export type HeartRateAccuracy = 'HIGH' | 'MEDIUM' | 'LOW' | 'NO_CONTACT' | 'UNKNOWN'

export type MetricValue = {
  value: number
  unit: string
  accuracy: HeartRateAccuracy
  timestampMs: number
}

export type SimpleMetric = {
  value: number
  unit: string
  timestampMs: number
}

export type LocationMetric = {
  latitude: number
  longitude: number
  altitude: number | null
  timestampMs: number
}

export type HealthPayload = {
  type: 'HEALTH_UPDATE'
  sessionId: string
  exerciseState: ExerciseState
  timestampMs: number
  heartRate: MetricValue | null
  distance: SimpleMetric | null
  calories: SimpleMetric | null
  speed: SimpleMetric | null
  pace: SimpleMetric | null
  steps: SimpleMetric | null
  elevationGain: SimpleMetric | null
  location: LocationMetric | null
  deviceName: string
  batteryPercent: number
}

export type WatchAck = {
  type: 'ACK' | 'PONG'
  commandType?: string
  status?: 'OK' | 'ERROR'
  errorMessage?: string | null
  id?: string
}

export type ConnectionStatus = {
  connected: boolean
  nodeId: string
  nodeName: string
}

export type BridgeMessage =
  | { type: 'health_update'; payload: HealthPayload }
  | { type: 'ack'; payload: WatchAck }
  | { type: 'connection_status'; payload: ConnectionStatus }

export type WatchCommand =
  | { type: 'START_EXERCISE' }
  | { type: 'STOP_EXERCISE' }
  | { type: 'PAUSE_EXERCISE' }
  | { type: 'RESUME_EXERCISE' }
  | { type: 'PING'; id: string }
  | { type: 'SET_INTERVAL'; intervalMs: number }

const EXERCISE_STATES: readonly ExerciseState[] = ['PREPARING', 'ACTIVE', 'PAUSED', 'ENDED', 'UNKNOWN']
const HEART_RATE_ACCURACY: readonly HeartRateAccuracy[] = ['HIGH', 'MEDIUM', 'LOW', 'NO_CONTACT', 'UNKNOWN']

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isSimpleMetric(value: unknown): value is SimpleMetric {
  if (!isRecord(value)) {
    return false
  }

  return (
    typeof value.value === 'number' &&
    typeof value.unit === 'string' &&
    typeof value.timestampMs === 'number'
  )
}

function isMetricValue(value: unknown): value is MetricValue {
  if (!isRecord(value)) {
    return false
  }

  return (
    typeof value.value === 'number' &&
    typeof value.unit === 'string' &&
    typeof value.timestampMs === 'number' &&
    typeof value.accuracy === 'string' &&
    HEART_RATE_ACCURACY.includes(value.accuracy as HeartRateAccuracy)
  )
}

function isLocationMetric(value: unknown): value is LocationMetric {
  if (!isRecord(value)) {
    return false
  }

  return (
    typeof value.latitude === 'number' &&
    typeof value.longitude === 'number' &&
    typeof value.timestampMs === 'number' &&
    (value.altitude === null || typeof value.altitude === 'number')
  )
}

export function isHealthPayload(obj: unknown): obj is HealthPayload {
  if (!isRecord(obj)) {
    return false
  }

  return (
    obj.type === 'HEALTH_UPDATE' &&
    typeof obj.sessionId === 'string' &&
    typeof obj.timestampMs === 'number' &&
    typeof obj.deviceName === 'string' &&
    typeof obj.batteryPercent === 'number' &&
    typeof obj.exerciseState === 'string' &&
    EXERCISE_STATES.includes(obj.exerciseState as ExerciseState) &&
    (obj.heartRate === null || isMetricValue(obj.heartRate)) &&
    (obj.distance === null || isSimpleMetric(obj.distance)) &&
    (obj.calories === null || isSimpleMetric(obj.calories)) &&
    (obj.speed === null || isSimpleMetric(obj.speed)) &&
    (obj.pace === null || isSimpleMetric(obj.pace)) &&
    (obj.steps === null || isSimpleMetric(obj.steps)) &&
    (obj.elevationGain === null || isSimpleMetric(obj.elevationGain)) &&
    (obj.location === null || isLocationMetric(obj.location))
  )
}
