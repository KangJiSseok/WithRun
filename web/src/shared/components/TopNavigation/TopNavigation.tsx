import { useNavigate } from 'react-router-dom';
import { Bookmark, User } from 'lucide-react';

export default function TopNavigation() {
  const navigate = useNavigate();

  return (
    <header className="shrink-0 border-b border-[hsl(var(--border-default))] bg-[hsl(var(--surface-subtle))] px-5 py-3">
      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={() => navigate('/profile')}
          className="flex h-12 w-12 items-center justify-center rounded-full border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))] shadow-[var(--shadow-sm)]"
          aria-label="프로필로 이동"
        >
          <User size={20} />
        </button>

        <button
          type="button"
          onClick={() => navigate('/bookmark')}
          className="flex h-12 w-12 items-center justify-center rounded-full border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))] shadow-[var(--shadow-sm)]"
          aria-label="저장된 코스"
        >
          <Bookmark size={20} />
        </button>
      </div>
    </header>
  );
}
