import { useCallback, useEffect, useRef, useState } from 'react';

import { getNearbyCoursesApi, type CourseItem, type PreferredDistanceM } from '@/apis';
import { useBridgeDataStore } from '@/bridge/stores/useBridgeDataStore';

type Center = { latitude: number; longitude: number };

export function useNormalRunMapCourses(
  center: Center | null,
  radiusM: number,
  preferredDistanceMs: PreferredDistanceM[] = []
) {
  const [courses, setCourses] = useState<CourseItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [hasMore, setHasMore] = useState(false);

  const gpsLocation = useBridgeDataStore((s) => s.gpsLocation);

  // 최신값 ref (fetchPage가 stable reference를 유지하도록)
  const centerRef = useRef(center);
  const radiusRef = useRef(radiusM);
  const preferredDistanceMsRef = useRef(preferredDistanceMs);
  const gpsLocationRef = useRef(gpsLocation);
  useEffect(() => { centerRef.current = center; }, [center]);
  useEffect(() => { radiusRef.current = radiusM; }, [radiusM]);
  useEffect(() => { preferredDistanceMsRef.current = preferredDistanceMs; }, [preferredDistanceMs]);
  useEffect(() => { gpsLocationRef.current = gpsLocation; }, [gpsLocation]);

  const pageRef = useRef(0);

  // fetchPage: ref에서 읽으므로 stable reference (deps: [])
  const fetchPage = useCallback(async (page: number) => {
    const c = centerRef.current;
    if (!c) return;

    const gps = gpsLocationRef.current;

    setIsLoading(true);
    try {
      const { data } = await getNearbyCoursesApi({
        latitude: gps?.latitude ?? c.latitude,
        longitude: gps?.longitude ?? c.longitude,
        targetLatitude: c.latitude,
        targetLongitude: c.longitude,
        radiusM: Math.max(1000, Math.min(5000, radiusRef.current)),
        preferredDistanceMs: preferredDistanceMsRef.current,
        page,
      });

      setCourses((prev) => (page === 0 ? data.items : [...prev, ...data.items]));
      setHasMore(data.hasNext);
      pageRef.current = page + 1;
    } catch {
      if (page === 0) setCourses([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // center/radiusM/preferredDistanceMs 변경 시 목록 초기화 후 첫 페이지 fetch
  useEffect(() => {
    if (!center) return;

    pageRef.current = 0;
    setCourses([]);
    setHasMore(false);
    fetchPage(0);
  }, [center, radiusM, preferredDistanceMs, fetchPage]);

  const loadMore = useCallback(() => {
    if (!hasMore || isLoading) return;
    fetchPage(pageRef.current);
  }, [hasMore, isLoading, fetchPage]);

  return { courses, isLoading, hasMore, loadMore };
}
