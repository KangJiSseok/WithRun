import { Bookmark, User } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

import { cn } from '../../utils';

export default function TopNavigation({
  showBottomBorder = false,
}: {
  showBottomBorder?: boolean;
}) {
  const navigate = useNavigate();

  return (
    <header
      className={cn(
        'shrink-0 bg-surface-subtle px-5 py-3',
        showBottomBorder && 'border-b border-border-default'
      )}
    >
      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={() => navigate('/profile')}
          className="flex h-12 w-12 items-center justify-center rounded-full border border-border-default bg-surface-default text-fg-primary shadow-sm"
          aria-label="프로필로 이동"
        >
          <User size={20} />
        </button>

        <button
          type="button"
          className="flex h-12 w-12 items-center justify-center rounded-full border border-border-default bg-surface-default text-fg-primary shadow-sm"
          disabled
          aria-label="북마크"
        >
          <Bookmark size={20} />
        </button>
      </div>
    </header>
  );
}
