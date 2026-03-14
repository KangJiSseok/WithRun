// 공식 코스 등록 확인 모달 (Yes/No)
// absolute 포지셔닝으로 MobileLayout 프레임 안에서 렌더링
// API 호출 없음 — 실제 등록 로직은 CourseShareModal에서 처리

import { UploadCloud } from 'lucide-react';
import { Button } from '@/shared/components/shadcn/button';

type CourseRegisterModalProps = {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
};

export function CourseRegisterModal({ isOpen, onClose, onConfirm }: CourseRegisterModalProps) {
  if (!isOpen) return null;

  return (
    // absolute inset-0: 외부 컨테이너 (MobileLayout 프레임 안)
    <div
      className="absolute inset-0 z-modal flex items-center justify-center bg-black/50"
      onClick={onClose}
    >
      <div
        className="w-[calc(100%-2rem)] max-w-[343px] flex flex-col gap-[var(--space-6)] rounded-[var(--radius-xl)] bg-[hsl(var(--secondary-2))] p-[var(--space-6)] border border-[hsl(var(--secondary-3))]"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex flex-col items-center gap-[var(--space-4)]">
          <div className="p-3 rounded-2xl bg-primary/20 border border-primary/30">
            <UploadCloud size={32} className="text-primary" />
          </div>
          <h3 className="h3-b text-[hsl(var(--text-inverse))] text-center">
            공식 코스로 등록하시겠습니까?
          </h3>
          <p className="body-r text-[hsl(var(--secondary-5))] text-center">
            내가 달린 경로를 다른 러너들과 공유하고 함께 달려보세요!
          </p>
        </div>

        <div className="flex flex-col gap-[var(--space-3)]">
          {/* onClick={onConfirm}은 prop으로 받은 함수를 그대로 호출 */}
          {/* 상태 변경은 FreeRunResultScreen에서 처리 */}
          <Button
            onClick={onConfirm}
            className="w-full py-4 rounded-2xl bg-primary text-[hsl(var(--text-inverse))] body-b h-auto"
          >
            코스 등록하기
          </Button>
          <Button
            variant="outline"
            onClick={onClose}
            className="w-full py-4 rounded-2xl bg-transparent border-[hsl(var(--secondary-4))] text-[hsl(var(--secondary-5))] body-b h-auto hover:bg-[hsl(var(--secondary-3))] hover:text-[hsl(var(--text-inverse))]"
          >
            취소
          </Button>
        </div>
      </div>
    </div>
  );
}
