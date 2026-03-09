import { useEffect, useState } from 'react'

export default function useWatchMessage() {
  const [lastMessage, setLastMessage] = useState<string | null>(null)

  useEffect(() => {
    const handler = (e: CustomEvent<string>) => {
      setLastMessage(e.detail)
    }

    window.addEventListener('watchMessage', handler as EventListener)
    return () => window.removeEventListener('watchMessage', handler as EventListener)
  }, [])

  return { lastMessage }
}
