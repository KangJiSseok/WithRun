import { Timer } from 'lucide-react';

import type { SplitResult } from '@/apis/runningSessions';
import { cn, formatPace } from '@/shared/utils';

type SplitTableProps = {
  splits: SplitResult[];
};

export function SplitTable({ splits }: SplitTableProps) {
  if (splits.length === 0) return null;

  const avgSplitSec =
    splits.reduce((sum, s) => sum + s.splitPaceSecPerKm, 0) / splits.length;

  return (
    <div className="bg-[hsl(var(--secondary-2))] rounded-2xl p-4">
      <div className="flex items-center gap-2 mb-4">
        <Timer size={16} className="text-primary" />
        <span className="caption-b text-[hsl(var(--text-inverse))]">구간별 페이스 (1km)</span>
      </div>
      <div className="rounded-xl overflow-hidden border border-[hsl(var(--secondary-3))]">
        <table className="w-full">
          <thead className="bg-[hsl(var(--secondary-3))]">
            <tr>
              <th className="px-4 py-2 text-left caption-r text-[hsl(var(--secondary-5))] font-normal">구간</th>
              <th className="px-4 py-2 text-left caption-r text-[hsl(var(--secondary-5))] font-normal">페이스</th>
              <th className="px-4 py-2 text-left caption-r text-[hsl(var(--secondary-5))] font-normal">상태</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[hsl(var(--secondary-3))]">
            {splits.map((split) => (
              <tr key={split.splitIndex}>
                <td className="px-4 py-3 caption-m text-[hsl(var(--text-inverse))]">
                  {split.splitIndex}km
                </td>
                <td className="px-4 py-3 caption-m text-primary font-bold">
                  {formatPace(split.splitPaceSecPerKm)}
                </td>
                <td className="px-4 py-3">
                  <div
                    className={cn(
                      'w-2 h-2 rounded-full',
                      split.splitPaceSecPerKm < avgSplitSec
                        ? 'bg-[hsl(var(--success))]'
                        : 'bg-[hsl(var(--warning))]',
                    )}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
