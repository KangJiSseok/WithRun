import { useRef } from 'react';

import type { GpsSample } from '@/apis/runningSessions';
import { useNormalRunResultMap } from '@/hooks/NormalRun/useNormalRunResultMap';

type NormalRunResultMapProps = {
  gpsSamples: GpsSample[];
};

export function NormalRunResultMap({ gpsSamples }: NormalRunResultMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  useNormalRunResultMap(containerRef, gpsSamples);

  return <div ref={containerRef} className="w-full h-full" />;
}
