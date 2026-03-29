import { useInfiniteQuery } from '@tanstack/react-query';

import { getBookmarkedCoursesApi } from '@/apis/courseBookmark';

export function useBookmarkedCourses() {
  const {
    data,
    isLoading,
    isFetchingNextPage,
    fetchNextPage,
    hasNextPage,
  } = useInfiniteQuery({
    queryKey: ['bookmarkedCourses'],
    queryFn: async ({ pageParam }: { pageParam: string | undefined }) => {
      const res = await getBookmarkedCoursesApi(
        pageParam ? { cursor: pageParam } : {},
      );
      return res.data;
    },
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? lastPage.nextCursor : undefined,
    initialPageParam: undefined as string | undefined,
    staleTime: 1000 * 60 * 5,
  });

  const items = data?.pages.flatMap((p) => p.items) ?? [];

  return {
    items,
    isLoading,
    isLoadingMore: isFetchingNextPage,
    loadMore: fetchNextPage,
    hasMore: hasNextPage ?? false,
  };
}
