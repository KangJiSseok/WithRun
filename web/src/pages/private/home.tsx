import { bridge } from '@/bridge';
import { useEffect } from 'react';

export function HomePage() {
  useEffect(() => {
    const unsubscribe = bridge.addEventListener(
      'watchMessage',
      ({ message }) => {
        alert(message);
      }
    );
    return unsubscribe;
  }, []);

  return <div>홈 페이지</div>;
}
