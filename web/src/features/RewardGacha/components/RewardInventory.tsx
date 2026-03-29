import { PackageOpen, X } from 'lucide-react';

import { useRewardInventory } from '@/hooks/RewardGacha';

interface RewardInventoryProps {
  isOpen: boolean;
  onClose: () => void;
}

export function RewardInventory({ isOpen, onClose }: RewardInventoryProps) {
  const { data, isLoading, isError } = useRewardInventory(isOpen);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-[hsl(var(--surface-default))]">
      <header className="flex items-center justify-between border-b border-[hsl(var(--border-default))] px-5 py-4">
        <div className="flex items-center gap-2">
          <PackageOpen size={24} className="text-[hsl(var(--primary-3))]" />
          <h2 className="text-xl font-black text-[hsl(var(--text-primary))]">
            내 인벤토리
          </h2>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="rounded-full p-2 text-[hsl(var(--text-secondary))] hover:bg-[hsl(var(--surface-subtle))]"
        >
          <X size={24} />
        </button>
      </header>

      <div className="flex-1 overflow-y-auto p-5">
        {isLoading ? (
          <div className="flex h-full items-center justify-center">
            <span className="text-sm text-[hsl(var(--text-secondary))] animate-pulse">
              인벤토리 불러오는 중...
            </span>
          </div>
        ) : isError || !data ? (
          <div className="flex h-full items-center justify-center">
            <span className="text-sm text-[hsl(var(--text-secondary))]">
              인벤토리를 불러오지 못했어요.
            </span>
          </div>
        ) : data.items.length === 0 ? (
          <div className="flex h-full flex-col items-center justify-center gap-4">
            <PackageOpen size={48} className="text-[hsl(var(--border-strong))]" />
            <span className="text-sm text-[hsl(var(--text-secondary))]">
              아직 획득한 리워드가 없습니다.
            </span>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-4">
            {data.items.map((item) => (
              <div
                key={item.rewardGachaDrawCardId}
                className="flex flex-col items-center gap-3 rounded-[1.5rem] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-subtle))] p-4 shadow-[var(--shadow-sm)]"
              >
                <div className="relative flex aspect-square w-full items-center justify-center overflow-hidden rounded-[1rem] bg-[hsl(var(--surface-default))]">
                  <img
                    src={item.imageUrl}
                    alt={item.title}
                    className="h-full w-full object-cover"
                  />
                </div>
                <div className="w-full text-center">
                  <span className="block text-sm font-bold text-[hsl(var(--text-primary))] line-clamp-2">
                    {item.title}
                  </span>
                  <span className="mt-1 block text-[10px] text-[hsl(var(--text-secondary))]">
                    {new Date(item.acquiredAt).toLocaleDateString('ko-KR')}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
