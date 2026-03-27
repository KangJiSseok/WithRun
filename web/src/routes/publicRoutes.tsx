import { type RouteObject } from 'react-router-dom';

import { AuthCallbackPage } from '@/pages/public/AuthCallback';
import { LoginPage } from '@/pages/public/Login';
import { OnboardingPage } from '@/pages/public/Onboarding';
import { PublicOnlyRoute } from '@/routes/AuthGuards';
import { MainLayout } from '@/shared/layouts/MainLayout';
import ErrorPage from './ErrorPage';

export const publicRoutes: RouteObject[] = [
  {
    // /login 은 별도 public route 를 유지하되, 실제 화면은 온보딩 흐름으로 합쳐서 보낸다.
    element: <PublicOnlyRoute />,
    children: [
      {
        element: <MainLayout fullScreen />,
        children: [
          { path: '/login', element: <LoginPage /> },
          { path: '/auth/callback', element: <AuthCallbackPage /> },
          { path: '/onboarding', element: <OnboardingPage /> },
          { path: '*', element: <ErrorPage /> },
        ],
      },
    ],
  },
];
