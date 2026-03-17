# Social Auth Implementation Checklist

WithRun의 Google + Kakao 로그인 구현을 `web + mobile + was` 기준으로 나눈 실행 체크리스트입니다.
구현 순서는 의존성이 적은 순서로 정리했고, 각 단계가 끝나면 바로 검증할 수 있게 마무리 조건도 함께 적었습니다.

## Scope

- 로그인 UI는 `web`에만 만든다.
- `mobile`은 `web` 화면을 WebView로 재사용한다.
- 소셜 로그인 시작/복귀만 네이티브가 보조한다.
- 인증 구조는 `OAuth2 -> loginCode 교환 -> access token + refresh cookie`로 고정한다.
- 같은 이메일의 기존 계정은 자동 링크하지 않는다.
- 온보딩 v1 입력값은 `nickname`, `birthDate`, `gender`, `height`, `weight`만 받는다.

## Step 0. 사전 준비

- [x] Google OAuth 앱에서 redirect URI를 `http://localhost:8080/login/oauth2/code/google`와 배포 주소로 등록한다.
- [x] Kakao Developers에서 Kakao Login을 활성화하고 redirect URI를 `http://localhost:8080/login/oauth2/code/kakao`와 배포 주소로 등록한다.
- [x] AWS Parameter Store에 아래 값을 준비한다.
- [x] `google.oauth.client-id`
- [x] `google.oauth.client-secret`
- [x] `kakao.oauth.client-id`
- [x] `kakao.oauth.client-secret`
- [x] `auth.jwt.access-secret`
- [x] `app.auth.frontend-base-url`
- [x] `app.auth.mobile-deep-link-base`
- [x] `app.auth.allowed-origins`
- [x] 개발 기준 URL을 확정한다.
- [x] `web`: `http://localhost:5173`
- [x] `was`: `http://localhost:8080`
- [x] `mobile` 딥링크: `withrun://auth/callback`

완료 기준:

- Google/Kakao 콘솔과 Parameter Store 값이 준비되어 있고, 로컬/배포 redirect 주소가 확정되어 있다.

## Step 1. WAS OAuth 기반 만들기

- [ ] `SecurityConfig`에서 `/oauth2/**`, `/login/oauth2/**`, `/api/auth/**`를 public으로 유지한다.
- [ ] `AuthProperties`에 프런트 redirect, 모바일 딥링크, cookie/TTL 관련 설정을 정리한다.
- [ ] `application.yml`에 Google registration과 Kakao custom provider 설정을 추가한다.
- [ ] `GET /api/auth/oauth2/{provider}/start?channel=web|mobile` 시작 엔드포인트를 만든다.
- [ ] 시작 엔드포인트에서 `AUTH_CHANNEL`만 세션에 저장하고 Spring Security `/oauth2/authorization/{provider}`로 redirect 한다.
- [ ] provider별 프로필 파싱 클래스를 분리한다.
- [ ] Google은 `sub`, `email`, `picture`를 읽는다.
- [ ] Kakao는 `id`, `kakao_account.email`, `kakao_account.profile.profile_image_url`를 읽는다.
- [ ] 이메일 충돌 정책을 확정한다.
- [ ] `provider + providerUserId`가 없고 같은 이메일의 기존 `UserAuthAccount`가 있으면 `already_registered`로 실패 처리한다.

완료 기준:

- 브라우저에서 `/api/auth/oauth2/google/start?channel=web`과 `/api/auth/oauth2/kakao/start?channel=web` 호출 시 provider 로그인 화면까지 정상 이동한다.

## Step 2. WAS loginCode / token 계층 만들기

- [ ] 일회용 loginCode 저장 모델을 추가한다.
- [ ] 저장 값은 `purpose(LOGIN|SIGNUP)`, `provider`, `providerUserId`, `userId(nullable)`, `email(nullable)`, `profileImageUrl(nullable)`, `channel`, `expiresAt`, `usedAt`다.
- [ ] loginCode 평문은 저장하지 않고 hash만 저장한다.
- [ ] refresh token 저장 모델을 추가한다.
- [ ] refresh token도 hash만 저장하고 `userId`, `expiresAt`, `revokedAt`, `lastRotatedAt`, `channel`, `userAgent`를 관리한다.
- [ ] JWT access token 발급/검증 로직을 추가한다.
- [ ] 권장 기본값은 access 15분, refresh 14일, loginCode 5분이다.
- [ ] OAuth 성공 핸들러를 만든다.
- [ ] 기존 연동 계정이면 `LOGIN` 목적 loginCode를 만든다.
- [ ] 신규 계정이면 `SIGNUP` 목적 loginCode를 만든다.
- [ ] 웹이면 `${frontendBaseUrl}/auth/callback?...`으로 redirect 한다.
- [ ] 모바일이면 `${mobileDeepLinkBase}?loginCode=...` 또는 `...?error=...`로 redirect 한다.
- [ ] OAuth 실패 핸들러를 만든다.
- [ ] 취소/거부/서버 오류를 `cancelled`, `access_denied`, `server_error` 같은 프런트 친화적 쿼리로 변환한다.
- [ ] `POST /api/auth/login-code/exchange`를 만든다.
- [ ] 기존 계정이면 `{ authStatus: 'AUTHENTICATED', accessToken, user }`를 반환하고 refresh cookie를 세팅한다.
- [ ] 신규 계정이면 `{ authStatus: 'PENDING_SIGNUP', signupToken, provider, email?, profileImageUrl? }`를 반환한다.
- [ ] `POST /api/auth/signup/complete`를 만든다.
- [ ] 입력값은 `signupToken`, `nickname`, `birthDate`, `gender`, `height`, `weight`다.
- [ ] 여기서 `User`와 `UserAuthAccount`를 같은 트랜잭션에서 생성하고 access token + refresh cookie를 발급한다.
- [ ] `POST /api/auth/token/refresh`를 만든다.
- [ ] refresh cookie를 검증하고 회전시킨 뒤 새 access token을 반환한다.
- [ ] `GET /api/auth/me`를 만든다.
- [ ] `POST /api/auth/logout`을 만든다.
- [ ] 로그아웃 시 refresh revoke + cookie 삭제를 처리한다.

완료 기준:

- OAuth 성공 후 웹/모바일 모두 callback URL로 돌아오고, loginCode 교환 API까지 응답이 나온다.

## Step 3. WEB 로그인 뷰와 라우팅 만들기

- [ ] `/login` public route를 추가한다.
- [ ] Google/Kakao 버튼 2개와 에러 배너 영역을 만든다.
- [ ] `cancelled`, `access_denied`, `already_registered`, `server_error`, `expired_code`, `signup_session_missing` 문구 매핑을 만든다.
- [ ] 브라우저 웹에서는 버튼이 `window.location.assign(`${VITE_API_BASE_URL}/api/auth/oauth2/{provider}/start?channel=web`)`를 호출하게 한다.
- [ ] WebView 안에서는 같은 버튼이 브리지 `startSocialLogin(url)`을 호출하게 한다.
- [ ] `/auth/callback` public route를 추가한다.
- [ ] `loginCode`가 있으면 즉시 exchange API를 호출한다.
- [ ] `error`가 있으면 `/login?error=...`로 치환 이동한다.
- [ ] 교환 중에는 로딩 전용 화면을 보여준다.
- [ ] 결과가 `AUTHENTICATED`면 access token을 메모리 상태에 저장하고 `/home` 또는 이전 목적지로 이동한다.
- [ ] 결과가 `PENDING_SIGNUP`이면 `signupToken`을 `sessionStorage`에 저장하고 `/onboarding`으로 이동한다.
- [ ] `/onboarding` public route를 추가한다.
- [ ] 입력 필드는 `nickname`, `birthDate`, `gender`, `height`, `weight`로 고정한다.
- [ ] 프로필 이미지는 상단 요약 카드에만 보여준다.
- [ ] 제출 성공 시 access token 저장 후 `/home`으로 이동한다.
- [ ] `signupToken`이 없거나 만료되면 `/login?error=signup_session_missing`으로 보낸다.

완료 기준:

- 웹 브라우저에서 로그인, callback, 온보딩, 홈 진입까지 수동 시나리오가 모두 연결된다.

## Step 4. WEB 인증 상태 복구 정리하기

- [ ] 메모리 기반 auth 상태 저장소를 만든다.
- [ ] 상태는 `anonymous`, `exchanging`, `pending-signup`, `authenticated` 정도로 단순화한다.
- [ ] access token은 메모리에만 저장한다.
- [ ] `signupToken`만 `sessionStorage`에 잠시 저장한다.
- [ ] 앱 시작 시 access token이 없으면 `refresh`를 1회 시도한다.
- [ ] refresh 성공이면 현재 route를 유지한다.
- [ ] refresh 실패면 `/login`으로 이동한다.
- [ ] `axiosClient`에 `withCredentials: true`를 켠다.
- [ ] 401 응답 시 refresh 1회 재시도 후 실패하면 auth 상태를 초기화하고 `/login`으로 이동한다.
- [ ] 기존 `window.location.href = '/login'` 단일 처리 코드를 제거한다.

완료 기준:

- 새로고침 후에도 refresh cookie가 살아 있으면 로그인 상태가 복구되고, 만료 시에는 `/login`으로 안전하게 돌아간다.

## Step 5. MOBILE WebView 연동하기

- [ ] `mobile`에 `startSocialLogin(url)` 브리지 메서드를 추가한다.
- [ ] 네이티브는 WebView 내부 이동 대신 외부 브라우저 auth session을 연다.
- [ ] Expo 설정에 `scheme: withrun`을 추가한다.
- [ ] Android/iOS에서 `withrun://auth/callback` 딥링크를 수신하도록 설정한다.
- [ ] 딥링크 수신 시 현재 WebView를 `web`의 `/auth/callback?loginCode=...` 또는 `/auth/callback?error=...`로 이동시킨다.
- [ ] 별도 모바일 웹 화면은 만들지 않는다.
- [ ] 로그인 이후 화면 전환은 브라우저 웹과 동일한 `web` 코드에 맡긴다.

완료 기준:

- 모바일 WebView에서 로그인 버튼을 누르면 외부 브라우저가 열리고, 딥링크 복귀 후 `web` callback 화면이 정상 동작한다.

## Step 6. 검증과 마무리

- [ ] `was` 테스트를 추가한다.
- [ ] provider mapping
- [ ] 기존 계정 로그인
- [ ] 신규 계정 signup 분기
- [ ] 이메일 충돌
- [ ] loginCode 만료/재사용/위조
- [ ] refresh rotation
- [ ] logout 후 refresh 거부
- [ ] `web`은 `npm run lint`와 `npm run build`를 통과시킨다.
- [ ] `mobile`은 Android/iOS 수동 확인을 한다.
- [ ] Google 로그인 성공
- [ ] Kakao 로그인 성공
- [ ] 신규 사용자 온보딩
- [ ] 이미 가입된 이메일 오류
- [ ] 로그인 취소
- [ ] refresh 만료 후 `/login` 복귀

완료 기준:

- 웹, 모바일, 백엔드 모두 동일한 사용자 흐름으로 동작하고, 실패 케이스까지 눈으로 확인된다.

## API Contract Summary

- `GET /api/auth/oauth2/{provider}/start?channel=web|mobile`
- `POST /api/auth/login-code/exchange`
- `POST /api/auth/signup/complete`
- `POST /api/auth/token/refresh`
- `GET /api/auth/me`
- `POST /api/auth/logout`

## Done Definition

- Google/Kakao 모두 웹에서 로그인 가능하다.
- 모바일 WebView에서도 외부 브라우저 + 딥링크 복귀가 된다.
- 기존 연동 계정은 홈으로 바로 들어간다.
- 신규 계정은 온보딩 후 홈으로 들어간다.
- 이메일 충돌 계정은 자동 링크되지 않고 로그인 화면 오류로 돌아간다.
- access token 만료 시 refresh로 복구되고, refresh 만료 시 `/login`으로 돌아간다.
