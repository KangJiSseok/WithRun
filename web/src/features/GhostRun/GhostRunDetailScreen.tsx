import { ArrowLeft, MapPin, Trophy } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import type { GhostCourseDetail } from '@/apis';
import { createRunningSession } from '@/apis/runningSessions';
import { bridge, useBridgeDataStore } from '@/bridge';
import { BOTTOM_NAV_HEIGHT } from '@/shared/components';
import { Badge } from '@/shared/components/shadcn/badge';
import { Button } from '@/shared/components/shadcn/button';
import { useRunDetailMap } from '@/shared/hooks/useRunDetailMap';
import { formatTime } from '@/shared/utils';
import { useGhostRunSessionStore } from '@/stores/GhostRun';
import { useGhostRunLeaderboard } from '@/hooks/GhostRun/useGhostRunLeaderboard';

type GhostTarget = { runningSessionId: number; isMine: boolean };

type GhostRunDetailScreenProps = {
  course: GhostCourseDetail;
};

export function GhostRunDetailScreen({ course }: GhostRunDetailScreenProps) {
  const navigate = useNavigate();
  const { courseId } = useParams();
  const containerRef = useRef<HTMLDivElement>(null);
  const sentinelRef = useRef<HTMLDivElement>(null);
  const [isStarting, setIsStarting] = useState(false);
  const [selectedTarget, setSelectedTarget] = useState<GhostTarget | null>(
    null
  );

  const gpsLocation = useBridgeDataStore((s) => s.gpsLocation);
  const setSession = useGhostRunSessionStore((s) => s.setSession);

  const { isNearStart } = useRunDetailMap(containerRef, {
    coordinates: course.coordinates,
    startLatitude: course.startLatitude,
    startLongitude: course.startLongitude,
    endLatitude: course.endLatitude,
    endLongitude: course.endLongitude,
  });

  const {
    items,
    hasMore,
    isLoading: leaderboardLoading,
    loadMore,
  } = useGhostRunLeaderboard(course.courseId);

  // IntersectionObserver로 무한스크롤 센티널 연결
  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore && !leaderboardLoading) {
          loadMore();
        }
      },
      { threshold: 0.1 }
    );
    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [hasMore, leaderboardLoading, loadMore]);

  const handleStartRunning = useCallback(async () => {
    if (!gpsLocation || !courseId) return;

    setIsStarting(true);
    try {
      const res = await createRunningSession({
        mode: 'GHOST',
        courseId: Number(courseId),
        ghostTargetRunningSessionId: selectedTarget?.runningSessionId,
        startLatitude: gpsLocation.latitude,
        startLongitude: gpsLocation.longitude,
      });
      setSession(res.data);
      navigate(`/ghost-run-session/${courseId}`);
    } finally {
      setIsStarting(false);
    }
  }, [gpsLocation, courseId, selectedTarget, setSession, navigate]);

  const handleSelectMyRecord = () => {
    if (!course.myRecord) return;
    setSelectedTarget((prev) =>
      prev?.runningSessionId === course.myRecord!.runningSessionId
        ? null
        : { runningSessionId: course.myRecord!.runningSessionId, isMine: true }
    );
  };

  const handleSelectLeaderboard = (runningSessionId: number) => {
    setSelectedTarget((prev) =>
      prev?.runningSessionId === runningSessionId
        ? null
        : { runningSessionId, isMine: false }
    );
  };

  const hasNoRecords = !course.myRecord && items.length === 0;
  const isDisabled = !isNearStart || (!selectedTarget && !hasNoRecords) || isStarting;

  return (
    <div className="relative h-screen w-full">
      {/* 지도 */}
      <div ref={containerRef} className="h-full w-full" />

      {/* 상단 뒤로가기 버튼 */}
      <div className="absolute left-[var(--space-4)] top-[var(--space-4)] z-raised">
        <Button
          type="button"
          variant="outline"
          size="icon"
          onClick={() => navigate(-1)}
          className="h-12 w-12 rounded-full border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))]"
        >
          <ArrowLeft size={20} />
        </Button>
      </div>

      {/* 하단 코스 정보 패널 */}
      <div
        style={{ paddingBottom: BOTTOM_NAV_HEIGHT + 20 }}
        className="absolute bottom-0 left-0 right-0 z-raised flex flex-col gap-[var(--space-4)] rounded-t-2xl bg-[hsl(var(--surface-default))] p-[var(--space-5)] shadow-[var(--shadow-lg)]"
      >
        <h2 className="h2-b text-[hsl(var(--text-primary))]">{course.title}</h2>

        {/* 코스 정보 배지 */}
        <div className="flex flex-wrap gap-[var(--space-2)]">
          <Badge
            variant="outline"
            className="rounded-full border-[hsl(var(--border-default))] bg-transparent px-[var(--space-3)] py-[var(--space-1)]"
          >
            {(course.distanceM / 1000).toFixed(1)}km
          </Badge>
          <Badge
            variant="outline"
            className="rounded-full border-[hsl(var(--border-default))] bg-transparent px-[var(--space-3)] py-[var(--space-1)]"
          >
            고도 {course.elevationGainM}m
          </Badge>
          {course.difficulty != null && (
            <Badge
              variant="outline"
              className="rounded-full border-[hsl(var(--border-default))] bg-transparent px-[var(--space-3)] py-[var(--space-1)]"
            >
              {course.difficulty.label}
            </Badge>
          )}
        </div>

        {/* 리더보드 섹션 */}
        <div className="flex flex-col gap-[var(--space-2)]">
          <div className="flex items-center gap-[var(--space-2)]">
            <Trophy size={16} className="text-[hsl(var(--primary-1))]" />
            <span className="body-l-b text-[hsl(var(--text-primary))]">
              리더보드
            </span>
          </div>

          {!selectedTarget && (
            <p className="caption-r text-[hsl(var(--text-secondary))]">
              대결할 상대를 선택하세요
            </p>
          )}

          {/* 나의 최고 기록 */}
          {course.myRecord && (
            <button
              onClick={handleSelectMyRecord}
              className={`flex items-center justify-between rounded-xl border p-[var(--space-3)] text-left transition-colors ${
                selectedTarget?.runningSessionId ===
                course.myRecord.runningSessionId
                  ? 'border-[hsl(var(--primary-1))] bg-[hsl(var(--primary-1)/0.08)]'
                  : 'border-[hsl(var(--border-default))] bg-[hsl(var(--surface-subtle))]'
              }`}
            >
              <div className="flex flex-col gap-0.5">
                <span className="caption-r text-[hsl(var(--primary-1))]">
                  나의 최고 기록
                </span>
                <span className="body-l-b text-[hsl(var(--text-primary))]">
                  {formatTime(course.myRecord.durationSec)}
                </span>
              </div>
              <div className="flex flex-col items-end gap-0.5">
                <span className="caption-r text-[hsl(var(--text-secondary))]">
                  {course.myRecord.rank}위
                </span>
                <span className="body-m text-[hsl(var(--primary-1))]">
                  {course.myRecord.point.toLocaleString()}P
                </span>
              </div>
            </button>
          )}

          {/* 리더보드 목록 */}
          <div className="max-h-[200px] overflow-y-auto flex flex-col gap-[var(--space-1)]">
            {items.map((item) => (
              <button
                key={item.leaderboardId}
                onClick={() => handleSelectLeaderboard(item.runningSessionId)}
                className={`flex items-center justify-between rounded-lg border px-[var(--space-3)] py-[var(--space-2)] text-left transition-colors ${
                  selectedTarget?.runningSessionId === item.runningSessionId
                    ? 'border-[hsl(var(--primary-1))] bg-[hsl(var(--primary-1)/0.08)]'
                    : 'border-[hsl(var(--border-default))] bg-transparent'
                }`}
              >
                <div className="flex items-center gap-[var(--space-2)]">
                  <span className="caption-r w-5 text-center text-[hsl(var(--text-secondary))]">
                    {item.rank}
                  </span>
                  <span className="body-m text-[hsl(var(--text-primary))]">
                    {item.nickname}
                  </span>
                </div>
                <span className="caption-r text-[hsl(var(--primary-1))]">
                  {item.point.toLocaleString()}P
                </span>
              </button>
            ))}

            {/* 무한스크롤 센티널 */}
            <div ref={sentinelRef} className="h-1" />

            {leaderboardLoading && (
              <p className="caption-r py-2 text-center text-[hsl(var(--text-secondary))]">
                불러오는 중...
              </p>
            )}
          </div>
        </div>

        {/* 출발 지점 도착 안내 박스 */}
        <div className="flex items-start gap-[var(--space-3)] rounded-[var(--radius-lg)] border border-[hsl(var(--border-default))] bg-[hsl(var(--primary-1)/0.05)] p-[var(--space-4)]">
          <MapPin
            size={20}
            className="mt-0.5 shrink-0 text-[hsl(var(--primary-1))]"
          />
          <div className="flex flex-col gap-[var(--space-1)]">
            <p className="body-l-b text-[hsl(var(--text-primary))]">
              출발 지점 도착
            </p>
            <p className="body-r text-[hsl(var(--text-secondary))]">
              출발 지점에 도착하면 대결을 시작할 수 있습니다.
            </p>
          </div>
        </div>

        {/* 대결 시작 버튼 */}
        {bridge.isWebViewBridgeAvailable ? (
          <Button
            type="button"
            disabled={isDisabled}
            onClick={handleStartRunning}
            className="h-14 w-full rounded-xl body-l-b"
          >
            {isStarting ? '시작 중...' : '대결 시작'}
          </Button>
        ) : (
          <p className="body-m py-(--space-3) text-center text-[hsl(var(--text-secondary))]">
            모바일 앱에서 이용할 수 있는 서비스입니다.
          </p>
        )}
      </div>
    </div>
  );
}
