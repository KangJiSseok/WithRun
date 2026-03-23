import { useEffect } from 'react';

import type { GpsSample } from '@/apis/runningSessions';
import { createMarkerContent } from '@/shared/utils';

const POLYLINE_STROKE_WEIGHT = 5;
const POLYLINE_STROKE_COLOR = '#4F80FF';
const POLYLINE_STROKE_OPACITY = 0.9;

export function useNormalRunResultMap(
  containerRef: React.RefObject<HTMLDivElement | null>,
  gpsSamples: GpsSample[],
): void {
  useEffect(() => {
    if (!containerRef.current) return;

    const container = containerRef.current;

    kakao.maps.load(() => {
      const first = gpsSamples[0];
      const center = first
        ? new kakao.maps.LatLng(first.latitude, first.longitude)
        : new kakao.maps.LatLng(37.5665, 126.978);

      const map = new kakao.maps.Map(container, { center, level: 5 });

      if (gpsSamples.length >= 2) {
        const path = gpsSamples.map((s) => new kakao.maps.LatLng(s.latitude, s.longitude));

        new kakao.maps.Polyline({
          map,
          path,
          strokeWeight: POLYLINE_STROKE_WEIGHT,
          strokeColor: POLYLINE_STROKE_COLOR,
          strokeOpacity: POLYLINE_STROKE_OPACITY,
          strokeStyle: 'solid',
        });

        const bounds = new kakao.maps.LatLngBounds();
        path.forEach((latLng) => bounds.extend(latLng));
        map.setBounds(bounds);

        new kakao.maps.CustomOverlay({
          map,
          position: path[0],
          content: createMarkerContent('출발', '#22C55E'),
          xAnchor: 0.5,
          yAnchor: 0.5,
        });

        new kakao.maps.CustomOverlay({
          map,
          position: path[path.length - 1],
          content: createMarkerContent('도착', '#EF4444'),
          xAnchor: 0.5,
          yAnchor: 0.5,
        });
      }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [containerRef]);
}
