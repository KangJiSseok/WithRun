import { useFreeRunMap } from '@/features/FreeRun/hooks/useFreeRunMap';

export function FreeRunMap() {
  const { containerRef } = useFreeRunMap();

  return <div ref={containerRef} className="absolute inset-0" />;
}
