import { useRef } from 'react';

import type { CoordinatePoint } from '@/apis';
import { useNormalRunMap } from '@/hooks/NormalRun/useNormalRunMap';

type NormalRunMapProps = {
  coordinates: CoordinatePoint[];
};

export function NormalRunMap({ coordinates }: NormalRunMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  useNormalRunMap(containerRef, { coordinates });

  return <div ref={containerRef} className="absolute inset-0 z-base" />;
}
