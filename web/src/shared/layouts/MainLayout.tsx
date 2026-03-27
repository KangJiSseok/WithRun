import { Outlet } from 'react-router-dom';

import { BottomNavigation } from '../components';
import TopNavigation from '../components/TopNavigation/TopNavigation';
import { cn } from '../utils';

export function MainLayout({
  showTopNav = false,
  showBottomNav = false,
  fullScreen = false,
}: {
  showTopNav?: boolean;
  showBottomNav?: boolean;
  fullScreen?: boolean;
}) {
  return (
    <div
      className={
        'relative mx-auto flex h-dvh min-w-[375px] max-w-[430px] items-center justify-center bg-gray-100'
      }
    >
      <div className="flex h-dvh w-full flex-col overflow-hidden bg-white shadow-xl">
        <header className="shrink-0 bg-[hsl(var(--surface-subtle))] px-5 pt-6">
          {showTopNav && <TopNavigation />}
        </header>

        <main
          className={cn(
            'flex-1 min-h-0 overflow-y-overlay p-5',
            fullScreen && 'p-0'
          )}
        >
          <Outlet />
        </main>

        {showBottomNav && <BottomNavigation />}
      </div>
    </div>
  );
}
