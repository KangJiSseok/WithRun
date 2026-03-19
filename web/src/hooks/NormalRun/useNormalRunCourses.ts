import {
  getNearbyCoursesApi,
  type CourseItem,
  type GetNearbyCoursesParams,
} from '@/apis';
import { useBridgeDataStore } from '@/bridge/stores/useBridgeDataStore';
import { useCallback, useEffect, useState } from 'react';

// 코스 탐색 반경(m)
const DEFAULT_RADIUS_M = 3000;
// 희망 코스 거리(m)
const DEFAULT_TARGET_DISTANCE_M = 3000;

export const useNormalRunCourses = () => {
  const gpsLocation = useBridgeDataStore((s) => s.gpsLocation);
  const [normalRunCourses, setNormalRunCourses] = useState<CourseItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const fetchNearByCourses = useCallback(
    async (params: GetNearbyCoursesParams) => {
      setIsLoading(true);
      try {
        const response = await getNearbyCoursesApi(params);
        setNormalRunCourses(response.data.items);
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    const init = async () => {
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

      // 코스 리스트 초기화
      fetchNearByCourses({
        latitude: location.latitude,
        longitude: location.longitude,
        radiusM: DEFAULT_RADIUS_M,
        targetDistanceM: DEFAULT_TARGET_DISTANCE_M,
      });
    };

    init();
  }, [fetchNearByCourses, gpsLocation]);

  return { normalRunCourses, isLoading, fetchNearByCourses };
};
