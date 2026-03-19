import type { RouteObject } from 'react-router-dom';

import { CalendarPage } from '@/pages/private/calendar';
import { CompetitionPage } from '@/pages/private/competition';
import { FreeRunPage } from '@/pages/private/FreeRunPage';
import { FreeRunSessionPage } from '@/pages/private/FreeRunSessionPage';
import { HomePage } from '@/pages/private/home';
import { NormalRunPage } from '@/pages/private/NormalRun';
import { NormalRunDetailPage } from '@/pages/private/NormalRunDetail';
import { NormalRunningPage } from '@/pages/private/NormalRunning';
import { FullscreenLayout } from '@/shared/layouts/FullscreenLayout';
import { MainLayout } from '@/shared/layouts/MainLayout';

export const privateRoutes: RouteObject[] = [
  {
    element: <MainLayout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/home', element: <HomePage /> },
      { path: '/free-run', element: <FreeRunPage /> },
      { path: '/normal', element: <NormalRunPage /> },
      { path: '/competition', element: <CompetitionPage /> },
      { path: '/calendar', element: <CalendarPage /> },
    ],
  },
  {
    element: <FullscreenLayout />,
    children: [
      { path: '/free-run-session', element: <FreeRunSessionPage /> },
      { path: '/normal/:courseId', element: <NormalRunDetailPage /> },
      { path: '/normal/:courseId/running', element: <NormalRunningPage /> },
    ],
  },
];
