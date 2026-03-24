import type { RouteObject } from 'react-router-dom';

import { CalendarPage } from '@/pages/private/CalendarPage';
import { FreeRunPage } from '@/pages/private/FreeRunPage';
import { FreeRunSessionPage } from '@/pages/private/FreeRunSessionPage';
import { HomePage } from '@/pages/private/home';
import { NormalRunPage } from '@/pages/private/NormalRun';
import { NormalRunDetailPage } from '@/pages/private/NormalRunDetail';
import { NormalRunSessionPage } from '@/pages/private/NormalRunSession';
import { MainLayout } from '@/shared/layouts/MainLayout';
import { GhostRunPage } from '@/pages/private/GhostRun';

export const privateRoutes: RouteObject[] = [
  {
    element: <MainLayout showTopNav showBottomNav />,
    children: [
      { path: '/home', element: <HomePage /> },

      { path: '/calendar', element: <CalendarPage /> },
    ],
  },
  {
    element: <MainLayout showBottomNav fullScreen />,
    children: [
      { path: '/normal-run', element: <NormalRunPage /> },
      { path: '/normal-run/:courseId', element: <NormalRunDetailPage /> },
      { path: '/free-run', element: <FreeRunPage /> },
      { path: '/ghost-run', element: <GhostRunPage /> },
    ],
  },
  {
    element: <MainLayout fullScreen />,
    children: [
      { path: '/free-run-session', element: <FreeRunSessionPage /> },
      {
        path: '/normal-run-session/:courseId',
        element: <NormalRunSessionPage />,
      },
    ],
  },
];
