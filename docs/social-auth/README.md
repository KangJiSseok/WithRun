# Social Auth Implementation Checklist

WithRun의 Google + Kakao 로그인 구현을 `web + mobile + was` 기준으로 정리한 최종 체크리스트입니다.
현재 레포 상태를 기준으로, 이미 있는 인증 구조는 최대한 활용하고 모바일 복귀 흐름만 추가로 정리하는 방향으로 맞췄습니다.

## Scope

- 로그인 UI는 `web`에만 만든다.
- `mobile`은 `web` 화면을 WebView로 재사용한다.
- 인증은 `was`가 담당한다.
- 인증 구조는 `OAuth2 -> WAS 검증 -> JWT/refresh 발급 -> /api/auth/me로 상태 확인`으로 간다.
- 웹은 `HttpOnly` 쿠키 기반 refresh token 구조를 사용한다.
- 모바일은 외부 브라우저/Auth Session + 딥링크 복귀 흐름을 사용한다.
<<<<<<< HEAD
- v1에서는 `provider + providerUserId` 기준으로만 기존 계정을 식별한다.
- v1에서는 이메일 기반 중복 계정 차단을 하지 않는다.
- 이메일 기반 중복 방지 로직은 추후 추가한다.
=======
- 같은 이메일의 기존 계정은 자동 링크하지 않는다.
>>>>>>> 2aa0226 (docs(was): 소셜 로그인 구현 체크리스트 최신화)
- 온보딩 v1 입력값은 `nickname`, `birthDate`, `gender`, `height`, `weight`만 받는다.
- `loginCode` / `signupToken` 중간 계층은 도입하지 않는다.

## 현재 상태 요약

- [x] `was`에 [`User`](../../was/src/main/java/kr/withrun/was/domain/user/entity/User.java) 엔티티가 있다.
- [x] `was`에 [`UserAuthAccount`](../../was/src/main/java/kr/withrun/was/domain/user/entity/UserAuthAccount.java) 엔티티가 있다.
<<<<<<< HEAD
- [x] `was`에 [`UserAuthAccountRepository`](../../was/src/main/java/kr/withrun/was/domain/user/repository/UserAuthAccountRepository.java)가 있다.
- [x] [`SecurityConfig`](../../was/src/main/java/kr/withrun/was/global/config/SecurityConfig.java) 에서 `/oauth2/**`, `/login/oauth2/**`, `/api/auth/**` public 경로가 잡혀 있다.
- [x] `application.yml`에 Google OAuth 설정과 `app.auth.frontend-base-url`, `app.auth.mobile-deep-link-base`, `app.auth.allowed-origins`가 이미 있다.
- [x] `User`는 `profileCompleted`와 온보딩 전 생성 메서드를 갖도록 정리되었다.
- [ ] Kakao OAuth 설정은 아직 없다.
- [ ] `web`은 아직 `localStorage` access token + 401 즉시 `/login` 이동 방식이다.
- [ ] `mobile` 브리지에는 아직 `startSocialLogin(url)`이 없다.
- [ ] 실제 OAuth/JWT 실행 클래스는 아직 거의 없어서, Step 2부터 새로 만들어야 한다.

## Step 0. 사전 준비

- [x] Google OAuth 앱의 redirect URI를 확인한다.
  - [x] `http://localhost:8080/login/oauth2/code/google`
  - [ ] 배포 주소
- [x] Kakao Developers에서 Kakao Login을 활성화하고 redirect URI를 등록한다.
  - [x] `http://localhost:8080/login/oauth2/code/kakao`
  - [ ] 배포 주소
  - [x] 동의 항목: `profile_nickname`, `profile_image`
  - [x] `account_email`은 추후 이메일 기반 중복 방지 로직 도입 시 검토한다.
- [x] AWS Parameter Store 값을 준비한다.
  - [x] `google.oauth.client-id`
  - [x] `google.oauth.client-secret`
  - [x] `kakao.oauth.client-id`
  - [x] `kakao.oauth.client-secret`
  - [x] `auth.jwt.access-secret`
  - [x] `app.auth.frontend-base-url`
  - [x] `app.auth.allowed-origins`
- [x] `app.auth.mobile-deep-link-base`는 기본값이 이미 있으므로, Step 9 진입 시 최종값만 확정한다.
- [x] 개발 기준 URL을 확정한다.
  - [x] `web`: `http://localhost:5173`
  - [x] `was`: `http://localhost:8080`
  - [x] `mobile` 딥링크: `withrun://auth/callback`
=======
- [x] [`SecurityConfig`](../../was/src/main/java/kr/withrun/was/global/config/SecurityConfig.java) 에서 `/oauth2/**`, `/login/oauth2/**`, `/api/auth/**` public 경로가 잡혀 있다.
- [x] `application.yml`에 Google OAuth 설정과 `app.auth.frontend-base-url`, `app.auth.mobile-deep-link-base`, `app.auth.allowed-origins`가 이미 있다.
- [ ] Kakao OAuth 설정은 아직 없다.
- [ ] `web`은 아직 `localStorage` access token + 401 즉시 `/login` 이동 방식이다.
- [ ] `mobile` 브리지에는 아직 `startSocialLogin(url)`이 없다.
- [ ] Google/Kakao OAuth, JWT, refresh, `/api/auth/me` 흐름이 현재 엔티티 구조에 완전히 맞게 정리되어 있는지 확인이 필요하다.

## Step 0. 사전 준비

- [ ] Google OAuth 앱의 redirect URI를 확인한다.
  - [ ] `http://localhost:8080/login/oauth2/code/google`
  - [ ] 배포 주소
- [ ] Kakao Developers에서 Kakao Login을 활성화하고 redirect URI를 등록한다.
  - [ ] `http://localhost:8080/login/oauth2/code/kakao`
  - [ ] 배포 주소
  - [ ] 동의 항목: `profile_nickname`, `profile_image`, `account_email`
- [ ] AWS Parameter Store 값을 준비한다.
  - [ ] `google.oauth.client-id`
  - [ ] `google.oauth.client-secret`
  - [ ] `kakao.oauth.client-id`
  - [ ] `kakao.oauth.client-secret`
  - [ ] `auth.jwt.access-secret`
  - [ ] `app.auth.frontend-base-url`
  - [ ] `app.auth.allowed-origins`
- [ ] `app.auth.mobile-deep-link-base`는 기본값이 이미 있으므로, Step 9 진입 시 최종값만 확정한다.
- [ ] 개발 기준 URL을 확정한다.
  - [ ] `web`: `http://localhost:5173`
  - [ ] `was`: `http://localhost:8080`
  - [ ] `mobile` 딥링크: `withrun://auth/callback`
>>>>>>> 2aa0226 (docs(was): 소셜 로그인 구현 체크리스트 최신화)

완료 기준:
Google/Kakao 콘솔과 Parameter Store 값이 준비되어 있고, 로컬/배포 redirect 주소가 확정되어 있다.

## Step 1. WAS 엔티티와 인증 기준 정리

- [ ] 기존 인증 로직이 `User`, `UserAuthAccount` 기준으로 동작하는지 점검한다.
<<<<<<< HEAD
- [x] `UserAuthAccountRepository`를 만들고 `findByProviderAndProviderUserId(provider, providerUserId)`를 추가한다.
- [ ] 필요 시 이메일 중복 확인 메서드를 나중에 추가할 수 있게 구조를 열어둔다.
- [x] `UserRepository`에 닉네임 중복 확인 메서드를 추가한다.
- [ ] 이후 만들 JWT 생성/검증 로직은 처음부터 `User` 기준으로 구현한다.
- [ ] refresh token 저장 구조는 Step 3 이후에 실제 구현 방식을 정한다.
- [x] `User`에 `profileCompleted` 필드를 추가한다.
- [x] `User`의 프로필 필드를 온보딩 전까지 nullable 허용으로 정리한다.
- [x] `User.createPendingSocialUser()`와 `completeProfile(...)`를 추가한다.
- [x] `completeProfile(...)` 입력값 검증을 보강한다.
- [x] `SecurityConfig`의 세션 정책이 `IF_REQUIRED`인지 확인한다.
- [ ] 현재 레포에는 주요 인증 실행 클래스가 아직 거의 없으므로, Step 2부터 새로 만들 클래스들을 `User` 기준으로 구현한다.
  - [ ] `CustomOAuth2UserService`
  - [ ] `OAuth2SuccessHandler`
  - [ ] `OAuth2FailureHandler`
  - [ ] `JwtProvider`
  - [ ] `JwtFilter`
  - [ ] `RefreshTokenStore`
  - [ ] `AuthController` / `AuthService`
=======
- [ ] `UserAuthAccountRepository`를 만든다.
  - [ ] `findByProviderAndProviderUserId(provider, providerUserId)`
  - [ ] 필요 시 이메일 중복 확인 메서드 추가
- [ ] `UserRepository`를 정리한다.
- [ ] JWT 생성/검증 로직이 `User` 기준으로 동작하도록 맞춘다.
- [ ] refresh token 저장 구조를 점검한다.
- [ ] `User`에 온보딩 완료 여부 필드를 추가한다.
  - [ ] `profileCompleted` 또는 `onboardedAt`
- [ ] 현재 `User`의 프로필 필드가 모두 `nullable = false`인지 확인하고, 온보딩 전 사용자 생성 전략을 먼저 정한다.
  - [ ] `nickname`, `birthDate`, `gender`, `height`, `weight`를 온보딩 전까지 nullable로 둘지 결정
  - [ ] 또는 임시 프로필 값을 허용할지 결정
  - [ ] Step 3의 신규 계정 생성은 이 결정 이후에만 진행한다.
- [ ] `SecurityConfig`의 세션 정책이 `IF_REQUIRED`인지 확인한다.
  - [ ] OAuth2 로그인 시 CSRF 방지를 위한 state 값 저장에 세션이 필요하다.
  - [ ] `STATELESS`로 바꾸면 OAuth 로그인이 실패할 수 있다.
- [ ] 기존 인증 코드에서 `Member` 참조가 남아 있는지 확인한다.
  - [ ] `JwtProvider`
  - [ ] `JwtFilter`
  - [ ] `OAuth2SuccessHandler`
  - [ ] `CustomOAuth2UserService`
  - [ ] `SecurityConfig`
  - [ ] `RefreshTokenStore`
  - [ ] 기존 Controller / Service 계층
>>>>>>> 2aa0226 (docs(was): 소셜 로그인 구현 체크리스트 최신화)

완료 기준:
Google 로그인 기준으로도 `User` + `UserAuthAccount` 구조가 일관되게 사용되고, 이후 Kakao를 붙일 수 있는 상태가 된다.

## Step 2. WAS OAuth2 공급자 확장

- [ ] `application.yml`에 Google registration 설정을 점검한다.
- [ ] `application.yml`에 Kakao custom provider 설정을 추가한다.
- [ ] provider별 사용자 정보 파싱 구조를 분리한다.
  - [ ] `OAuth2UserInfo`
  - [ ] `GoogleOAuth2UserInfo`
  - [ ] `KakaoOAuth2UserInfo`
  - [ ] `OAuth2UserInfoFactory`
- [ ] Google은 `sub`, `email`, `picture`를 읽는다.
<<<<<<< HEAD
- [ ] Kakao는 `id`를 필수로 읽는다.
- [ ] Kakao의 `profile_nickname`, `profile_image`는 선택값으로 읽는다.
- [ ] Kakao의 `account_email`은 현재 필수 전제로 두지 않는다.
- [ ] `CustomOAuth2UserService`가 provider별 파서를 사용하도록 정리한다.
- [ ] v1 중복 처리 정책을 구현한다.
  - [ ] 같은 `provider + providerUserId`면 기존 로그인
  - [ ] 다른 provider 계정과의 이메일 중복 차단은 아직 하지 않는다
  - [ ] 이메일 기반 중복 방지는 추후 `account_email` 사용 가능 시 추가한다
=======
- [ ] Kakao는 `id`, `kakao_account.email`, `kakao_account.profile.profile_image_url`를 읽는다.
- [ ] `CustomOAuth2UserService`가 provider별 파서를 사용하도록 정리한다.
- [ ] 이메일 충돌 정책을 구현한다.
  - [ ] 같은 `provider + providerUserId`면 기존 로그인
  - [ ] 다른 provider인데 같은 이메일이면 `already_registered`
>>>>>>> 2aa0226 (docs(was): 소셜 로그인 구현 체크리스트 최신화)

완료 기준:
브라우저에서 Google과 Kakao 로그인 시작이 모두 가능하고, provider별 사용자 정보가 일관되게 파싱된다.

## Step 3. WAS 로그인 성공/실패 처리

- [ ] OAuth 성공 핸들러를 정리한다.
  - [ ] 기존 계정이면 로그인 처리
  - [ ] 신규 계정이면 `User`를 `profileCompleted=false`로 생성한다.
  - [ ] 신규 계정이면 `UserAuthAccount`를 함께 생성한다.
  - [ ] 신규 계정도 기존 계정과 동일하게 토큰을 발급한다.
  - [ ] redirect 시 `isNewMember=true` 쿼리 파라미터를 포함한다.
  - [ ] access token / refresh token 발급
  - [ ] refresh token을 쿠키로 내려준다
- [ ] OAuth 실패 핸들러를 정리한다.
  - [ ] `cancelled`
  - [ ] `access_denied`
<<<<<<< HEAD
=======
  - [ ] `already_registered`
>>>>>>> 2aa0226 (docs(was): 소셜 로그인 구현 체크리스트 최신화)
  - [ ] `server_error`
- [ ] 웹 redirect 흐름을 정리한다.
  - [ ] 성공 시 `/auth/callback`
  - [ ] 실패 시 `/login?error=...`
- [ ] 이 단계에서는 웹 redirect만 구현한다. 모바일 redirect는 Step 9에서 추가한다.

완료 기준:
Google/Kakao 로그인 성공 시 토큰 발급까지 이어지고, 실패 시 프론트가 처리 가능한 에러 형태로 복귀한다.

## Step 4. WAS 온보딩 API 추가

- [ ] `PUT /api/users/me/profile` 엔드포인트를 만든다.
- [ ] access token 인증을 거친 사용자만 호출 가능하게 한다.
- [ ] 요청값을 검증한다.
  - [ ] `nickname`
  - [ ] `birthDate`
  - [ ] `gender`
  - [ ] `height`
  - [ ] `weight`
- [ ] `User` 프로필을 업데이트한다.
- [ ] 완료 시 `profileCompleted = true` 또는 `onboardedAt`을 기록한다.
- [ ] 닉네임 중복 정책을 정리한다.

완료 기준:
신규 로그인 사용자가 온보딩 API를 호출해 프로필을 완성할 수 있다.

## Step 5. WAS 인증 API 표준화

- [ ] 아래 경로를 기준으로 정리한다.
  - [ ] `GET /api/auth/me`
  - [ ] `POST /api/auth/token/reissue`
  - [ ] `POST /api/auth/logout`
  - [ ] `PUT /api/users/me/profile`
- [ ] `/api/auth/me` 응답에 아래 필드를 포함한다.
  - [ ] `userId`
  - [ ] `nickname`
  - [ ] `role`
  - [ ] `profileCompleted`
  - [ ] `provider`
  - [ ] `email`
  - [ ] `profileImageUrl`
- [ ] 기존 `isNewMember` 플래그를 `profileCompleted` 중심 흐름으로 정리한다.
  - [ ] callback redirect 시에는 `isNewMember=true`를 1회성 쿼리 파라미터로만 사용한다.
  - [ ] `/api/auth/me` 응답에서는 `profileCompleted: false`를 지속 상태로 사용한다.
- [ ] 에러 응답 형식을 통일한다.

완료 기준:
프론트가 로그인 상태, 온보딩 상태, 에러 상태를 일관된 응답 형태로 처리할 수 있다.

## Step 6. WEB 로그인 화면과 콜백 페이지

- [ ] `/login` 페이지를 만든다.
- [ ] Google 로그인 버튼을 둔다.
- [ ] Kakao 로그인 버튼을 둔다.
- [ ] 에러 배너를 둔다.
  - [ ] `cancelled`
  - [ ] `access_denied`
<<<<<<< HEAD
=======
  - [ ] `already_registered`
>>>>>>> 2aa0226 (docs(was): 소셜 로그인 구현 체크리스트 최신화)
  - [ ] `server_error`
- [ ] 웹 브라우저에서는 `window.location.assign(...)`으로 OAuth 시작 URL로 이동한다.
- [ ] WebView 안에서는 `startSocialLogin(url)` 브리지를 호출한다.
- [ ] `/auth/callback` 페이지를 만든다.
- [ ] `/auth/callback` 진입 시 URL에 `error` 파라미터가 있으면 `/login?error=...`로 이동한다.
- [ ] `error`가 없으면 `/api/auth/me`를 호출한다.
- [ ] `/api/auth/me` 성공 + `profileCompleted: false`면 `/onboarding`으로 이동한다.
- [ ] `/api/auth/me` 성공 + `profileCompleted: true`면 `/home`으로 이동한다.
- [ ] `/api/auth/me` 실패(401)이면 `/login?error=server_error`로 이동한다.

완료 기준:
웹에서 Google/Kakao 로그인 버튼, 콜백 처리, 홈 또는 온보딩 이동이 모두 연결된다.

## Step 7. WEB 인증 상태 관리

- [ ] 앱 시작 시 `/api/auth/me`로 인증 상태를 복구한다.
- [ ] 인증 안 됨이면 public 페이지로 보낸다.
- [ ] 인증됨 + 온보딩 미완료면 `/onboarding`으로 보낸다.
- [ ] 인증됨 + 온보딩 완료면 정상 사용 가능 상태로 둔다.
- [ ] axios 인터셉터를 정리한다.
  - [ ] 401이면 `/api/auth/token/reissue` 시도
  - [ ] 성공 시 원래 요청 재시도
  - [ ] 실패 시 인증 상태 초기화 후 `/login`
- [ ] 새로고침 후에도 인증 상태가 복구되는지 확인한다.

완료 기준:
로그인, 새로고침, access token 만료, refresh 만료 상황에서 모두 일관되게 동작한다.

## Step 8. WEB 온보딩 화면

- [ ] `/onboarding` 페이지를 만든다.
- [ ] 입력 필드를 둔다.
  - [ ] `nickname`
  - [ ] `birthDate`
  - [ ] `gender`
  - [ ] `height`
  - [ ] `weight`
- [ ] 소셜 프로필 이미지/기본 정보 요약을 보여준다.
- [ ] 제출 시 `PUT /api/users/me/profile` 호출
- [ ] 성공 시 `/home` 이동
- [ ] 실패 시 에러 메시지 표시
- [ ] 이미 온보딩이 끝난 사용자는 `/home`으로 보낸다.

완료 기준:
신규 사용자가 로그인 후 온보딩을 거쳐 홈까지 진입할 수 있다.

## Step 9. MOBILE WebView 연동

- [ ] `mobile` 브리지에 `startSocialLogin(url)`을 추가한다.
- [ ] 외부 브라우저/Auth Session으로 로그인 흐름을 연다.
- [ ] Expo 설정에 `scheme: withrun`을 추가한다.
- [ ] Android/iOS에서 `withrun://auth/callback` 딥링크를 수신하도록 설정한다.
- [ ] 딥링크 수신 후 WebView를 `/auth/callback`으로 이동시킨다.
- [ ] iOS/Android 쿠키 공유를 확인한다.

완료 기준:
모바일 WebView에서 로그인 버튼 -> 외부 브라우저 -> 딥링크 복귀 -> 웹 콜백 처리까지 이어진다.

## Step 10. Refresh Token 저장소 개선

- [ ] 현재 저장소 구조를 점검한다.
- [ ] 안정화 단계에서 영속 저장소 전환을 검토한다.
  - [ ] Redis
  - [ ] DB
- [ ] refresh token hash 저장을 적용한다.
<<<<<<< HEAD
- [ ] v1에서는 refresh token rotation을 도입하지 않는다.
- [ ] 필요 시 안정화 단계에서 rotation 도입을 검토한다.
=======
- [ ] rotation 정책을 도입한다.
>>>>>>> 2aa0226 (docs(was): 소셜 로그인 구현 체크리스트 최신화)

완료 기준:
서버 재시작, 만료 관리, 로그아웃, 재발급 흐름이 안정적으로 관리된다.

## Step 11. 검증과 마무리

- [ ] `was` 검증
  - [ ] Google 로그인
  - [ ] Kakao 로그인
  - [ ] 기존 사용자 로그인
  - [ ] 신규 사용자 생성
<<<<<<< HEAD
=======
  - [ ] 이메일 충돌
>>>>>>> 2aa0226 (docs(was): 소셜 로그인 구현 체크리스트 최신화)
  - [ ] 온보딩 API 유효성 검증
  - [ ] reissue
  - [ ] logout 후 refresh 거부
- [ ] `web` 검증
  - [ ] `npm run lint`
  - [ ] `npm run build`
  - [ ] Google 로그인
  - [ ] Kakao 로그인
  - [ ] 신규 사용자 온보딩
  - [ ] 로그인 취소/거부
  - [ ] 새로고침 후 인증 유지
  - [ ] refresh 만료 후 `/login`
- [ ] `mobile` 검증
  - [ ] Android 로그인
  - [ ] iOS 로그인
  - [ ] 딥링크 복귀
  - [ ] 온보딩 흐름

완료 기준:
웹, 모바일, 백엔드가 같은 사용자 흐름으로 연결되고 실패 케이스까지 확인된다.

## API Contract Summary

- `GET /oauth2/authorization/{provider}`
- `GET /api/auth/me`
- `POST /api/auth/token/reissue`
- `POST /api/auth/logout`
- `PUT /api/users/me/profile`

## Done Definition

- Google/Kakao 모두 웹에서 로그인 가능하다.
- 모바일 WebView에서도 외부 브라우저 + 딥링크 복귀가 된다.
- 기존 계정은 홈으로 바로 진입한다.
- 신규 계정은 온보딩 후 홈으로 진입한다.
<<<<<<< HEAD
- access token 만료 시 refresh로 복구되고, refresh 만료 시 `/login`으로 돌아간다.
- `User` + `UserAuthAccount` 구조와 인증 흐름이 일관되게 맞물린다.
- 이메일 기반 중복 계정 방지는 추후 확장 가능한 구조로 남겨둔다.
=======
- 이메일 충돌 계정은 자동 링크되지 않고 로그인 화면 오류로 돌아간다.
- access token 만료 시 refresh로 복구되고, refresh 만료 시 `/login`으로 돌아간다.
- `User` + `UserAuthAccount` 구조와 인증 흐름이 일관되게 맞물린다.
>>>>>>> 2aa0226 (docs(was): 소셜 로그인 구현 체크리스트 최신화)

## 구현 우선순위

1. Step 1: WAS 엔티티와 인증 기준 정리
2. Step 2: Kakao OAuth 공급자 추가
3. Step 3: 로그인 성공/실패 처리
4. Step 4: 온보딩 API 추가
5. Step 5: 인증 API 응답/경로 표준화
6. Step 6: WEB 로그인/콜백 화면
7. Step 7: WEB 인증 상태 관리
8. Step 8: WEB 온보딩 화면
9. Step 9: MOBILE WebView + 딥링크 연동
10. Step 10: Refresh token 저장소 개선
11. Step 11: 전체 검증
