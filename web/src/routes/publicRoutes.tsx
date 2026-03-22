import type { RouteObject } from 'react-router-dom';

import { EntryPage } from '@/pages/public/entry';
import ExamplePublicPage from '@/pages/public/example';
import { OnboardingPage } from '@/pages/public/onboarding';
import RiveTestPage from '@/pages/public/rive-test';
import { FullscreenLayout } from '@/shared/layouts/FullscreenLayout';

export const publicRoutes: RouteObject[] = [
  { path: '/', element: <EntryPage /> },
  { path: '/example', element: <ExamplePublicPage /> },
  { path: '/rive-test', element: <RiveTestPage /> },
  {
    element: <FullscreenLayout />,
    children: [{ path: '/onboarding', element: <OnboardingPage /> }],
  },
];
