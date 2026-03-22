import { useNavigate } from 'react-router-dom';

import { Button } from '@/shared/components/shadcn/button';

const ONBOARDING_STORAGE_KEY = 'withrun:onboarding-complete';

export function OnboardingPage() {
  const navigate = useNavigate();

  function handleCompleteOnboarding() {
    localStorage.setItem(ONBOARDING_STORAGE_KEY, 'true');
    navigate('/home', { replace: true });
  }

  function handleResetOnboarding() {
    localStorage.removeItem(ONBOARDING_STORAGE_KEY);
  }

  return (
    <div className="flex min-h-screen flex-col justify-between bg-[linear-gradient(180deg,_hsl(var(--surface-default))_0%,_hsl(var(--surface-subtle))_100%)] px-[var(--space-6)] py-[var(--space-8)]">
      <div className="space-y-[var(--space-4)] pt-[var(--space-10)]">
        <p className="caption-b text-[hsl(var(--primary-1))]">ENTRY GATE TEST</p>
        <h1 className="h1-b text-[hsl(var(--text-primary))]">
          온보딩 진입 페이지가 연결되었습니다
        </h1>
        <p className="body-l-r text-[hsl(var(--text-secondary))]">
          지금은 라우트와 첫 진입 분기만 연결한 상태입니다. 이후 이 자리에
          실제 인트로, 로그인, 온보딩 디자인을 순서대로 붙이면 됩니다.
        </p>
      </div>

      <div className="space-y-[var(--space-3)]">
        <Button
          type="button"
          onClick={handleCompleteOnboarding}
          className="h-14 w-full rounded-[var(--radius-xl)] body-l-b"
        >
          임시로 온보딩 완료 처리하고 홈으로 이동
        </Button>
        <Button
          type="button"
          variant="outline"
          onClick={handleResetOnboarding}
          className="h-14 w-full rounded-[var(--radius-xl)] body-l-b"
        >
          온보딩 완료 플래그 초기화
        </Button>
      </div>
    </div>
  );
}
