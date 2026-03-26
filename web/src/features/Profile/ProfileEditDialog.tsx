import { Camera, LoaderCircle } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';

import type { AuthGender } from '@/apis/auth';
import {
  type UpdateUserProfileRequest,
  type UserProfileResponse,
  updateUserProfile,
} from '@/apis/UserProfile';
import { Button } from '@/shared/components/shadcn/button';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/shared/components/shadcn/dialog';
import { Input } from '@/shared/components/shadcn/input';
import { cn } from '@/shared/utils';

type ProfileEditDialogProps = {
  isOpen: boolean;
  profile: UserProfileResponse;
  onOpenChange: (open: boolean) => void;
  onSaved: (profile: UserProfileResponse) => void;
};

type ProfileFormValues = {
  nickname: string;
  birthDate: string;
  gender: AuthGender | null;
  height: string;
  weight: string;
};

function createFormValues(profile: UserProfileResponse): ProfileFormValues {
  return {
    nickname: profile.nickname ?? '',
    birthDate: profile.birthDate ?? '',
    gender: profile.gender,
    height: profile.height?.toString() ?? '',
    weight: profile.weight?.toString() ?? '',
  };
}

function parsePositiveNumber(value: string): number | null {
  const parsedValue = Number(value);

  if (!Number.isFinite(parsedValue) || parsedValue <= 0) {
    return null;
  }

  return parsedValue;
}

function resolveInitial(nickname: string): string {
  return nickname.trim().charAt(0).toUpperCase() || 'W';
}

export function ProfileEditDialog({
  isOpen,
  profile,
  onOpenChange,
  onSaved,
}: ProfileEditDialogProps) {
  const [formValues, setFormValues] = useState<ProfileFormValues>(
    createFormValues(profile),
  );
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    setFormValues(createFormValues(profile));
  }, [isOpen, profile]);

  const parsedHeight = useMemo(
    () => parsePositiveNumber(formValues.height),
    [formValues.height],
  );
  const parsedWeight = useMemo(
    () => parsePositiveNumber(formValues.weight),
    [formValues.weight],
  );
  // 시안에는 생년월일 입력이 없지만, 백엔드 수정 DTO 에 필수라서 null 사용자에게만 추가로 받는다.
  const needsBirthDateInput = profile.birthDate === null;
  const isFormValid =
    formValues.nickname.trim().length > 0 &&
    formValues.birthDate.length > 0 &&
    formValues.gender !== null &&
    parsedHeight !== null &&
    parsedWeight !== null;

  function updateField<K extends keyof ProfileFormValues>(
    field: K,
    value: ProfileFormValues[K],
  ) {
    setFormValues((currentValues) => ({
      ...currentValues,
      [field]: value,
    }));
  }

  async function handleSubmit() {
    if (!isFormValid || isSubmitting || !formValues.gender) {
      return;
    }

    const payload: UpdateUserProfileRequest = {
      // 이미 저장된 birthDate 는 숨김 필드처럼 유지해서 부분 수정이 아닌 전체 DTO 저장 규칙을 맞춘다.
      nickname: formValues.nickname.trim(),
      birthDate: formValues.birthDate,
      gender: formValues.gender,
      height: parsedHeight!,
      weight: parsedWeight!,
    };

    setIsSubmitting(true);

    try {
      await updateUserProfile(payload);
      onSaved(payload);
      toast.success('프로필이 저장되었어요.');
      onOpenChange(false);
    } catch {
      toast.error('프로필을 저장하지 못했어요. 다시 시도해주세요.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={onOpenChange}>
      <DialogContent
        showCloseButton
        className="w-[calc(100%-2rem)] max-w-[360px] gap-6 rounded-[2rem] border-0 bg-[linear-gradient(180deg,#101116_0%,#171A22_100%)] p-6 text-white shadow-[0_24px_80px_rgba(0,0,0,0.45)]"
      >
        <DialogHeader className="gap-1 text-left">
          <DialogTitle className="text-xl font-black text-white">
            프로필 수정
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-5">
          <div className="flex justify-center">
            <div className="relative flex h-24 w-24 items-center justify-center rounded-full bg-[radial-gradient(circle_at_top,#4b2c2c,#16181f)] shadow-[0_20px_40px_rgba(0,0,0,0.35)]">
              <span className="text-3xl font-black text-white/90">
                {resolveInitial(formValues.nickname)}
              </span>
              <div className="absolute bottom-0 right-0 flex h-8 w-8 items-center justify-center rounded-full border border-white/10 bg-[#2D3039] text-white/85">
                <Camera size={14} />
              </div>
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-white/75">
              닉네임
            </label>
            <Input
              value={formValues.nickname}
              onChange={(event) => updateField('nickname', event.target.value)}
              placeholder="닉네임을 입력해주세요"
              className="h-12 rounded-2xl border-white/10 bg-white/8 px-4 text-base text-white placeholder:text-white/35"
            />
          </div>

          {needsBirthDateInput ? (
            <div className="space-y-2">
              <label className="text-sm font-semibold text-white/75">
                생년월일
              </label>
              <Input
                type="date"
                value={formValues.birthDate}
                onChange={(event) =>
                  updateField('birthDate', event.target.value)
                }
                className="h-12 rounded-2xl border-white/10 bg-white/8 px-4 text-base text-white"
              />
            </div>
          ) : null}

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <label className="text-sm font-semibold text-white/75">
                키 (cm)
              </label>
              <Input
                type="number"
                inputMode="decimal"
                value={formValues.height}
                onChange={(event) => updateField('height', event.target.value)}
                placeholder="175"
                className="h-12 rounded-2xl border-white/10 bg-white/8 px-4 text-base text-white placeholder:text-white/35"
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm font-semibold text-white/75">
                몸무게 (kg)
              </label>
              <Input
                type="number"
                inputMode="decimal"
                value={formValues.weight}
                onChange={(event) => updateField('weight', event.target.value)}
                placeholder="70"
                className="h-12 rounded-2xl border-white/10 bg-white/8 px-4 text-base text-white placeholder:text-white/35"
              />
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-white/75">성별</label>
            <div className="grid grid-cols-2 gap-3">
              {(['MALE', 'FEMALE'] as const).map((gender) => {
                const isSelected = formValues.gender === gender;

                return (
                  <button
                    key={gender}
                    type="button"
                    onClick={() => updateField('gender', gender)}
                    className={cn(
                      'h-12 rounded-2xl border text-base font-bold transition-colors',
                      isSelected
                        ? 'border-[hsl(var(--primary-1))] bg-[hsl(var(--primary-1)/0.18)] text-[hsl(var(--primary-1))]'
                        : 'border-white/10 bg-white/8 text-white/70',
                    )}
                  >
                    {gender === 'MALE' ? '남성' : '여성'}
                  </button>
                );
              })}
            </div>
          </div>

          <Button
            type="button"
            onClick={() => void handleSubmit()}
            disabled={!isFormValid || isSubmitting}
            className="h-14 w-full rounded-2xl text-base font-black"
          >
            {isSubmitting ? (
              <>
                <LoaderCircle size={18} className="animate-spin" />
                저장 중...
              </>
            ) : (
              '저장하기'
            )}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
