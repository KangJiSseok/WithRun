import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';

import { deleteBookmarkApi } from '@/apis/courseBookmark';
import type { BookmarkedCourseItem } from '@/apis/courseBookmark';
import { useBookmarkedCourses } from '@/hooks/BookmarkedCourses';
import { RunningCourseCard } from '@/shared/components/RunningCourseCard';
import { useInfiniteScroll, useLikeToggle } from '@/shared/hooks';
import { useBookmarkStore } from '@/stores/Bookmark/useBookmarkStore';
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '@/shared/components/shadcn/tabs';

const SCROLL_KEY = 'bookmarkScrollY';
sessionStorage.removeItem('scroll:bookmark');

export function BookmarkedCoursesScreen() {
  const queryClient = useQueryClient();
  const { setBookmarked } = useBookmarkStore();
  const { isLiked, getLikeCount, toggle: toggleLike } = useLikeToggle();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = (searchParams.get('tab') ?? 'OFFICIAL') as 'OFFICIAL' | 'COMMUNITY';
  const { items, isLoading, isLoadingMore, loadMore, hasMore } =
    useBookmarkedCourses();
  const [removedIds, setRemovedIds] = useState<Set<number>>(new Set());

  const scrollRef = useRef<HTMLDivElement>(null);
  const didRestoreRef = useRef(false);

  useEffect(() => {
    if (isLoading || didRestoreRef.current) return;
    const saved = sessionStorage.getItem(SCROLL_KEY);
    sessionStorage.removeItem(SCROLL_KEY);
    if (saved && scrollRef.current) {
      scrollRef.current.scrollTop = Number(saved);
    }
    didRestoreRef.current = true;
  }, [isLoading]);

  const visibleItems = items.filter((item) => !removedIds.has(item.courseId));
  const officialItems = visibleItems.filter((item) => item.status === 'OFFICIAL');
  const communityItems = visibleItems.filter((item) => item.status === 'COMMUNITY');

  const handleLoadMore = useCallback(() => {
    void loadMore();
  }, [loadMore]);

  const sentinelRef = useInfiniteScroll(hasMore, isLoadingMore, handleLoadMore);

  function handleCardClick(courseId: number, status: 'OFFICIAL' | 'COMMUNITY') {
    if (scrollRef.current) {
      sessionStorage.setItem(SCROLL_KEY, String(scrollRef.current.scrollTop));
    }
    navigate(status === 'OFFICIAL' ? `/ghost-run/${courseId}` : `/normal-run/${courseId}`);
  }

  function handleToggleSave(courseId: number) {
    setRemovedIds((prev) => new Set([...prev, courseId]));
    setBookmarked(courseId, false);
    deleteBookmarkApi(courseId)
      .then(() => {
        toast.success('저장이 해제되었어요.');
        void queryClient.invalidateQueries({ queryKey: ['bookmarkedCourses'] });
      })
      .catch(() => {
        setRemovedIds((prev) => {
          const next = new Set(prev);
          next.delete(courseId);
          return next;
        });
        toast.error('다시 시도해주세요.');
      });
  }

  function renderList(tabItems: BookmarkedCourseItem[]) {
    if (isLoading) {
      return (
        <div className="flex items-center justify-center py-16">
          <p className="body-r text-[hsl(var(--text-secondary))]">불러오는 중...</p>
        </div>
      );
    }

    if (tabItems.length === 0) {
      return (
        <div className="flex items-center justify-center py-16">
          <p className="body-r text-[hsl(var(--text-secondary))]">
            저장된 코스가 없습니다.
          </p>
        </div>
      );
    }

    return (
      <div className="flex flex-col gap-[var(--space-4)]">
        {tabItems.map((item) => (
          <RunningCourseCard
            key={item.courseId}
            course={{
              ...item,
              isLiked: isLiked(item.courseId, item.isLiked),
              likeCount: getLikeCount(item.courseId, item.likeCount),
            }}
            isSaved={true}
            onClick={() => handleCardClick(item.courseId, item.status)}
            onToggleSave={handleToggleSave}
            onToggleLike={() => toggleLike(item.courseId, item.isLiked, item.likeCount)}
          />
        ))}
      </div>
    );
  }

  return (
    <div
      ref={scrollRef}
      className="-mx-5 -my-5 h-[calc(100%+2.5rem)] overflow-y-auto bg-[hsl(var(--surface-subtle))]"
    >
      <div className="px-5 pb-24 pt-6">
        <div className="mx-auto max-w-[430px]">
          <h1 className="h2-b mb-3 pl-2 text-[hsl(var(--text-primary))]">저장된 코스</h1>

          <Tabs
            value={activeTab}
            onValueChange={(v) => setSearchParams({ tab: v }, { replace: true })}
            className="gap-0"
          >
            <TabsList
              variant="line"
              className="mb-6 h-auto w-full justify-start rounded-none border-b border-[hsl(var(--border-default))] pb-0"
            >
              <TabsTrigger
                value="OFFICIAL"
                className="flex-none data-[state=active]:text-[hsl(var(--primary-1))] after:bg-[hsl(var(--primary-1))]"
              >
                공식 코스
              </TabsTrigger>
              <TabsTrigger
                value="COMMUNITY"
                className="flex-none data-[state=active]:text-[hsl(var(--primary-1))] after:bg-[hsl(var(--primary-1))]"
              >
                커뮤니티 코스
              </TabsTrigger>
            </TabsList>

            <TabsContent value="OFFICIAL">
              {renderList(officialItems)}
            </TabsContent>
            <TabsContent value="COMMUNITY">
              {renderList(communityItems)}
            </TabsContent>
          </Tabs>

          <div ref={sentinelRef} />
          {isLoadingMore && (
            <p className="body-r mt-4 text-center text-[hsl(var(--text-secondary))]">
              불러오는 중...
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
