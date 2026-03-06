import { StyleSheet, Text, View } from 'react-native'

export type WatchDebugOverlayProps = {
  isConnected: boolean
  lastPayloadTimestamp: number | null
  lastAckType: string | null
  nodeName: string | null
}

export function WatchDebugOverlay({
  isConnected,
  lastPayloadTimestamp,
  lastAckType,
  nodeName,
}: WatchDebugOverlayProps) {
  const connectionText = isConnected
    ? `Connected: ${nodeName ?? 'Watch'}`
    : 'Disconnected'

  const timestampText =
    lastPayloadTimestamp != null
      ? new Date(lastPayloadTimestamp).toLocaleTimeString()
      : '—'

  return (
    <View style={styles.container}>
      <Text style={[styles.badge, isConnected ? styles.connected : styles.disconnected]}>
        {connectionText}
      </Text>
      <Text style={styles.meta}>Last data: {timestampText}</Text>
      {lastAckType != null && (
        <Text style={styles.meta}>Last ACK: {lastAckType}</Text>
      )}
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 12,
    paddingVertical: 6,
    backgroundColor: '#0f0f1a',
  },
  badge: {
    fontSize: 11,
    fontWeight: '600',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 999,
    overflow: 'hidden',
  },
  connected: {
    backgroundColor: '#22c55e20',
    color: '#22c55e',
  },
  disconnected: {
    backgroundColor: '#ef444420',
    color: '#ef4444',
  },
  meta: {
    fontSize: 11,
    color: '#888',
  },
})
