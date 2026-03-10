import { AppRoutes } from '@/routes';
import { MobileLayout } from './shared/layouts/MobileLayout';

function App() {
  return (
    <MobileLayout>
      <AppRoutes />
    </MobileLayout>
  );
}

export default App;
