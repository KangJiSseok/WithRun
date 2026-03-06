import { useState, useEffect, useCallback } from 'react'

import { wearHealthEmitter, wearHealthModule, WEAR_EVENTS } from '@/modules/wear-health'
import type { HealthPayload, WatchAck, ConnectionStatus, WatchCommand } from '@/types/watch-bridge'

type WatchNativeEventsResult = {
  lastPayload: HealthPayload | null
  lastAck: WatchAck | null
  connectionStatus: ConnectionStatus | null
  isConnected: boolean
  sendCommand: (command: WatchCommand) => Promise<void>
}

function safeParseJson(data: unknown): unknown {
  if (typeof data !== 'string') return null
  try {
    return JSON.parse(data)
  } catch {
    return null
  }
}

function isHealthPayload(obj: unknown): obj is HealthPayload {
  return (
    typeof obj === 'object' &&
    obj !== null &&
    (obj as Record<string, unknown>)['type'] === 'HEALTH_UPDATE'
  )
}

function isWatchAck(obj: unknown): obj is WatchAck {
  if (typeof obj !== 'object' || obj === null) return false
  const type = (obj as Record<string, unknown>)['type']
  return type === 'ACK' || type === 'PONG'
}

function isConnectionStatus(obj: unknown): obj is ConnectionStatus {
  return (
    typeof obj === 'object' &&
    obj !== null &&
    typeof (obj as Record<string, unknown>)['connected'] === 'boolean'
  )
}

export function useWatchNativeEvents(): WatchNativeEventsResult {
  const [lastPayload, setLastPayload] = useState<HealthPayload | null>(null)
  const [lastAck, setLastAck] = useState<WatchAck | null>(null)
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus | null>(null)

  useEffect(() => {
    if (wearHealthEmitter == null) return

    const healthSub = wearHealthEmitter.addListener(
      WEAR_EVENTS.HEALTH_UPDATE,
      (data: unknown) => {
        const parsed = safeParseJson(data)
        if (isHealthPayload(parsed)) {
          setLastPayload(parsed)
        }
      },
    )

    const ackSub = wearHealthEmitter.addListener(
      WEAR_EVENTS.ACK_RECEIVED,
      (data: unknown) => {
        const parsed = safeParseJson(data)
        if (isWatchAck(parsed)) {
          setLastAck(parsed)
        }
      },
    )

    const connectionSub = wearHealthEmitter.addListener(
      WEAR_EVENTS.CONNECTION_STATUS,
      (data: unknown) => {
        const parsed = safeParseJson(data)
        if (isConnectionStatus(parsed)) {
          setConnectionStatus(parsed)
        }
      },
    )

    // Check initial connection status on mount
    void (async () => {
      if (wearHealthModule == null) return
      try {
        const statusJson = await wearHealthModule.checkConnectionStatus()
        const parsed = safeParseJson(statusJson)
        if (isConnectionStatus(parsed)) {
          setConnectionStatus(parsed)
        }
      } catch {
        // Ignore errors during initial check
      }
    })()

    return () => {
      healthSub.remove()
      ackSub.remove()
      connectionSub.remove()
    }
  }, [])

  const sendCommand = useCallback(async (command: WatchCommand): Promise<void> => {
    if (wearHealthModule == null) {
      console.warn('[WearHealth] Native module unavailable — cannot send command')
      return
    }
    await wearHealthModule.sendCommand(JSON.stringify(command))
  }, [])

  return {
    lastPayload,
    lastAck,
    connectionStatus,
    isConnected: connectionStatus?.connected === true,
    sendCommand,
  }
}
