import React, { useRef } from 'react';

import type { CoordinatePoint } from '@/apis';
import { useGhostRunMap } from '@/hooks/GhostRun/useGhostRunMap';

type GhostRunMapProps = {
  coordinates: CoordinatePoint[];
  courseAnchor: { latitude: number; longitude: number } | null;
  ghostDurationSec: number;
  isPaused: boolean;
  ghostDistanceMRef?: React.MutableRefObject<number>;
};

export function GhostRunMap({
  coordinates,
  courseAnchor,
  ghostDurationSec,
  isPaused,
  ghostDistanceMRef,
}: GhostRunMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  useGhostRunMap(containerRef, { coordinates, courseAnchor, ghostDurationSec, isPaused, ghostDistanceMRef });

  return <div ref={containerRef} className="absolute inset-0 z-base" />;
}
