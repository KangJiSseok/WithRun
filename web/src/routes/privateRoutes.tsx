import type { RouteObject } from 'react-router-dom';

import { CalendarPage } from '@/pages/private/calendar';
import { CompetitionPage } from '@/pages/private/competition';
import { FreePage } from '@/pages/private/FreeRun';
import { FreeRunningPage } from '@/pages/private/FreeRunning';
import { FreeRunResultPage } from '@/pages/private/FreeRunResult';
import { HomePage } from '@/pages/private/home';
import { NormalPage } from '@/pages/private/normal';
import { MobileLayout } from '@/shared/layouts/MobileLayout';

export const privateRoutes: RouteObject[] = [
  {
    element: <MobileLayout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/home', element: <HomePage /> },
      { path: '/free', element: <FreePage /> },
      { path: '/freerun', element: <FreeRunningPage /> },
      { path: '/freerun-result', element: <FreeRunResultPage /> },
      { path: '/normal', element: <NormalPage /> },
      { path: '/competition', element: <CompetitionPage /> },
      { path: '/calendar', element: <CalendarPage /> },
    ],
  },
];
