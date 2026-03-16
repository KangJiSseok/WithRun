import { useEffect, useState } from 'react'
import { bridge } from '@/bridge'
import type { WatchHealthData } from '@bridge'

export default function useWatchMessage() {
  const [watchData, setWatchData] = useState<WatchHealthData | null>(null)

  useEffect(() => {
    const unsubscribe = bridge.addEventListener('watchHealthData', (data) => {
      setWatchData(data)
    })
    return unsubscribe
  }, [])

  return { watchData }
}
