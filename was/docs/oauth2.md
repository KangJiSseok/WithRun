# Google OAuth 2.0 Plan

## 목적

이 문서는 WithRun `was` 프로젝트에서 Google OAuth 2.0 로그인을 구현할 때 참고할 설계 메모다.
현재 논의된 방향과 전제 조건, 구현 순서, 주의사항을 한곳에 정리한다.

## 현재 전제

- 소셜 로그인은 Google 계정을 이용해 사용자 신원을 확인하는 용도로 사용한다.
- 실제 서비스 프로필은 Google이 아니라 온보딩에서 입력받은 값을 사용한다.
- `User` 엔티티는 `nickname`, `birthDate`, `gender`, `height`, `weight`가 모두 필수다.
- `UserAuthAccount` 엔티티는 소셜 계정 연동 정보를 저장하는 용도로 이미 분리되어 있다.
- 현재 `auth` 패키지에는 `PendingSignupSessionData`, `AuthChannel`, `AuthStatus`, `AuthSessionKeys`가 준비되어 있다.
- `AuthProperties`는 프론트엔드 리다이렉트 주소, 모바일 딥링크, 허용 origin 같은 인증 설정 바인딩에 사용한다.

## 핵심 결정

### 1. Google 정보는 로그인 식별에만 사용한다

Google에서 받은 정보를 그대로 `User`에 채우지 않는다.
Google 로그인은 "누가 들어왔는지 확인"하는 역할만 맡고, 실제 회원 프로필은 온보딩 단계에서 생성한다.

### 2. 회원가입 진행 중 상태가 필요하다

Google 로그인 직후에는 `providerUserId`는 확보되지만 `User`를 만들기 위한 필수 프로필 값이 아직 없다.
그래서 아래 3단계 상태가 필요하다.

- `ANONYMOUS`: 아직 로그인 전
- `PENDING_SIGNUP`: Google 로그인 완료, 서비스 회원가입 미완료
- `AUTHENTICATED`: 온보딩 완료, 서비스 회원 생성 완료

### 3. PendingSignupSessionData는 최소 정보만 들고 간다

현재 합의된 최소 구성은 아래와 같다.

```java
public record PendingSignupSessionData(
        String provider,
        String providerUserId
) implements Serializable {
}
```

- `provider`: 추후 Kakao까지 함께 지원할 수 있도록 유지
- `providerUserId`: 소셜 계정의 실제 식별자이므로 필수
- `email`, `profileImageUrl`, `displayName`: 현재 요구사항 기준 필수 아님

### 4. Google의 고정 식별자는 email이 아니라 sub를 사용한다

- `provider`: `google`
- `providerUserId`: Google OIDC `sub`
- `email`: 선택 정보
- `picture`: 선택 정보

이 프로젝트에서는 `email`을 사용자 고유 키로 사용하지 않는다.

### 5. Google userinfo를 수동 호출하지 않는다

Spring Security OAuth2 Login/OIDC가 가져온 사용자 속성에서 필요한 값을 꺼내 쓴다.
현재 설계 기준으로는 Google API를 별도로 다시 호출할 필요가 없다.

## 권장 아키텍처

프론트엔드가 Google SDK를 직접 다루는 방식보다, 백엔드가 인증 코드를 처리하는 서버 주도형 흐름을 사용한다.

1. 웹 또는 모바일이 백엔드 엔트리포인트로 이동한다.
2. 백엔드가 Google 로그인 화면으로 리다이렉트한다.
3. Google이 인증 후 백엔드 콜백으로 돌아온다.
4. Spring Security가 인증 코드를 토큰으로 교환하고 사용자 속성을 읽는다.
5. 백엔드가 `provider`와 `providerUserId`를 기준으로 기존 연동 계정을 찾는다.
6. 계정이 있으면 인증 완료 처리한다.
7. 계정이 없으면 세션에 pending signup 정보를 저장하고 온보딩으로 보낸다.
8. 온보딩 완료 시 `User`와 `UserAuthAccount`를 함께 생성한다.

## 파이프라인

```text
Web/Mobile
  -> GET /oauth2/authorization/google
      -> Google 로그인
          -> GET /login/oauth2/code/google
              -> Spring Security OAuth2 처리
                  -> Google sub 추출
                      -> user_auth_accounts 조회
                          -> 기존 계정 있음: AUTHENTICATED
                          -> 기존 계정 없음: PENDING_SIGNUP 세션 저장
                              -> 온보딩 입력
                                  -> User + UserAuthAccount 생성
                                      -> AUTHENTICATED
```

## 현재 코드 기준 역할 정리

| 구성 요소 | 역할 |
| --- | --- |
| `User` | 서비스 프로필 본체 |
| `UserAuthAccount` | 소셜 계정 연동 정보 저장 |
| `PendingSignupSessionData` | 회원가입 완료 전까지 세션에 들고 갈 최소 식별 정보 |
| `AuthChannel` | 로그인 시작 채널 구분 (`WEB`, `MOBILE`) |
| `AuthStatus` | 인증 상태 구분 (`ANONYMOUS`, `PENDING_SIGNUP`, `AUTHENTICATED`) |
| `AuthSessionKeys` | 세션 attribute key 상수 |
| `AuthProperties` | 인증 관련 설정 바인딩 |

## 구현 대상

### 1. Security 설정

- `SecurityFilterChain` 추가
- `/oauth2/**`, `/login/oauth2/**`, `/api/auth/**`는 인증 없이 허용
- 나머지 보호 API는 점진적으로 인증 적용
- 지금 단계에서는 기본 보안 필터가 테스트를 깨지 않도록 명시 설정 필요

### 2. OAuth2 로그인 성공 처리

- Google 속성에서 `sub` 추출
- `provider = google`, `providerUserId = sub`로 계정 조회
- 기존 계정이면 세션 로그인 완료
- 신규 계정이면 `PendingSignupSessionData` 저장 후 온보딩으로 이동

### 3. 온보딩 완료 처리

- 세션의 `PENDING_SIGNUP` 데이터 확인
- 사용자가 입력한 온보딩 값으로 `User` 생성
- 같은 트랜잭션에서 `UserAuthAccount` 생성
- 세션 상태를 `AUTHENTICATED`로 전환

### 4. 로그아웃 및 세션 조회

- 세션 무효화 로그아웃 API
- 현재 로그인 상태를 확인할 수 있는 세션 조회 API

## 권장 엔드포인트 초안

- `GET /oauth2/authorization/google`
  Google 로그인 시작
- `GET /login/oauth2/code/google`
  Spring Security 기본 콜백
- `POST /api/auth/signup/complete`
  pending signup 상태에서 온보딩 완료 처리
- `GET /api/auth/session`
  현재 인증 상태 조회
- `POST /api/auth/logout`
  세션 로그아웃

## 설정값

### Spring OAuth 설정

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${google.oauth.client-id}
            client-secret: ${google.oauth.client-secret}
            scope:
              - openid
              - profile
              - email
```

### 앱 설정

```yaml
app:
  auth:
    frontend-base-url: ${app.auth.frontend-base-url:http://localhost:5173}
    mobile-deep-link-base: ${app.auth.mobile-deep-link-base:withrun://auth/callback}
    allowed-origins:
      - http://localhost:5173
```

### 시크릿 저장 위치

- `google.oauth.client-id`
- `google.oauth.client-secret`

실제 값은 저장소에 커밋하지 않고 AWS Parameter Store에서 주입한다.

## 테스트 및 CI 주의사항

Spring Security 의존성만 먼저 추가하면 현재 테스트가 깨지는 것이 자연스럽다.
특히 아래 두 가지를 같이 처리해야 한다.

### 1. OAuth placeholder 보완

테스트 컨텍스트에서 아래 값이 없으면 애플리케이션 로딩이 실패할 수 있다.

- `google.oauth.client-id`
- `google.oauth.client-secret`

테스트용 더미 값을 `src/test/resources/application.yml`에 넣거나, 테스트에서 관련 설정을 오버라이드해야 한다.

### 2. WebMvcTest 보안 영향 대응

`spring-boot-starter-security` 추가 후에는 기본 보안 필터 때문에 기존 `@WebMvcTest`가 `401` 또는 `403`으로 바뀔 수 있다.

대응 방법 예시:

- 테스트 전용 `SecurityFilterChain` 추가
- 필요한 엔드포인트 permitAll 설정
- 컨트롤러 테스트에서 필터 비활성화 또는 security test 지원 추가

## 구현 순서 제안

1. `SecurityFilterChain`과 테스트 보정부터 추가한다.
2. Google 로그인 성공/실패 핸들러를 붙인다.
3. pending signup 세션 저장 흐름을 만든다.
4. 온보딩 완료 API에서 `User`와 `UserAuthAccount`를 생성한다.
5. 세션 조회/로그아웃 API를 마무리한다.
6. 이후 Kakao를 같은 구조로 확장한다.

## 메모

- 현재 설계에서는 Google 프로필 정보를 서비스 프로필 소스로 사용하지 않는다.
- `PendingSignupSessionData`는 "구글 상세 프로필 저장"이 아니라 "온보딩 전 임시 인증 상태 저장"을 위한 구조다.
- 중간 구현 커밋에서 CI가 깨질 수 있으므로, 의존성 추가와 보안 설정/테스트 보정은 가능한 한 같은 작업 단위로 묶는 것이 좋다.
