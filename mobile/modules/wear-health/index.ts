import { NativeModules, NativeEventEmitter, Platform } from 'react-native'

type WearHealthNativeModule = {
  sendCommand: (commandJson: string) => Promise<string>
  getConnectedNodes: () => Promise<string>
  checkConnectionStatus: () => Promise<string>
  addListener: (eventName: string) => void
  removeListeners: (count: number) => void
}

const { WearHealth } = NativeModules as { WearHealth: WearHealthNativeModule | undefined }

if (Platform.OS === 'android' && WearHealth == null) {
  console.warn(
    '[WearHealth] Native module not found. Run expo prebuild and rebuild the app.',
  )
}

export const wearHealthModule: WearHealthNativeModule | null = WearHealth ?? null

export const wearHealthEmitter: NativeEventEmitter | null =
  Platform.OS === 'android' && WearHealth != null
    ? new NativeEventEmitter(WearHealth)
    : null

export const WEAR_EVENTS = {
  HEALTH_UPDATE: 'WearHealthUpdate',
  ACK_RECEIVED: 'WearAckReceived',
  CONNECTION_STATUS: 'WearConnectionStatus',
} as const
