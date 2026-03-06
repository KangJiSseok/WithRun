import { useEffect, useMemo, useState } from 'react'

import type { ExerciseState, HealthPayload, HeartRateAccuracy, SimpleMetric } from '../types/watch-bridge'

type HealthDashboardProps = {
  payload: HealthPayload | null
}

type MetricRowProps = {
  label: string
  value: string
}

function formatDistance(metric: SimpleMetric | null): string {
  if (!metric || !Number.isFinite(metric.value)) {
    return '—'
  }

  const normalizedMeters = Math.max(0, metric.value)
  return `${(normalizedMeters / 1000).toFixed(2)} km`
}

function formatCalories(metric: SimpleMetric | null): string {
  if (!metric) {
    return '—'
  }

  return `${metric.value.toFixed(0)} kcal`
}

function formatSpeed(metric: SimpleMetric | null): string {
  if (!metric || !Number.isFinite(metric.value)) {
    return '—'
  }

  return `${(metric.value * 3.6).toFixed(1)} km/h`
}

function formatPace(metric: SimpleMetric | null): string {
  if (!metric || !Number.isFinite(metric.value) || metric.value <= 0) {
    return '—'
  }

  const seconds = Math.max(0, Math.floor(metric.value))
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60

  return `${minutes}:${String(remainingSeconds).padStart(2, '0')} /km`
}

function formatSteps(metric: SimpleMetric | null): string {
  if (!metric || !Number.isFinite(metric.value)) {
    return '—'
  }

  return `${Math.max(0, Math.round(metric.value)).toLocaleString()} steps`
}

function formatElevationGain(metric: SimpleMetric | null): string {
  if (!metric || !Number.isFinite(metric.value)) {
    return '—'
  }

  return `${Math.max(0, metric.value).toFixed(0)} m`
}

function formatLocation(
  latitude: number | undefined,
  longitude: number | undefined,
): string {
  if (latitude === undefined || longitude === undefined || !Number.isFinite(latitude) || !Number.isFinite(longitude)) {
    return '—'
  }

  return `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`
}

function formatRelativeTime(timestampMs: number, nowMs: number): string {
  const secondsAgo = Math.max(0, Math.floor((nowMs - timestampMs) / 1000))

  if (secondsAgo <= 1) {
    return 'just now'
  }

  return `${secondsAgo}s ago`
}

function getStateClassName(state: ExerciseState): string {
  if (state === 'ACTIVE') {
    return 'state-active'
  }

  if (state === 'PAUSED') {
    return 'state-paused'
  }

  if (state === 'PREPARING') {
    return 'state-preparing'
  }

  if (state === 'ENDED') {
    return 'state-ended'
  }

  return 'state-unknown'
}

function getAccuracyClassName(accuracy: HeartRateAccuracy): string {
  if (accuracy === 'HIGH') {
    return 'hr-accuracy-high'
  }

  if (accuracy === 'MEDIUM') {
    return 'hr-accuracy-medium'
  }

  return 'hr-accuracy-low'
}

function formatSessionId(sessionId: string): string {
  if (sessionId.length <= 8) {
    return sessionId
  }

  return sessionId.slice(-8)
}

function MetricRow({ label, value }: MetricRowProps) {
  return (
    <div className="metric-row">
      <span className="metric-label">{label}</span>
      <span className="metric-value metric-value-inline">{value}</span>
    </div>
  )
}

export function HealthDashboard({ payload }: HealthDashboardProps) {
  const [nowMs, setNowMs] = useState<number>(() => Date.now())

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      setNowMs(Date.now())
    }, 1000)

    return () => {
      window.clearInterval(intervalId)
    }
  }, [])

  const stateLabel = payload?.exerciseState ?? 'UNKNOWN'
  const stateClassName = useMemo(() => getStateClassName(stateLabel), [stateLabel])
  const heartRateAccuracy = payload?.heartRate?.accuracy ?? 'UNKNOWN'
  const accuracyClassName = useMemo(() => getAccuracyClassName(heartRateAccuracy), [heartRateAccuracy])

  if (!payload) {
    return (
      <section className="health-card">
        <h2>Health Dashboard</h2>
        <p>Waiting for watch data...</p>
      </section>
    )
  }

  const heartRateValue = payload.heartRate ? Math.round(payload.heartRate.value).toString() : '—'
  const distanceText = formatDistance(payload.distance)
  const caloriesText = formatCalories(payload.calories)
  const speedText = formatSpeed(payload.speed)
  const paceText = formatPace(payload.pace)
  const stepsText = formatSteps(payload.steps)
  const elevationGainText = formatElevationGain(payload.elevationGain)
  const locationText = formatLocation(payload.location?.latitude, payload.location?.longitude)
  const relativeTime = formatRelativeTime(payload.timestampMs, nowMs)
  const sessionIdText = formatSessionId(payload.sessionId)
  const batteryText = `${payload.batteryPercent}%`

  return (
    <section className="health-card">
      <div className="metric-row">
        <span className="metric-label">Exercise State</span>
        <span className={`exercise-state-badge ${stateClassName}`}>{stateLabel}</span>
      </div>

      <div className="heart-rate-display">
        <div className="hr-value">{heartRateValue}</div>
        <div className="hr-unit">bpm</div>
        <div className={`hr-accuracy ${accuracyClassName}`}>Accuracy: {heartRateAccuracy}</div>
      </div>

      <MetricRow label="Distance" value={distanceText} />
      <MetricRow label="Calories" value={caloriesText} />
      <MetricRow label="Speed" value={speedText} />
      <MetricRow label="Pace" value={paceText} />
      <MetricRow label="Steps" value={stepsText} />
      <MetricRow label="Elevation Gain" value={elevationGainText} />
      <MetricRow label="Location" value={locationText} />
      <MetricRow label="Last Update" value={relativeTime} />
      <MetricRow label="Session" value={sessionIdText} />
      <MetricRow label="Device" value={payload.deviceName} />
      <MetricRow label="Battery" value={batteryText} />
    </section>
  )
}
