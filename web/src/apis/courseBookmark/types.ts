import type { RouteType } from '@/shared/types/api.type';

type MetaOption = {
  data: string;
  label: string;
};

export type BookmarkedCourseItem = {
  bookmarkId: number;
  bookmarkedAt: string;
  courseId: number;
  title: string;
  status: 'OFFICIAL' | 'COMMUNITY';
  routeType: RouteType;
  distanceM: number;
  elevationGainM: number;
  difficulty: MetaOption | null;
  courseTypes: MetaOption[];
  snapshotImageUrl: string | null;
  likeCount: number;
  isLiked: boolean;
  isBookmarked: boolean;
};

export type GetBookmarkedCoursesParams = {
  size?: number;
  cursor?: string;
};

export type GetBookmarkedCoursesData = {
  items: BookmarkedCourseItem[];
  hasMore: boolean;
  nextCursor: string | null;
};

export type AddBookmarkData = {
  courseId: number;
  isBookmarked: boolean;
  createdAt: string;
};
