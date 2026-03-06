import { useCallback, useEffect, useMemo, useState } from 'react'

import {
  isHealthPayload,
  type BridgeMessage,
  type ConnectionStatus,
  type HealthPayload,
  type WatchAck,
  type WatchCommand,
} from '../types/watch-bridge'

type ReactNativeWebViewBridge = {
  postMessage: (message: string) => void
}

type WrappedEventName = 'WearHealthUpdate' | 'WearAckReceived' | 'WearConnectionStatus'

type WrappedBridgeEvent = {
  event: WrappedEventName
  data: unknown
}

declare global {
  interface Window {
    ReactNativeWebView?: ReactNativeWebViewBridge
  }
}

const WRAPPED_EVENTS: readonly WrappedEventName[] = [
  'WearHealthUpdate',
  'WearAckReceived',
  'WearConnectionStatus',
]

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function parseJson(value: string): unknown {
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

function parseMaybeJson(value: unknown): unknown {
  if (typeof value === 'string') {
    return parseJson(value)
  }

  return value
}

function isWatchAck(value: unknown): value is WatchAck {
  if (!isRecord(value)) {
    return false
  }

  if (value.type !== 'ACK' && value.type !== 'PONG') {
    return false
  }

  if (value.commandType !== undefined && typeof value.commandType !== 'string') {
    return false
  }

  if (value.status !== undefined && value.status !== 'OK' && value.status !== 'ERROR') {
    return false
  }

  if (value.errorMessage !== undefined && value.errorMessage !== null && typeof value.errorMessage !== 'string') {
    return false
  }

  if (value.id !== undefined && typeof value.id !== 'string') {
    return false
  }

  return true
}

function isConnectionStatus(value: unknown): value is ConnectionStatus {
  if (!isRecord(value)) {
    return false
  }

  return (
    typeof value.connected === 'boolean' &&
    typeof value.nodeId === 'string' &&
    typeof value.nodeName === 'string'
  )
}

function isBridgeMessage(value: unknown): value is BridgeMessage {
  if (!isRecord(value) || typeof value.type !== 'string') {
    return false
  }

  if (value.type === 'health_update') {
    return isHealthPayload(value.payload)
  }

  if (value.type === 'ack') {
    return isWatchAck(value.payload)
  }

  if (value.type === 'connection_status') {
    return isConnectionStatus(value.payload)
  }

  return false
}

function isWrappedBridgeEvent(value: unknown): value is WrappedBridgeEvent {
  if (!isRecord(value)) {
    return false
  }

  return (
    typeof value.event === 'string' &&
    WRAPPED_EVENTS.includes(value.event as WrappedEventName) &&
    'data' in value
  )
}

function parseIncomingBridgeMessage(rawData: unknown): BridgeMessage | null {
  const message = parseMaybeJson(rawData)

  if (isBridgeMessage(message)) {
    return message
  }

  if (isHealthPayload(message)) {
    return { type: 'health_update', payload: message }
  }

  if (!isWrappedBridgeEvent(message)) {
    return null
  }

  const wrappedPayload = parseMaybeJson(message.data)

  if (message.event === 'WearHealthUpdate' && isHealthPayload(wrappedPayload)) {
    return { type: 'health_update', payload: wrappedPayload }
  }

  if (message.event === 'WearAckReceived' && isWatchAck(wrappedPayload)) {
    return { type: 'ack', payload: wrappedPayload }
  }

  if (message.event === 'WearConnectionStatus' && isConnectionStatus(wrappedPayload)) {
    return { type: 'connection_status', payload: wrappedPayload }
  }

  return null
}

export function useWatchBridge(): {
  lastPayload: HealthPayload | null
  lastAck: WatchAck | null
  connectionStatus: ConnectionStatus | null
  isConnected: boolean
  sendCommand: (command: WatchCommand) => void
} {
  const [lastPayload, setLastPayload] = useState<HealthPayload | null>(null)
  const [lastAck, setLastAck] = useState<WatchAck | null>(null)
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus | null>(null)

  const hasNativeBridge = useMemo(() => {
    if (typeof window === 'undefined') {
      return false
    }

    return typeof window.ReactNativeWebView?.postMessage === 'function'
  }, [])

  const sendCommand = useCallback(
    (command: WatchCommand): void => {
      const serializedCommand = JSON.stringify(command)

      if (hasNativeBridge && window.ReactNativeWebView) {
        window.ReactNativeWebView.postMessage(serializedCommand)
        return
      }

      console.info('[watch-bridge] ReactNativeWebView unavailable. Command:', command)
    },
    [hasNativeBridge],
  )

  useEffect(() => {
    function handleIncoming(rawData: unknown): void {
      const parsed = parseIncomingBridgeMessage(rawData)

      if (!parsed) {
        return
      }

      if (parsed.type === 'health_update') {
        setLastPayload(parsed.payload)
        return
      }

      if (parsed.type === 'ack') {
        setLastAck(parsed.payload)
        return
      }

      setConnectionStatus(parsed.payload)
    }

    function handleWindowMessage(event: MessageEvent<unknown>): void {
      handleIncoming(event.data)
    }

    const handleDocumentMessage: EventListener = (event) => {
      handleIncoming(Reflect.get(event, 'data'))
    }

    window.addEventListener('message', handleWindowMessage)
    document.addEventListener('message', handleDocumentMessage)

    return () => {
      window.removeEventListener('message', handleWindowMessage)
      document.removeEventListener('message', handleDocumentMessage)
    }
  }, [])

  return {
    lastPayload,
    lastAck,
    connectionStatus,
    isConnected: connectionStatus?.connected ?? false,
    sendCommand,
  }
}
