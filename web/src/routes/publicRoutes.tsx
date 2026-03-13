import type { RouteObject } from 'react-router-dom';

import ExamplePublicPage from '@/pages/public/example';

export const publicRoutes: RouteObject[] = [
  { path: '/example', element: <ExamplePublicPage /> },
];
