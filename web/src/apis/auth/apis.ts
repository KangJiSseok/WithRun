import type { ApiResponse } from '@/shared/types/api.type';

import { buildServerUrl } from '@/shared/config/api';
import { authApiClient } from '@/shared/libs/axiosClient';
import type {
  AuthUser,
  ExchangeAuthCodeRequest,
  ReissueAccessTokenResponse,
} from './types';

export type SocialProvider = 'kakao' | 'google';

export async function reissueAccessToken(): Promise<ReissueAccessTokenResponse> {
  const { data } = await authApiClient.post<
    ApiResponse<ReissueAccessTokenResponse>
  >('/auth/reissue');

  return data.data;
}

export async function exchangeAuthCode(
  request: ExchangeAuthCodeRequest,
): Promise<ReissueAccessTokenResponse> {
  const { data } = await authApiClient.post<
    ApiResponse<ReissueAccessTokenResponse>
  >('/auth/exchange', request);

  return data.data;
}

export async function getCurrentUser(
  accessToken: string,
): Promise<AuthUser> {
  // /auth/me 는 bootstrap 중 "이 토큰이 실제로 누구인지"를 확정하는 마지막 단계다.
  const { data } = await authApiClient.get<ApiResponse<AuthUser>>('/auth/me', {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  return data.data;
}

export async function logout(accessToken: string | null): Promise<void> {
  await authApiClient.post<ApiResponse<void>>(
    '/auth/logout',
    undefined,
    accessToken
      ? {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      : undefined,
  );
}

export function buildSocialLoginUrl(provider: SocialProvider): string {
  // 웹과 모바일이 모두 같은 백엔드 OAuth 진입점 규칙을 공유한다.
  return buildServerUrl(`/oauth2/authorization/${provider}`);
}
