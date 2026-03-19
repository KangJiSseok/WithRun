import { RotateCcw, X } from 'lucide-react';

import {
  Drawer,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from '@/shared/components/shadcn/drawer';
import { Button } from '@/shared/components/shadcn/button';

const DISTANCE_OPTIONS = ['전체', '5km 미만', '5km - 10km', '10km 이상'];
const COURSE_TYPE_OPTIONS = [
  '전체',
  '도심',
  '공원',
  '한강변',
  '산/둘레길',
  '트랙',
  '기타',
];
const DIFFICULTY_OPTIONS = ['전체', '쉬움', '보통', '어려움'];

export type RunningFilterState = {
  distance: string[];
  courseType: string[];
  difficulty: string[];
};

type FilterCategory = {
  key: keyof RunningFilterState;
  label: string;
  options: string[];
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
    key: 'distance',
    label: '거리',
    options: DISTANCE_OPTIONS,
  },
  {
    key: 'courseType',
    label: '코스 타입',
    options: COURSE_TYPE_OPTIONS,
  },
  {
    key: 'difficulty',
    label: '난이도',
    options: DIFFICULTY_OPTIONS,
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
    <Drawer open={isOpen} onOpenChange={(nextOpen) => !nextOpen && onClose()}>
      <DrawerContent className="border-[hsl(var(--border-default)/0.1)] bg-[hsl(var(--surface-default))] px-6 pb-6 z-modal">
        <DrawerHeader className="gap-3 px-0 text-left">
          <div className="flex items-center justify-between">
            <DrawerTitle className="h3-b">상세 필터</DrawerTitle>
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
        </DrawerHeader>

        <div className="flex max-h-[55vh] flex-col gap-6 overflow-y-auto">
          {FILTER_CATEGORIES.map((category) => (
            <section key={category.key} className="flex flex-col gap-3">
              <h3 className="body-l-b">{category.label}</h3>

              <div className="flex flex-wrap gap-2">
                {category.options.map((option) => {
                  const valuesForCategory = values[category.key];
                  const isActive =
                    option === '전체'
                      ? valuesForCategory.length === 0
                      : valuesForCategory.includes(option);

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

        <DrawerFooter className="flex-row gap-3 px-0 pt-6">
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
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  );
}
