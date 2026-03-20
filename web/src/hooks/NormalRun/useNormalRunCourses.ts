import { getNearbyCoursesApi, type CourseItem } from '@/apis';
import { useBridgeDataStore } from '@/bridge/stores/useBridgeDataStore';
import { useCallback, useEffect, useState } from 'react';

// 코스 탐색 반경(m) 초기값
const DEFAULT_RADIUS_M = 1000;
// 희망 코스 거리(m) 기본값
export const DEFAULT_TARGET_DISTANCE_M = 1000;

export const useNormalRunCourses = (targetDistanceM = DEFAULT_TARGET_DISTANCE_M) => {
  const gpsLocation = useBridgeDataStore((s) => s.gpsLocation);
  const [normalRunCourses, setNormalRunCourses] = useState<CourseItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const fetchAllCourses = useCallback(
    async (
      location: { latitude: number; longitude: number },
      signal: { cancelled: boolean }
    ) => {
      setIsLoading(true);
      setNormalRunCourses([]);

      const allItems: CourseItem[] = [];
      let radiusM = DEFAULT_RADIUS_M;
      let cursor: string | undefined = undefined;

      try {
        while (!signal.cancelled) {
          const { data } = await getNearbyCoursesApi({
            latitude: location.latitude,
            longitude: location.longitude,
            radiusM,
            targetDistanceM,
            cursor,
          });

          if (signal.cancelled) break;

          allItems.push(...data.items);

          if (data.hasMore) {
            // 현재 반경 내 다음 페이지 조회
            cursor = data.nextCursor ?? undefined;
          } else {
            if (data.suggestedNextRadiusM === null || data.nextCursor === null) {
              // 더 이상 코스 없음 (최대 반경 도달 or nextCursor 없음)
              break;
            }
            // 탐색 반경 확장 후 계속
            radiusM = data.suggestedNextRadiusM;
            cursor = data.nextCursor; // 문맥 유지
          }
        }

        if (!signal.cancelled) {
          setNormalRunCourses(allItems);
        }
      } finally {
        if (!signal.cancelled) {
          setIsLoading(false);
        }
      }
    },
    [targetDistanceM]
  );

  useEffect(() => {
    const signal = { cancelled: false };

    const init = async () => {
      try {
        // 브릿지 GPS 데이터가 없으면 브라우저 위치 API 사용
        const location =
          gpsLocation ??
          (await new Promise<{ latitude: number; longitude: number }>(
            (resolve, reject) =>
              navigator.geolocation.getCurrentPosition(
                ({ coords }) =>
                  resolve({
                    latitude: coords.latitude,
                    longitude: coords.longitude,
                  }),
                reject
              )
          ));

        if (!signal.cancelled) {
          fetchAllCourses(location, signal);
        }
      } catch {
        if (!signal.cancelled) {
          setIsLoading(false);
        }
      }
    };

    init();

    return () => {
      signal.cancelled = true;
    };
  }, [fetchAllCourses, gpsLocation]);

  return { normalRunCourses, isLoading };
};
