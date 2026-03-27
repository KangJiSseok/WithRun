import { useNavigate } from 'react-router-dom';
import { Bookmark, Settings2 } from 'lucide-react';

export default function TopNavigation() {
  const navigate = useNavigate();

  return (
    <header className="shrink-0 px-5 pt-6">
      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={() => navigate('/profile')}
          className="flex h-12 w-12 items-center justify-center rounded-full border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))] shadow-sm"
          aria-label="설정으로 이동"
        >
          <Settings2 size={20} />
        </button>

        <button
          type="button"
          className="flex h-12 w-12 items-center justify-center rounded-full border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))] shadow-sm"
          disabled
          aria-label="북마크"
        >
          <Bookmark size={20} />
        </button>
      </div>
    </header>
  );
}
