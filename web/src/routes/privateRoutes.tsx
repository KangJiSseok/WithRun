import type { RouteObject } from 'react-router-dom';

import { CalendarPage } from '@/pages/private/calendar';
import { FreeRunPage } from '@/pages/private/FreeRunPage';
import { FreeRunSessionPage } from '@/pages/private/FreeRunSessionPage';
import { HomePage } from '@/pages/private/home';
import { NormalRunPage } from '@/pages/private/NormalRun';
import { NormalRunDetailPage } from '@/pages/private/NormalRunDetail';
import { NormalRunSessionPage } from '@/pages/private/NormalRunSession';
import { FullscreenLayout } from '@/shared/layouts/FullscreenLayout';
import { MainLayout } from '@/shared/layouts/MainLayout';
import { GhostRunPage } from '@/pages/private/GhostRun';

export const privateRoutes: RouteObject[] = [
  {
    element: <MainLayout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/home', element: <HomePage /> },
      { path: '/free-run', element: <FreeRunPage /> },
      { path: '/normal-run', element: <NormalRunPage /> },
      { path: '/ghost-run', element: <GhostRunPage /> },
      { path: '/calendar', element: <CalendarPage /> },
    ],
  },
  {
    element: <FullscreenLayout />,
    children: [
      { path: '/free-run-session', element: <FreeRunSessionPage /> },
      { path: '/normal-run/:courseId', element: <NormalRunDetailPage /> },
      {
        path: '/normal-run-session/:courseId',
        element: <NormalRunSessionPage />,
      },
    ],
  },
];
