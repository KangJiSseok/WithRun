import { createBrowserRouter } from 'react-router-dom';

import { privateRoutes } from './privateRoutes';
import { publicRoutes } from './publicRoutes';

export const router = createBrowserRouter([
  ...publicRoutes,
  ...privateRoutes,
]);
