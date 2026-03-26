import { getCurrentUser, reissueAccessToken, type AuthUser } from '@/apis/auth';

export type BootstrapAuthSessionResult = {
  accessToken: string;
  user: AuthUser;
};

export async function bootstrapAuthSession(
  fallbackAccessToken?: string | null,
): Promise<BootstrapAuthSessionResult> {
  try {
    // refresh cookie 가 살아 있으면 access token 을 새로 받고, 그 토큰으로 현재 사용자를 복구한다.
    const reissueResponse = await reissueAccessToken();
    const user = await getCurrentUser(reissueResponse.accessToken);

    return {
      accessToken: reissueResponse.accessToken,
      user,
    };
  } catch (reissueError) {
    if (!fallbackAccessToken) {
      throw reissueError;
    }

    // 이미 저장된 access token 이 아직 유효한 경우에는 쿠키 재발급 실패여도 사용자 복구를 한 번 더 시도한다.
    const user = await getCurrentUser(fallbackAccessToken);

    return {
      accessToken: fallbackAccessToken,
      user,
    };
  }
}
