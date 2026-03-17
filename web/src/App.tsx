import { RouterProvider } from 'react-router-dom';
import { router } from './routes/routes';
import { useBridgeSync } from '@/bridge';

function App() {
  useBridgeSync();
  return <RouterProvider router={router} />;
}

export default App;
