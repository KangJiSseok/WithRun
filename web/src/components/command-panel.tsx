import { useCallback, useState, type ChangeEvent } from 'react'

import type { WatchAck, WatchCommand } from '../types/watch-bridge'

type CommandPanelProps = {
  onCommand: (cmd: WatchCommand) => void
  lastAck: WatchAck | null
  isConnected: boolean
}

const INTERVAL_OPTIONS = [500, 1000, 2000, 5000] as const

export function CommandPanel({ onCommand, lastAck, isConnected }: CommandPanelProps) {
  const [intervalMs, setIntervalMs] = useState<number>(1000)

  const handleStartExercise = useCallback((): void => {
    onCommand({ type: 'START_EXERCISE' })
  }, [onCommand])

  const handleStopExercise = useCallback((): void => {
    onCommand({ type: 'STOP_EXERCISE' })
  }, [onCommand])

  const handlePauseExercise = useCallback((): void => {
    onCommand({ type: 'PAUSE_EXERCISE' })
  }, [onCommand])

  const handleResumeExercise = useCallback((): void => {
    onCommand({ type: 'RESUME_EXERCISE' })
  }, [onCommand])

  const handlePing = useCallback((): void => {
    onCommand({ type: 'PING', id: crypto.randomUUID() })
  }, [onCommand])

  const handleIntervalChange = useCallback((event: ChangeEvent<HTMLSelectElement>): void => {
    setIntervalMs(Number(event.target.value))
  }, [])

  const handleApplyInterval = useCallback((): void => {
    onCommand({ type: 'SET_INTERVAL', intervalMs })
  }, [intervalMs, onCommand])

  const ackType = lastAck?.type ?? '—'
  const ackCommandType = lastAck?.commandType ?? '—'
  const ackStatus = lastAck?.status ?? '—'
  const connectionClassName = isConnected ? 'connected' : 'disconnected'

  return (
    <section className="command-panel">
      <div className="metric-row">
        <span className="metric-label">Connection</span>
        <span className={`connection-badge ${connectionClassName}`}>{isConnected ? 'Online' : 'Offline'}</span>
      </div>

      <div className="command-grid">
        <button
          type="button"
          className="cmd-btn cmd-start"
          onClick={handleStartExercise}
          disabled={!isConnected}
        >
          START
        </button>
        <button
          type="button"
          className="cmd-btn cmd-stop"
          onClick={handleStopExercise}
          disabled={!isConnected}
        >
          STOP
        </button>
        <button
          type="button"
          className="cmd-btn cmd-pause"
          onClick={handlePauseExercise}
          disabled={!isConnected}
        >
          PAUSE
        </button>
        <button
          type="button"
          className="cmd-btn cmd-resume"
          onClick={handleResumeExercise}
          disabled={!isConnected}
        >
          RESUME
        </button>
      </div>

      <button
        type="button"
        className="cmd-btn cmd-ping"
        onClick={handlePing}
        disabled={!isConnected}
      >
        PING
      </button>

      <div className="interval-row">
        <label htmlFor="interval-select" className="metric-label">
          Interval
        </label>
        <select id="interval-select" value={intervalMs} onChange={handleIntervalChange} disabled={!isConnected}>
          {INTERVAL_OPTIONS.map((option) => (
            <option key={option} value={option}>
              {option} ms
            </option>
          ))}
        </select>
        <button
          type="button"
          className="cmd-btn cmd-apply"
          onClick={handleApplyInterval}
          disabled={!isConnected}
        >
          Apply
        </button>
      </div>

      <div className="ack-display">
        <div>ACK type: {ackType}</div>
        <div>Command: {ackCommandType}</div>
        <div>Status: {ackStatus}</div>
      </div>
    </section>
  )
}
