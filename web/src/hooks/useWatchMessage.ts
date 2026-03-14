import { useEffect, useState } from 'react'
import { bridge } from '@/bridge'

export default function useWatchMessage() {
  const [lastMessage, setLastMessage] = useState<string | null>(null)

  useEffect(() => {
    const unsubscribe = bridge.addEventListener('watchMessage', ({ message }) => {
      setLastMessage(message)
    })
    return unsubscribe
  }, [])

  return { lastMessage }
}
