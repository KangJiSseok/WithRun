import type { RouteObject } from 'react-router-dom';

import { AuthCallbackPage } from '@/pages/public/AuthCallback';
import { EntryPage } from '@/pages/public/Entry';
import ExamplePublicPage from '@/pages/public/Example';
import { LoginPage } from '@/pages/public/Login';
import { OnboardingPage } from '@/pages/public/Onboarding';
import { PublicOnlyRoute, RequireOnboardingRoute } from '@/routes/AuthGuards';
import { MainLayout } from '@/shared/layouts/MainLayout';

export const publicRoutes: RouteObject[] = [
  { path: '/', element: <EntryPage /> },
  { path: '/example', element: <ExamplePublicPage /> },
  {
    // /login 은 별도 public route 를 유지하되, 실제 화면은 온보딩 흐름으로 합쳐서 보낸다.
    element: <PublicOnlyRoute />,
    children: [
      {
        element: <MainLayout fullScreen />,
        children: [{ path: '/login', element: <LoginPage /> }],
      },
    ],
  },
  {
    // OAuth 성공/실패 복구는 success/failure redirect 가 모두 모이는 callback route 하나로 처리한다.
    element: <MainLayout fullScreen />,
    children: [{ path: '/auth/callback', element: <AuthCallbackPage /> }],
  },
  {
    element: <RequireOnboardingRoute />,
    children: [
      {
        element: <MainLayout fullScreen />,
        children: [{ path: '/onboarding', element: <OnboardingPage /> }],
      },
    ],
  },
];
