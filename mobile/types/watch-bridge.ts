export type ExerciseState = 'PREPARING' | 'ACTIVE' | 'PAUSED' | 'ENDED' | 'UNKNOWN'

export type MetricValue = {
  value: number
  unit: string
  accuracy: string
  timestampMs: number
}

export type SimpleMetric = {
  value: number
  unit: string
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

export type WatchCommand =
  | { type: 'START_EXERCISE' }
  | { type: 'STOP_EXERCISE' }
  | { type: 'PAUSE_EXERCISE' }
  | { type: 'RESUME_EXERCISE' }
  | { type: 'PING'; id: string }
  | { type: 'SET_INTERVAL'; intervalMs: number }

/** Envelope sent from RN → WebView via injectJavaScript */
export type WebViewBridgeEnvelope =
  | { event: 'WearHealthUpdate'; data: string }
  | { event: 'WearAckReceived'; data: string }
  | { event: 'WearConnectionStatus'; data: string }
