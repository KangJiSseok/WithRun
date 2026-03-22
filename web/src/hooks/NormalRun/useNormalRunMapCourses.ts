import { useCallback, useEffect, useRef, useState } from 'react';

import { getNearbyCoursesApi, type CourseItem } from '@/apis';

// 희망 코스 거리(m) 기본값 (useNormalRunCourses와 동일)
export const DEFAULT_TARGET_DISTANCE_M = 3000;

type Center = { latitude: number; longitude: number };

export function useNormalRunMapCourses(
  center: Center | null,
  radiusM: number,
  targetDistanceM = DEFAULT_TARGET_DISTANCE_M
) {
  const [courses, setCourses] = useState<CourseItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [hasMore, setHasMore] = useState(false);

  // 최신값 ref (fetchPage가 stable reference를 유지하도록)
  const centerRef = useRef(center);
  const radiusRef = useRef(radiusM);
  const targetDistanceMRef = useRef(targetDistanceM);
  useEffect(() => {
    centerRef.current = center;
  }, [center]);
  useEffect(() => {
    radiusRef.current = radiusM;
  }, [radiusM]);
  useEffect(() => {
    targetDistanceMRef.current = targetDistanceM;
  }, [targetDistanceM]);

  const cursorRef = useRef<string | null>(null);

  // fetchPage: ref에서 읽으므로 stable reference (deps: [])
  const fetchPage = useCallback(async (cursor: string | null) => {
    const c = centerRef.current;
    if (!c) return;

    setIsLoading(true);
    try {
      const { data } = await getNearbyCoursesApi({
        latitude: c.latitude,
        longitude: c.longitude,
        radiusM: radiusRef.current,
        targetDistanceM: targetDistanceMRef.current,
        ...(cursor ? { cursor } : {}),
      });

      setCourses((prev) => (cursor ? [...prev, ...data.items] : data.items));
      setHasMore(data.hasMore);
      cursorRef.current = data.nextCursor;
    } catch {
      if (!cursor) setCourses([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // center/radiusM/targetDistanceM 변경 시 목록 초기화 후 첫 페이지 fetch
  useEffect(() => {
    if (!center) return;

    cursorRef.current = null;
    setCourses([]);
    setHasMore(false);
    fetchPage(null);
  }, [center, radiusM, targetDistanceM, fetchPage]);

  const loadMore = useCallback(() => {
    if (!hasMore || isLoading || !cursorRef.current) return;
    fetchPage(cursorRef.current);
  }, [hasMore, isLoading, fetchPage]);

  return { courses, isLoading, hasMore, loadMore };
}
