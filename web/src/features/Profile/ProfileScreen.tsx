import {
  LogOut,
  PencilLine,
  ShieldAlert,
  Settings2,
} from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';

import { useLogout } from '@/hooks/Auth';
import {
  deleteUserProfile,
  getUserProfile,
  type UserProfileResponse,
} from '@/apis/UserProfile';
import { Button } from '@/shared/components/shadcn/button';
import { useAuthStore } from '@/stores/Auth';
import { cn } from '@/shared/utils';
import { ProfileEditDialog } from './ProfileEditDialog';

type ProfileMetricChipProps = {
  label: string;
  value: string;
};

function createProfileState(): UserProfileResponse {
  const user = useAuthStore.getState().user;

  return {
    nickname: user?.nickname ?? null,
    birthDate: user?.birthDate ?? null,
    gender: user?.gender ?? null,
    height: user?.height ?? null,
    weight: user?.weight ?? null,
  };
}

function formatProvider(provider: string | null | undefined): string {
  if (provider === 'kakao') {
    return 'Kakao';
  }

  if (provider === 'google') {
    return 'Google';
  }

  return 'With Run';
}

function formatBirthDate(value: string | null): string | null {
  if (!value) {
    return null;
  }

  const [year, month, day] = value.split('-');

  if (!year || !month || !day) {
    return value;
  }

  return `${year}.${month}.${day}`;
}

function formatGender(value: UserProfileResponse['gender']): string | null {
  if (value === 'MALE') {
    return '남성';
  }

  if (value === 'FEMALE') {
    return '여성';
  }

  return null;
}

function syncProfileIntoAuthStore(profile: UserProfileResponse): void {
  const currentUser = useAuthStore.getState().user;

  if (!currentUser) {
    return;
  }

  useAuthStore.getState().updateUser({
    ...currentUser,
    ...profile,
  });
}

function ProfileMetricChip({ label, value }: ProfileMetricChipProps) {
  return (
    <div className="rounded-full border border-[hsl(var(--border-default))] bg-[hsl(var(--primary-5))] px-4 py-2 text-sm font-semibold text-[hsl(var(--text-primary))]">
      <span className="text-[hsl(var(--text-secondary))]">{label}</span> {value}
    </div>
  );
}

export function ProfileScreen() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const { handleLogout, isLoggingOut } = useLogout();
  const [profile, setProfile] =
    useState<UserProfileResponse>(createProfileState());
  const [isLoadingProfile, setIsLoadingProfile] = useState(true);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isDeletingAccount, setIsDeletingAccount] = useState(false);

  const providerLabel = useMemo(
    () => formatProvider(user?.provider),
    [user?.provider]
  );
  const genderLabel = useMemo(
    () => formatGender(profile.gender),
    [profile.gender]
  );
  const birthDateLabel = useMemo(
    () => formatBirthDate(profile.birthDate),
    [profile.birthDate]
  );

  useEffect(() => {
    let isCancelled = false;

    void (async () => {
      try {
        // 프로필 화면은 auth store 값이 있어도 서버 조회를 한 번 더 해서 최신 프로필을 기준으로 렌더링한다.
        const response = await getUserProfile();

        if (isCancelled) {
          return;
        }

        setProfile(response);
        syncProfileIntoAuthStore(response);
      } catch {
        if (isCancelled) {
          return;
        }

        toast.error('프로필 정보를 불러오지 못했어요.');
      } finally {
        if (!isCancelled) {
          setIsLoadingProfile(false);
        }
      }
    })();

    return () => {
      isCancelled = true;
    };
  }, []);

  function handleSavedProfile(nextProfile: UserProfileResponse) {
    setProfile(nextProfile);
    syncProfileIntoAuthStore(nextProfile);
  }

  async function handleDeleteAccount() {
    if (isDeletingAccount) {
      return;
    }

    const shouldDelete = window.confirm(
      '회원탈퇴를 진행할까요?\n탈퇴하면 계정과 현재 로그인 상태가 정리됩니다.'
    );

    if (!shouldDelete) {
      return;
    }

    setIsDeletingAccount(true);

    try {
      // 탈퇴가 끝나면 로컬 세션도 즉시 비워서 stale user 정보가 남지 않게 한다.
      await deleteUserProfile();
      clearAuth();
      toast.success('회원탈퇴가 완료되었어요.');
      navigate('/onboarding', { replace: true });
    } catch {
      toast.error('회원탈퇴를 완료하지 못했어요. 잠시 후 다시 시도해주세요.');
    } finally {
      setIsDeletingAccount(false);
    }
  }

  return (
    <>
      <div className="min-h-[calc(100dvh-5rem)] overflow-y-auto bg-[hsl(var(--surface-subtle))] px-5 pb-10 pt-6">
        <div className="mx-auto flex max-w-[430px] flex-col gap-5">
          <header className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-full border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--primary-2))] shadow-sm">
              <Settings2 size={20} />
            </div>
            <h1 className="text-2xl font-black text-[hsl(var(--text-primary))]">
              설정
            </h1>
          </header>

          <section className="overflow-hidden rounded-[2rem] border border-[hsl(var(--border-default))] bg-[linear-gradient(180deg,hsl(var(--primary-5))_0%,hsl(var(--surface-default))_62%,hsl(var(--surface-subtle))_100%)] p-6 text-[hsl(var(--text-primary))] shadow-[var(--shadow-lg)]">
            <div className="flex items-center justify-between">
              <div className="text-sm font-semibold tracking-[0.08em] text-[hsl(var(--text-secondary))]">
                계정
              </div>
              <div className="text-xs font-semibold text-[hsl(var(--text-secondary))]">
                {providerLabel}
              </div>
            </div>

            <div className="mt-8 flex flex-col items-center text-center">
              <div className="mt-0 space-y-1">
                <h2 className="text-3xl font-black">
                  {profile.nickname ?? 'Runner'}
                </h2>
                <p className="text-xs font-semibold text-[hsl(var(--text-secondary))]">
                  {providerLabel}
                </p>
                <p className="text-sm text-[hsl(var(--text-secondary))]">
                  {birthDateLabel ?? '생년월일 미등록'}
                  {birthDateLabel && genderLabel ? ' · ' : ''}
                  {genderLabel ?? '성별 미등록'}
                </p>
              </div>

              <div className="mt-4 flex flex-wrap items-center justify-center gap-2">
                {profile.height ? (
                  <ProfileMetricChip
                    label="키"
                    value={`${profile.height.toLocaleString('ko-KR')}cm`}
                  />
                ) : null}
                {profile.weight ? (
                  <ProfileMetricChip
                    label="몸무게"
                    value={`${profile.weight.toLocaleString('ko-KR')}kg`}
                  />
                ) : null}
              </div>

            </div>
          </section>

          {isLoadingProfile ? (
            <div className="rounded-[1.5rem] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] px-4 py-6 text-center text-sm text-[hsl(var(--text-secondary))]">
              프로필 정보를 불러오는 중이에요.
            </div>
          ) : null}

          <section className="space-y-3">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsEditOpen(true)}
              className="h-14 w-full rounded-[1.5rem] border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-base font-black text-[hsl(var(--text-primary))] shadow-sm hover:bg-[hsl(var(--surface-subtle))]"
            >
              <PencilLine size={18} />
              프로필 수정
            </Button>

            <Button
              type="button"
              variant="outline"
              onClick={() => void handleLogout()}
              disabled={isLoggingOut}
              className="h-14 w-full rounded-[1.5rem] border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-base font-black text-[hsl(var(--text-primary))] shadow-sm hover:bg-[hsl(var(--surface-subtle))]"
            >
              <LogOut size={18} />
              {isLoggingOut ? '로그아웃 중...' : '로그아웃'}
            </Button>
          </section>

          <section className="space-y-3 rounded-[1.5rem] border border-[hsl(var(--error)/0.16)] bg-[hsl(var(--error)/0.04)] p-4">
            <div className="space-y-1">
              <p className="text-xs font-semibold tracking-[0.08em] text-[hsl(var(--text-error))]">
                위험 구역
              </p>
              <p className="text-sm text-[hsl(var(--text-secondary))]">
                회원탈퇴를 진행하면 현재 로그인 상태와 연결된 소셜 계정 매핑도
                함께 정리됩니다.
              </p>
            </div>

            <Button
              type="button"
              variant="outline"
              onClick={() => void handleDeleteAccount()}
              disabled={isDeletingAccount}
              className={cn(
                'h-14 w-full rounded-[1.5rem] border-[hsl(var(--error)/0.22)] bg-[hsl(var(--surface-default))] text-base font-black text-[hsl(var(--text-error))] shadow-sm hover:bg-[hsl(var(--error)/0.05)]',
                isDeletingAccount ? 'opacity-70' : undefined
              )}
            >
              <ShieldAlert size={18} />
              {isDeletingAccount ? '회원탈퇴 처리 중...' : '회원탈퇴'}
            </Button>
          </section>
        </div>
      </div>

      <ProfileEditDialog
        isOpen={isEditOpen}
        profile={profile}
        onOpenChange={setIsEditOpen}
        onSaved={handleSavedProfile}
      />
    </>
  );
}
