// 라우터와 전역 스토어가 모두 같은 인증 상태 머신을 보도록 문자열 리터럴을 고정한다.
export type AuthStatus =
  | 'idle'
  | 'bootstrapping'
  | 'authenticated'
  | 'unauthenticated';

export type AuthGender = 'MALE' | 'FEMALE';

export type AuthUser = {
  userId: number;
  provider: string;
  profileCompleted: boolean;
  nickname: string | null;
  birthDate: string | null;
  gender: AuthGender | null;
  height: number | null;
  weight: number | null;
};

export type ReissueAccessTokenResponse = {
  accessToken: string;
  provider: string;
  profileCompleted: boolean;
};
