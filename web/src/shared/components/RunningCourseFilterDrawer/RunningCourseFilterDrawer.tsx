import { RotateCcw, X } from 'lucide-react';

import {
  Drawer,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from '@/shared/components/shadcn/drawer';
import { Button } from '@/shared/components/shadcn/button';

export type FilterOption = {
  value: string;
  label: string;
};

export type RunningCourseFilterState = {
  distance: string[];
  courseType: string[];
  difficulty: string[];
};

export type CourseFilterCategory = {
  key: keyof RunningCourseFilterState;
  label: string;
  options: FilterOption[];
};

type RunningCourseFilterDrawerProps = {
  isOpen: boolean;
  categories: CourseFilterCategory[];
  values: RunningCourseFilterState;
  onClose: () => void;
  onApply: () => void;
  onReset: () => void;
  onChange: (key: keyof RunningCourseFilterState, value: string) => void;
};

export function RunningCourseFilterDrawer({
  isOpen,
  categories,
  values,
  onClose,
  onApply,
  onReset,
  onChange,
}: RunningCourseFilterDrawerProps) {
  return (
    <Drawer open={isOpen} onOpenChange={(nextOpen) => !nextOpen && onClose()}>
      <DrawerContent className="z-modal border-[hsl(var(--border-default)/0.1)] bg-[hsl(var(--surface-default))] px-6 pb-6">
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
          {categories.map((category) => (
            <section key={category.key} className="flex flex-col gap-3">
              <h3 className="body-l-b">{category.label}</h3>

              <div className="flex flex-wrap gap-2">
                {category.options.map((option) => {
                  const valuesForCategory = values[category.key];
                  const isActive =
                    option.value === '전체'
                      ? valuesForCategory.length === 0
                      : valuesForCategory.includes(option.value);

                  return (
                    <Button
                      key={option.value}
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => onChange(category.key, option.value)}
                      className={
                        isActive
                          ? 'rounded-full border-[hsl(var(--primary-1))] bg-[hsl(var(--primary-1))] text-[hsl(var(--text-inverse))]'
                          : 'rounded-full'
                      }
                    >
                      {option.label}
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
