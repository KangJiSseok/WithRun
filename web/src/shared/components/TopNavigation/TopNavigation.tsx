import { Bookmark, User } from 'lucide-react';
import { Button } from '../shadcn/button';

export default function TopNavigation() {
  return (
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
  );
}
