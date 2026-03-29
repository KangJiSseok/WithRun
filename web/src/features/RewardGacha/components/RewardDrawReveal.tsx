import { useState } from 'react';
import { Gift, Sparkles, X } from 'lucide-react';

import type { RewardGachaDrawCardResponse } from '@/apis/rewardGacha';
import { cn } from '@/shared/utils';

interface RewardDrawRevealProps {
  cards: RewardGachaDrawCardResponse[];
  onClose: () => void;
}

export function RewardDrawReveal({ cards, onClose }: RewardDrawRevealProps) {
  const [flippedCards, setFlippedCards] = useState<Set<number>>(new Set());

  const handleFlip = (index: number) => {
    setFlippedCards((prev) => {
      const next = new Set(prev);
      next.add(index);
      return next;
    });
  };

  const handleRevealAll = () => {
    setFlippedCards(new Set(cards.map((_, i) => i)));
  };

  const allFlipped = flippedCards.size === cards.length;

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-[hsl(var(--surface-inverse))] text-[hsl(var(--text-inverse))]">
      <header className="flex items-center justify-between px-5 py-4">
        <h2 className="text-xl font-black">가챠 결과</h2>
        {allFlipped && (
          <button
            type="button"
            onClick={onClose}
            className="rounded-full p-2 hover:bg-[hsl(var(--surface-strong))]"
          >
            <X size={24} />
          </button>
        )}
      </header>

      <div className="flex-1 overflow-y-auto p-5">
        <div className="grid grid-cols-2 gap-4">
          {cards.map((card, index) => {
            const isFlipped = flippedCards.has(index);
            const isReward = card.cardType === 'REWARD';

            return (
              <button
                key={card.rewardGachaDrawCardId}
                type="button"
                onClick={() => handleFlip(index)}
                disabled={isFlipped}
                className={cn(
                  'relative aspect-[3/4] w-full transition-transform duration-300',
                  !isFlipped && 'hover:scale-105 active:scale-95'
                )}
              >
                {!isFlipped ? (
                  <div className="absolute inset-0 flex flex-col items-center justify-center rounded-[1.5rem] border-2 border-[hsl(var(--border-strong))] bg-[hsl(var(--surface-default))] shadow-[var(--shadow-lg)]">
                    <div className="mb-3 rounded-full bg-[hsl(var(--primary-5))] p-4 text-[hsl(var(--primary-2))]">
                      <Gift size={40} />
                    </div>
                    <span className="text-sm font-bold tracking-[0.14em] text-[hsl(var(--text-secondary))]">
                      TAP TO OPEN
                    </span>
                  </div>
                ) : (
                  <div
                    className={cn(
                      'absolute inset-0 flex flex-col items-center justify-center rounded-[1.5rem] border-2 p-4 shadow-[var(--shadow-lg)] animate-in fade-in zoom-in duration-300',
                      isReward
                        ? 'border-[hsl(var(--primary-3))] bg-[hsl(var(--surface-default))]'
                        : 'border-[hsl(var(--border-default))] bg-[hsl(var(--surface-subtle))]'
                    )}
                  >
                    {isReward ? (
                      <>
                        <div className="relative mb-4 flex h-24 w-24 items-center justify-center overflow-hidden rounded-full bg-[hsl(var(--surface-subtle))]">
                          <img
                            src={card.imageUrl!}
                            alt={card.title!}
                            className="h-full w-full object-cover"
                          />
                        </div>
                        <span className="text-center text-sm font-bold text-[hsl(var(--text-primary))] line-clamp-2">
                          {card.title}
                        </span>
                        <Sparkles
                          size={24}
                          className="absolute right-4 top-4 text-[hsl(var(--primary-3))]"
                        />
                      </>
                    ) : (
                      <>
                        <div className="mb-4 text-[hsl(var(--text-secondary))]">
                          <X size={48} />
                        </div>
                        <span className="text-center text-sm font-bold text-[hsl(var(--text-secondary))]">
                          아쉽네요!
                          <br />
                          다음 기회에
                        </span>
                      </>
                    )}
                  </div>
                )}
              </button>
            );
          })}
        </div>
      </div>

      <div className="p-5">
        {!allFlipped ? (
          <button
            type="button"
            onClick={handleRevealAll}
            className="w-full rounded-[1.5rem] bg-[hsl(var(--primary-1))] px-4 py-4 text-xl font-black text-[hsl(var(--text-inverse))] transition-all hover:bg-[hsl(var(--primary-1-hover))] active:scale-95"
          >
            모두 열기
          </button>
        ) : (
          <button
            type="button"
            onClick={onClose}
            className="w-full rounded-[1.5rem] bg-[hsl(var(--surface-default))] px-4 py-4 text-xl font-black text-[hsl(var(--text-primary))] transition-all hover:bg-[hsl(var(--surface-subtle))] active:scale-95"
          >
            닫기
          </button>
        )}
      </div>
    </div>
  );
}
