import { useRewardShowcase } from '@/hooks/RewardGacha';

export function RewardShowcase() {
  const { data, isLoading, isError } = useRewardShowcase();

  if (isLoading) {
    return (
      <div className="flex h-32 items-center justify-center rounded-[1.5rem] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))]">
        <span className="text-sm text-[hsl(var(--text-secondary))] animate-pulse">
          쇼케이스 불러오는 중...
        </span>
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="flex h-32 items-center justify-center rounded-[1.5rem] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))]">
        <span className="text-sm text-[hsl(var(--text-secondary))]">
          쇼케이스를 불러오지 못했어요.
        </span>
      </div>
    );
  }

  if (data.items.length === 0) {
    return (
      <div className="flex h-32 items-center justify-center rounded-[1.5rem] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))]">
        <span className="text-sm text-[hsl(var(--text-secondary))]">
          현재 전시 중인 리워드가 없습니다.
        </span>
      </div>
    );
  }

  return (
    <div className="flex gap-4 overflow-x-auto pb-4 snap-x snap-mandatory scrollbar-hide">
      {data.items.map((item) => (
        <div
          key={item.rewardItemId}
          className="flex min-w-[140px] flex-col items-center gap-3 rounded-[1.5rem] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-4 shadow-[var(--shadow-sm)] snap-center"
        >
          <div className="relative flex h-20 w-20 items-center justify-center overflow-hidden rounded-full bg-[hsl(var(--surface-subtle))]">
            <img
              src={item.imageUrl}
              alt={item.title}
              className="h-full w-full object-cover"
            />
          </div>
          <span className="text-center text-xs font-bold text-[hsl(var(--text-primary))] line-clamp-2">
            {item.title}
          </span>
        </div>
      ))}
    </div>
  );
}
