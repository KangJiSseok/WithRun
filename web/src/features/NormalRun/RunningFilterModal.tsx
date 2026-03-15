import { RotateCcw, X } from 'lucide-react';

import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/shared/components/shadcn/dialog';
import { Button } from '@/shared/components/shadcn/button';

const PURPOSE_OPTIONS = [
  '전체',
  '다이어트',
  '건강유지',
  '대회준비',
  '체력향상',
];

const DISTANCE_OPTIONS = ['전체', '5km 미만', '5km - 10km', '10km 이상'];
const COURSE_TYPE_OPTIONS = [
  '전체',
  '도심',
  '공원',
  '한강변',
  '산/둘레길',
  '트랙',
];
const DIFFICULTY_OPTIONS = ['전체', '쉬움', '보통', '어려움'];

export type RunningFilterState = {
  purpose: string;
  distance: string[];
  courseType: string[];
  difficulty: string[];
};

type FilterCategory = {
  key: keyof RunningFilterState;
  label: string;
  options: string[];
  multiple: boolean;
};

type RunningFilterModalProps = {
  isOpen: boolean;
  values: RunningFilterState;
  onClose: () => void;
  onApply: () => void;
  onReset: () => void;
  onChange: (key: keyof RunningFilterState, value: string) => void;
};

const FILTER_CATEGORIES: FilterCategory[] = [
  {
    key: 'purpose',
    label: '러닝 목적',
    options: PURPOSE_OPTIONS,
    multiple: false,
  },
  {
    key: 'distance',
    label: '거리',
    options: DISTANCE_OPTIONS,
    multiple: true,
  },
  {
    key: 'courseType',
    label: '코스 타입',
    options: COURSE_TYPE_OPTIONS,
    multiple: true,
  },
  {
    key: 'difficulty',
    label: '난이도',
    options: DIFFICULTY_OPTIONS,
    multiple: true,
  },
];

export function RunningFilterModal({
  isOpen,
  values,
  onClose,
  onApply,
  onReset,
  onChange,
}: RunningFilterModalProps) {
  return (
    <Dialog open={isOpen} onOpenChange={(nextOpen) => !nextOpen && onClose()}>
      <DialogContent
        showCloseButton={false}
        className="max-h-[85vh] w-[calc(100%-1.5rem)] max-w-[375px] rounded-[28px] border-[hsl(var(--border-default)/0.1)] bg-[hsl(var(--surface-default))] p-6"
      >
        <DialogHeader className="gap-3 text-left">
          <div className="flex items-center justify-between">
            <DialogTitle className="h3-b">상세 필터</DialogTitle>
            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={onReset}
                className="gap-1"
              >
                <RotateCcw size={14} />
                초기화
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                onClick={onClose}
                className="size-9"
              >
                <X size={18} />
              </Button>
            </div>
          </div>
        </DialogHeader>

        <div className="flex max-h-[55vh] flex-col gap-6 overflow-y-auto">
          {FILTER_CATEGORIES.map((category) => (
            <section key={category.key} className="flex flex-col gap-3">
              <h3 className="body-l-b">{category.label}</h3>

              <div className="flex flex-wrap gap-2">
                {category.options.map((option) => {
                  const valuesForCategory = values[category.key];
                  const isActive = category.multiple
                    ? option === '전체'
                      ? valuesForCategory.length === 0
                      : valuesForCategory.includes(option)
                    : valuesForCategory === option;

                  return (
                    <Button
                      key={option}
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => onChange(category.key, option)}
                      className={
                        isActive
                          ? 'rounded-full border-[hsl(var(--primary-1))] bg-[hsl(var(--primary-1))] text-[hsl(var(--text-inverse))]'
                          : 'rounded-full'
                      }
                    >
                      {option}
                    </Button>
                  );
                })}
              </div>
            </section>
          ))}
        </div>

        <DialogFooter className="flex-row gap-3">
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            className="flex-1"
          >
            닫기
          </Button>
          <Button type="button" onClick={onApply} className="flex-1">
            필터 적용
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
