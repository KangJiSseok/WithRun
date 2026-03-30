import { useMemo, useState } from 'react';
import { Coins, Gift, History, PackageOpen, Sparkles } from 'lucide-react';
import { toast } from 'sonner';

import type { RewardGachaDrawResponse } from '@/apis/rewardGacha';
import type { RewardPointHistoryItem, RewardPointReason } from '@/apis/rewardPoints';
import { useDrawRewardGacha, useRewardPoints } from '@/hooks/RewardGacha';
import { cn } from '@/shared/utils';

import { RewardDrawReveal } from './components/RewardDrawReveal';
import { RewardInventory } from './components/RewardInventory';
import { RewardShowcase } from './components/RewardShowcase';

const REWARD_POINT_REASON_LABELS: Record<RewardPointReason, string> = {
  DAILY_RUNNING: '일반 러닝 보상',
  WEEKLY_STREAK_3_DAYS: '주간 스트릭 3일',
  WEEKLY_STREAK_5_DAYS: '주간 스트릭 5일',
  WEEKLY_STREAK_7_DAYS: '주간 스트릭 7일',
  GHOST_WIN: '고스트 승리 보상',
  GACHA_DRAW: '가챠 사용',
};

function formatHistoryReason(reason: RewardPointHistoryItem['reason']): string {
  return REWARD_POINT_REASON_LABELS[reason] ?? reason;
}

export function RewardGachaScreen() {
  const { balanceQuery, historyQuery } = useRewardPoints();
  const drawMutation = useDrawRewardGacha();

  const [showHistory, setShowHistory] = useState(false);
  const [isInventoryOpen, setIsInventoryOpen] = useState(false);
  const [activeDrawResult, setActiveDrawResult] = useState<RewardGachaDrawResponse | null>(
    null
  );

  const balance = balanceQuery.data?.currentBalance ?? null;
  const displayedBalance = activeDrawResult?.remainingBalance ?? balance;
  const histories = historyQuery.data?.items ?? [];
  const isLoadingBalance = balanceQuery.isLoading;
  const isBalanceError = balanceQuery.isError;
  const isLoadingHistories = historyQuery.isLoading;

  const drawButtonLabel = useMemo(() => {
    if (isLoadingBalance) {
      return '포인트 확인 중...';
    }
    if (drawMutation.isPending) {
      return '뽑는 중...';
    }
    return '뽑기 시작';
  }, [isLoadingBalance, drawMutation.isPending]);

  async function handleDrawGacha() {
    if (displayedBalance === null || displayedBalance < 1) {
      toast.error('포인트가 부족합니다.');
      return;
    }

    try {
      const result = await drawMutation.mutateAsync();
      setActiveDrawResult(result);
      void drawMutation.refreshRewardGachaState();
    } catch {
      toast.error('가챠 뽑기에 실패했어요. 다시 시도해주세요.');
    }
  }

  return (
    <div className="-m-5 min-h-[calc(100dvh-5rem)] overflow-hidden bg-[hsl(var(--surface-subtle))] text-[hsl(var(--text-primary))]">
      <div className="relative min-h-full px-5 pb-10 pt-6">
        <div className="relative mx-auto flex max-w-[430px] flex-col gap-6">
          <section className="space-y-2">
            <h1 className="text-4xl font-black leading-tight tracking-tight text-[hsl(var(--text-primary))]">
              리워드 뽑기
            </h1>
            <p className="text-base leading-7 text-[hsl(var(--text-secondary))]">
              달리기로 모은 포인트로 리워드를 뽑아보세요!
            </p>
          </section>

          <section>
            <h2 className="mb-3 text-lg font-black text-[hsl(var(--text-primary))]">
              현재 전시 중인 리워드
            </h2>
            <RewardShowcase />
          </section>

          <section className="grid grid-cols-[1fr_auto] items-center gap-4 rounded-[2rem] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-6 shadow-[var(--shadow-md)]">
            <div className="relative overflow-hidden rounded-[1.5rem] border border-[hsl(var(--border-strong))] bg-[hsl(var(--surface-inverse))] px-5 py-4 text-[hsl(var(--text-inverse))]">
              <div className="absolute -right-4 -top-4 opacity-10">
                <Coins size={120} />
              </div>
              <p className="text-xs font-bold tracking-[0.18em] text-[hsl(var(--secondary-6))]">
                REWARD POINT
              </p>
              <div className="mt-3 flex items-end gap-2">
                <Coins className="mb-1 text-[hsl(var(--primary-3))]" size={24} />
                {isLoadingBalance ? (
                  <span className="text-4xl font-black tracking-[0.08em] text-[hsl(var(--text-inverse))] animate-pulse">
                    ---
                  </span>
                ) : (
                  <span className="text-4xl font-black tracking-[0.08em] text-[hsl(var(--text-inverse))]">
                    {displayedBalance?.toLocaleString('ko-KR') ?? 0}
                  </span>
                )}
                <span className="pb-1 text-lg font-bold text-[hsl(var(--secondary-6))]">
                  P
                </span>
              </div>
              <p className="mt-2 text-xs leading-5 text-[hsl(var(--secondary-6))]">
                {isLoadingBalance
                  ? '포인트를 확인하는 중이에요.'
                  : isBalanceError
                    ? '포인트를 불러오지 못했어요.'
                    : '현재 보유 중인 리워드 포인트입니다.'}
              </p>
            </div>

            <button
              type="button"
              onClick={() => setIsInventoryOpen(true)}
              className="flex min-h-28 min-w-24 flex-col items-center justify-center gap-2 rounded-[1.5rem] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-subtle))] px-4 py-3 text-center transition-colors hover:bg-[hsl(var(--surface-muted))]"
            >
              <PackageOpen size={24} className="text-[hsl(var(--text-secondary))]" />
              <span className="text-sm font-bold text-[hsl(var(--text-primary))]">
                인벤토리
              </span>
            </button>
          </section>

          <section className="flex flex-col items-center rounded-[2rem] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-8 text-center shadow-[var(--shadow-md)]">
            <div className="relative mb-6 flex h-48 w-full max-w-[260px] items-center justify-center">
              <div className="absolute inset-x-6 bottom-5 h-6 rounded-full bg-[hsl(var(--surface-muted))] blur-xl" />
              <div className="absolute left-1/2 top-7 h-36 w-28 -translate-x-[54%] rounded-[1.5rem] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-subtle))] shadow-[var(--shadow-soft-lg)]" />
              <div className="absolute left-1/2 top-4 h-36 w-28 -translate-x-[46%] rounded-[1.5rem] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-muted))] shadow-[var(--shadow-soft-lg)]" />
              <div className="relative flex h-40 w-32 flex-col items-center justify-center rounded-[1.75rem] border border-[hsl(var(--border-strong))] bg-[hsl(var(--surface-default))] shadow-[var(--shadow-soft-xl)]">
                <div className="mb-3 rounded-full bg-[hsl(var(--primary-5))] p-4 text-[hsl(var(--primary-2))]">
                  <Gift size={40} />
                </div>
                <span className="text-sm font-bold tracking-[0.14em] text-[hsl(var(--text-secondary))]">
                  MYSTERY PACK
                </span>
                <Sparkles
                  size={18}
                  className="absolute right-4 top-4 text-[hsl(var(--primary-3))]"
                />
              </div>
            </div>

            <h2 className="mb-2 text-2xl font-black text-[hsl(var(--text-primary))]">
              오늘의 리워드 가챠
            </h2>
            <p className="mb-8 max-w-[18rem] text-sm leading-6 text-[hsl(var(--text-secondary))]">
              1포인트를 사용하여 리워드를 뽑아보세요!
            </p>

            <button
              type="button"
              onClick={() => void handleDrawGacha()}
              disabled={isLoadingBalance || isBalanceError || drawMutation.isPending}
              className={cn(
                'flex w-full items-center justify-center gap-2 rounded-[1.5rem] bg-[hsl(var(--primary-1))] px-4 py-4 text-xl font-black text-[hsl(var(--text-inverse))] transition-all active:scale-95',
                isLoadingBalance || isBalanceError || drawMutation.isPending
                  ? 'pointer-events-none opacity-50'
                  : 'hover:bg-[hsl(var(--primary-1-hover))]'
              )}
            >
              <PackageOpen size={24} />
              {drawButtonLabel}
            </button>
          </section>

          <section>
            <button
              type="button"
              onClick={() => setShowHistory((current) => !current)}
              className={cn(
                'flex w-full items-center justify-between rounded-[1.5rem] border border-[hsl(var(--border-default))] p-4 shadow-[var(--shadow-sm)] transition-colors',
                showHistory
                  ? 'bg-[hsl(var(--surface-subtle))]'
                  : 'bg-[hsl(var(--surface-default))] hover:bg-[hsl(var(--surface-subtle))]'
              )}
            >
              <div className="flex items-center gap-3">
                <History size={24} className="text-[hsl(var(--text-secondary))]" />
                <div className="text-left">
                  <span className="block text-sm font-bold text-[hsl(var(--text-primary))]">
                    포인트 내역
                  </span>
                  <span className="block text-xs text-[hsl(var(--text-secondary))]">
                    최근 지급 이력을 확인하세요.
                  </span>
                </div>
              </div>
              <span className="text-xs font-bold text-[hsl(var(--text-secondary))]">
                {showHistory ? '접기' : '열기'}
              </span>
            </button>
          </section>

          {showHistory && (
            <section className="rounded-[2rem] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-5 shadow-[var(--shadow-md)]">
              <h3 className="mb-4 text-lg font-black text-[hsl(var(--text-primary))]">
                최근 포인트 내역
              </h3>

              {isLoadingHistories ? (
                <div className="py-8 text-center text-sm text-[hsl(var(--text-secondary))]">
                  내역을 불러오는 중이에요...
                </div>
              ) : histories.length === 0 ? (
                <div className="py-8 text-center text-sm text-[hsl(var(--text-secondary))]">
                  포인트 내역이 없습니다.
                </div>
              ) : (
                <div className="space-y-4">
                  {histories.map((history) => (
                    <div
                      key={history.rewardPointHistoryId}
                      className="flex items-center justify-between border-b border-[hsl(var(--border-default))] pb-4 last:border-0 last:pb-0"
                    >
                      <div>
                        <p className="text-sm font-bold text-[hsl(var(--text-primary))]">
                          {formatHistoryReason(history.reason)}
                        </p>
                        <p className="mt-1 text-xs text-[hsl(var(--text-secondary))]">
                          {new Date(history.createdAt).toLocaleDateString('ko-KR', {
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                        </p>
                      </div>
                      <div
                        className={cn(
                          'text-base font-black',
                          history.deltaPoint > 0
                            ? 'text-[hsl(var(--success))]'
                            : 'text-[hsl(var(--error))]'
                        )}
                      >
                        {history.deltaPoint > 0 ? '+' : ''}
                        {history.deltaPoint} P
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>
          )}

          <div className="h-8" />
        </div>
      </div>

      <RewardInventory
        isOpen={isInventoryOpen}
        onClose={() => setIsInventoryOpen(false)}
      />

      {activeDrawResult && (
        <RewardDrawReveal
          cards={activeDrawResult.cards}
          onClose={() => setActiveDrawResult(null)}
        />
      )}
    </div>
  );
}

