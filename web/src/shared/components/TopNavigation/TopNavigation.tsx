import { useNavigate } from 'react-router-dom';
import { Bookmark, User } from 'lucide-react';
import { Button } from '../shadcn/button';

export default function TopNavigation() {
  const navigate = useNavigate();

  return (
    <header className="flex h-16 shrink-0 items-center justify-between px-[var(--space-4)]">
      <Button
        type="button"
        variant="outline"
        size="icon"
        // 상단 공통 프로필 버튼은 마이페이지 도입 후 /profile 로 통일합니다.
        onClick={() => navigate('/profile')}
        className="h-12 w-12 rounded-full border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))]"
      >
        <User size={20} />
      </Button>

      <Button
        type="button"
        variant="outline"
        size="icon"
        disabled
        className="h-12 w-12 rounded-full border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))]"
      >
        <Bookmark size={20} />
      </Button>
    </header>
  );
}
