import type { RouteObject } from 'react-router-dom';

import { EntryPage } from '@/pages/public/entry';
import ExamplePublicPage from '@/pages/public/example';
import { OnboardingPage } from '@/pages/public/onboarding';
import { MainLayout } from '@/shared/layouts/MainLayout';

export const publicRoutes: RouteObject[] = [
  { path: '/', element: <EntryPage /> },
  { path: '/example', element: <ExamplePublicPage /> },
  {
    // 온보딩은 헤더/하단 탭 없이 보여줘야 해서 전체 화면 레이아웃을 사용한다.
    element: <MainLayout fullScreen />,
    children: [{ path: '/onboarding', element: <OnboardingPage /> }],
  },
];
