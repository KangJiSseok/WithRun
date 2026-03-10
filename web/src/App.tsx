import { AppRoutes } from '@/routes';
import RootLayout from '@/shared/layouts/RootLayout';
import { MobileLayout } from './shared/layouts/MobileLayout';

function App() {
  return (
    <RootLayout>
      <MobileLayout>
        <AppRoutes />
      </MobileLayout>
    </RootLayout>
  );
}

export default App;
