import {
  Camera,
  ChevronLeft,
  LogOut,
  PencilLine,
  ShieldAlert,
  UserRound,
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

function resolveInitial(nickname: string | null): string {
  return nickname?.trim().charAt(0).toUpperCase() || 'W';
}

function formatProvider(provider: string | null | undefined): string {
  if (provider === 'kakao') {
    return '카카오 계정';
  }

  if (provider === 'google') {
    return 'Google 계정';
  }

  return 'With Run 계정';
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
    <div className="rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm font-semibold text-white/85">
      <span className="text-white/50">{label}</span> {value}
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

  function handleBack() {
    if (window.history.length > 1) {
      navigate(-1);
      return;
    }

    navigate('/home');
  }

  return (
    <>
      <div className="min-h-[calc(100dvh-5rem)] overflow-y-auto bg-[hsl(var(--surface-subtle))] px-5 pb-10 pt-6">
        <div className="mx-auto flex max-w-[430px] flex-col gap-5">
          <header className="flex items-center gap-3">
            <button
              type="button"
              onClick={handleBack}
              className="flex h-12 w-12 items-center justify-center rounded-full border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))] shadow-sm"
              aria-label="이전으로"
            >
              <ChevronLeft size={20} />
            </button>
            <div>
              <p className="text-sm font-semibold text-[hsl(var(--text-secondary))]">
                내 프로필
              </p>
              <h1 className="text-2xl font-black text-[hsl(var(--text-primary))]">
                프로필 관리
              </h1>
            </div>
          </header>

          <section className="overflow-hidden rounded-[2rem] bg-[linear-gradient(180deg,#3D2020_0%,#1A1E26_58%,#202938_100%)] p-6 text-white shadow-[0_24px_60px_rgba(0,0,0,0.18)]">
            <div className="flex items-center justify-between">
              <div className="text-lg font-black">프로필</div>
              <div className="text-sm font-medium text-white/45">
                {providerLabel}
              </div>
            </div>

            <div className="mt-8 flex flex-col items-center text-center">
              <div className="relative flex h-24 w-24 items-center justify-center rounded-full bg-[radial-gradient(circle_at_top,#6D4A2D,#2B231F)] shadow-[0_16px_36px_rgba(0,0,0,0.24)]">
                <span className="text-3xl font-black text-white/95">
                  {resolveInitial(profile.nickname)}
                </span>
                <div className="absolute bottom-0 right-0 flex h-8 w-8 items-center justify-center rounded-full border border-white/10 bg-white/10 text-white/90 backdrop-blur">
                  <Camera size={14} />
                </div>
              </div>

              <div className="mt-5 space-y-1">
                <h2 className="text-3xl font-black">
                  {profile.nickname ?? 'Runner'}
                </h2>
                <p className="text-sm text-white/60">
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

              <button
                type="button"
                onClick={() => setIsEditOpen(true)}
                className="mt-6 inline-flex items-center gap-2 text-sm font-black text-[#FF6A6A]"
              >
                <PencilLine size={16} />
                프로필 수정
              </button>
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
              onClick={() => void handleLogout()}
              disabled={isLoggingOut}
              className="h-14 w-full rounded-[1.5rem] border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-base font-black text-[hsl(var(--text-primary))] shadow-sm hover:bg-[hsl(var(--surface-subtle))]"
            >
              <LogOut size={18} />
              {isLoggingOut ? '로그아웃 중...' : '로그아웃'}
            </Button>

            <Button
              type="button"
              variant="outline"
              onClick={() => void handleDeleteAccount()}
              disabled={isDeletingAccount}
              className={cn(
                'h-14 w-full rounded-[1.5rem] border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-base font-black text-[hsl(var(--primary-1))] shadow-sm hover:bg-[hsl(var(--surface-subtle))]',
                isDeletingAccount ? 'opacity-70' : undefined
              )}
            >
              <ShieldAlert size={18} />
              {isDeletingAccount ? '회원탈퇴 처리 중...' : '회원탈퇴'}
            </Button>
          </section>

          <div className="flex items-start gap-3 rounded-[1.5rem] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] px-4 py-4 text-sm text-[hsl(var(--text-secondary))]">
            <UserRound
              size={18}
              className="mt-0.5 shrink-0 text-[hsl(var(--primary-1))]"
            />
            <p>
              프로필 이미지는 다음 단계에서 백엔드 응답에 맞춰 연결할 수 있도록
              구조만 열어뒀어요. 회원탈퇴를 진행하면 현재 로그인 상태와 연결된
              소셜 계정 매핑도 함께 정리됩니다.
            </p>
          </div>
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
