import { useNavigate } from 'react-router-dom';
import { Bookmark, Settings2 } from 'lucide-react';
import { Button } from '../shadcn/button';

export default function TopNavigation() {
  const navigate = useNavigate();

  return (
    <header className="flex h-16 shrink-0 items-center justify-between px-[var(--space-4)]">
      <Button
        type="button"
        variant="outline"
        size="icon"
        // 상단 공통 설정 버튼은 현재 /profile 경로를 설정 화면으로 재정의해서 사용합니다.
        onClick={() => navigate('/profile')}
        aria-label="설정으로 이동"
        className="h-12 w-12 rounded-full border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))]"
      >
        <Settings2 size={20} />
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
