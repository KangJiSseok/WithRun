import { ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export function CalendarDetailHeader() {
  const navigate = useNavigate();

  return (
    <header className="shrink-0 border-b border-[hsl(var(--border-default))] bg-[hsl(var(--surface-subtle))] px-[var(--space-5)] py-[var(--space-3)]">
      <button
        type="button"
        onClick={() => navigate('/calendar')}
        className="flex h-12 w-12 items-center justify-center rounded-full border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))] shadow-[var(--shadow-sm)]"
        aria-label="뒤로가기"
      >
        <ArrowLeft size={20} />
      </button>
    </header>
  );
}
