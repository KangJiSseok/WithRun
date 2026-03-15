import { Bookmark, User } from 'lucide-react';
import { Outlet } from 'react-router-dom';

import { BottomNavigation } from '../components';
import { Button } from '../components/shadcn/button';

export function MainLayout() {
  return (
    <div className="relative mx-auto flex min-h-screen min-w-[375px] max-w-[430px] items-center justify-center bg-gray-100">
      <div className="flex min-h-screen w-full flex-col overflow-hidden border bg-white shadow-xl">
        <header className="flex items-center justify-between px-[var(--space-4)] pt-[var(--space-4)]">
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

        <main className="flex-1 overflow-y-auto px-[var(--space-4)] pb-24 pt-[var(--space-6)]">
          <Outlet />
        </main>

        <div className="fixed bottom-6 left-6 right-6 z-overlay flex justify-center">
          <div className="w-full min-w-[375px] max-w-[430px] px-4">
            <BottomNavigation />
          </div>
        </div>
      </div>
    </div>
  );
}
