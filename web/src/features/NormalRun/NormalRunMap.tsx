import { useRef } from 'react';

import type { CoordinatePoint } from '@/apis';
import { useNormalRunMap } from '@/hooks/NormalRun/useNormalRunMap';

type NormalRunMapProps = {
  coordinates: CoordinatePoint[];
  courseAnchor: { latitude: number; longitude: number } | null;
};

export function NormalRunMap({ coordinates, courseAnchor }: NormalRunMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  useNormalRunMap(containerRef, { coordinates, courseAnchor });

  return <div ref={containerRef} className="absolute inset-0 z-base" />;
}
