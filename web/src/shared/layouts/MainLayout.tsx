import { Bookmark, User } from 'lucide-react';
import { Outlet } from 'react-router-dom';

import { BottomNavigation } from '../components';
import { Button } from '../components/shadcn/button';

export function MainLayout() {
  return (
    <div className="relative mx-auto flex h-dvh min-w-[375px] max-w-[430px] items-center justify-center bg-gray-100">
      <div className="flex h-dvh w-full flex-col overflow-hidden bg-white shadow-xl">
        <header className="flex h-16 shrink-0 items-center justify-between px-[var(--space-4)]">
          <Button
            type="button"
            variant="outline"
            size="icon"
            className="h-12 w-12 rounded-full border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))]"
          >
            <User size={20} />
          </Button>

          <Button
            type="button"
            variant="outline"
            size="icon"
            className="h-12 w-12 rounded-full border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))]"
          >
            <Bookmark size={20} />
          </Button>
        </header>

        <main className="flex-1 min-h-0 overflow-y-overlay px-[var(--space-4)] py-[var(--space-6)]">
          <Outlet />
        </main>

        <footer className="shrink-0 w-full h-20 relative z-overlay">
          <div className="absolute left-0 bottom-0 w-full">
            <BottomNavigation />
          </div>
        </footer>
      </div>
    </div>
  );
}
