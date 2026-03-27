# 웹 영향 API 분석 문서

## 목적

이번 문서는 `was`의 인증/`userId` 제거 리팩터 이후,

1. 현재 프론트에서 반드시 재검증해야 하는 API가 무엇인지
2. 실제로 계약이 바뀐 API가 무엇인지
3. `/web`에서 어떤 파일을 수정해야 하고 어떤 화면을 다시 검증해야 하는지

를 한글로 정리한 분석 문서입니다.

기준 변경은 주로 다음 두 커밋입니다.

- `985049e` `fix(was) : 로그인 예외를 제외한 API를 토큰 인증 기반으로 통일`
- `d3ad36f` `test(was) : 토큰 인증 전환과 userId 제거에 대한 컨트롤러 및 DTO 테스트 추가`

---

## 1. 핵심 변경 요약

### 1-1. 인증 정책 변경

기준 파일: `was/src/main/java/kr/withrun/was/global/config/SecurityConfig.java`

- 기존: 일부 `/me` API만 인증 강제, 나머지 다수 API는 사실상 공개 접근 가능
- 현재: 로그인 부트스트랩용 예외를 제외한 대부분의 애플리케이션 API가 `authenticated()` 대상
- 예외 유지:
  - `POST /api/auth/reissue`
  - `POST /api/auth/exchange`
  - `/oauth2/**`, `/login/oauth2/**`, `/mobile/oauth2/**`, `/web/oauth2/**`
  - `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`

### 1-2. 현재 사용자 식별 방식 변경

- 기존: path/query/body 로 `userId`를 전달하는 API가 여러 개 있었음
- 현재: 서버가 `@AuthenticationPrincipal AuthenticatedUser`를 통해 현재 사용자를 해석
- 결과적으로 `/web`에서 남아 있는 `MOCK_USER_ID`, `userId` body/query/path 사용은 대부분 제거 대상

---

## 2. 현재 검증이 필요한 백엔드 API 목록

아래는 프론트 관점에서 우선순위가 높은 순서대로 정리한 목록입니다.

### 2-1. 즉시 깨지는 계약 변경 API

#### 캘린더

기준 컨트롤러: `was/src/main/java/kr/withrun/was/domain/user/controller/UserCalendarController.java`

| 메서드 | 기존 | 현재 | 검증 이유 |
|---|---|---|---|
| `GET` | `/api/calendars/{userId}` | `/api/calendars/me` | 웹이 아직 `MOCK_USER_ID` 기반 경로를 호출 중이라 즉시 404 가능 |
| `GET` | `/api/calendars/{userId}/summary` | `/api/calendars/me/summary` | 요약 조회 경로 변경 |
| `GET` | `/api/calendars/{userId}/weekly` | `/api/calendars/me/weekly` | 주간 요약 경로 변경 |

추가 확인 사항:

- 현재 `web/src`에는 `/api/calendars/me/weekly`를 호출하는 active consumer가 없습니다.
- 대신 홈 요약은 아직 fixture 기반으로 렌더링됩니다.
  - `web/src/features/Home/HomeSummaryStats.ts`
  - `web/src/features/Home/HomeScreen.tsx`
- 따라서 이 API는 “경로 변경 + 아직 웹 미연결” 항목으로 분류하는 것이 맞습니다.

#### 러닝 세션

기준 컨트롤러: `was/src/main/java/kr/withrun/was/domain/running/controller/RunningSessionController.java`

| 메서드 | 기존 | 현재 | 검증 이유 |
|---|---|---|---|
| `POST` | `/api/running-sessions` + body `userId` 포함 | `/api/running-sessions` + body `userId` 제거 | 웹 create payload 타입과 실제 호출이 아직 `userId`를 포함 |
| `PATCH` | `/api/running-sessions/{id}` + body `userId`, split `userId` 포함 | `/api/running-sessions/{id}` + body/split `userId` 제거 | 웹 complete payload와 split 생성 로직 수정 필요 |
| `GET` | `/api/running-sessions/history/past?userId=...` | `/api/running-sessions/history/past` | query `userId` 제거 |
| `GET` | `/api/running-sessions/{id}/detail/{userId}` | `/api/running-sessions/{id}/detail` | 상세 조회 path 변경 |
| `POST` | `/api/running-sessions/{id}/register-course` | 경로 동일, 인증/소유권 강화 | path는 같지만 로그인/소유자 기준 검증 필요 |

추가 확인 사항:

- 웹 언마운트 cleanup payload는 아직 `isCompleted: false`를 보내고 있습니다.
- 하지만 현재 서버는 종료 처리를 사실상 completed 기준으로 확정하는 흐름이라, 이 값은 서버 계약과 의미가 어긋날 가능성이 큽니다.
- 따라서 `PATCH /api/running-sessions/{id}`는 단순한 `userId` 제거뿐 아니라, 웹의 비정상 종료 payload 의미까지 함께 재검토해야 합니다.

#### 코스 / 고스트 / 상호작용

기준 컨트롤러:

- `was/src/main/java/kr/withrun/was/domain/course/controller/CourseController.java`
- `was/src/main/java/kr/withrun/was/domain/course/controller/CourseLikeController.java`
- `was/src/main/java/kr/withrun/was/domain/course/controller/CourseBookmarkController.java`
- `was/src/main/java/kr/withrun/was/domain/course/controller/CourseReviewController.java`

| 메서드 | 기존 | 현재 | 검증 이유 |
|---|---|---|---|
| `GET` | `/api/courses/{courseId}/ghost-detail/{userId}` | `/api/courses/{courseId}/ghost-detail` | 웹이 아직 구형 path 사용 |
| `POST` | `/api/courses/{courseId}/like/{userId}` | `/api/courses/{courseId}/like` | 웹 wrapper가 구형 path 사용 |
| `DELETE` | `/api/courses/{courseId}/like/{userId}` | `/api/courses/{courseId}/like` | 동일 |
| `POST` | `/api/courses/{courseId}/bookmark/{userId}` | `/api/courses/{courseId}/bookmark` | 활성 웹 호출은 없지만 향후 연결 시 즉시 깨짐 |
| `DELETE` | `/api/courses/{courseId}/bookmark/{userId}` | `/api/courses/{courseId}/bookmark` | 동일 |
| `GET` | `/api/courses/bookmarks/{userId}` | `/api/courses/bookmarks` | 활성 웹 호출은 없지만 계약 변경 완료 |
| `POST` | `/api/courses/{courseId}/reviews` + body `userId` 포함 | `/api/courses/{courseId}/reviews` + body `userId` 제거 | 현재 활성 웹 호출은 없지만 계약 변경 완료 |

### 2-2. 경로는 유지되지만 인증 기준이 바뀌어 재검증이 필요한 API

| 메서드 | 경로 | 기준 파일 | 검증 이유 |
|---|---|---|---|
| `POST` | `/api/auth/logout` | `was/src/main/java/kr/withrun/was/domain/auth/controller/AuthController.java` | 이제 access token 전제. 토큰 누락/만료 상태에서 웹 logout UX 확인 필요 |
| `GET` | `/api/users/nickname-availability` | `was/src/main/java/kr/withrun/was/domain/user/controller/UserProfileController.java` | 컨트롤러 설명은 공개처럼 보이지만 보안 설정상 인증 필요 |
| `GET` | `/api/users/me/running-preferences` | `was/src/main/java/kr/withrun/was/domain/user/controller/UserRunningPreferenceController.java` | path는 동일하지만 임시 `/{userId}` 라우트 제거됨 |
| `PUT` | `/api/users/me/running-preferences` | same | 저장 후 재조회 흐름 재검증 필요 |
| `GET` | `/api/courses/{courseId}` | `was/src/main/java/kr/withrun/was/domain/course/controller/CourseController.java` | 과거에는 공개처럼 사용되었으나 현재 인증 흐름에 의존 |
| `GET` | `/api/courses/nearby` | same | now auth required |
| `GET` | `/api/courses/nearby/recommended` | same | now auth required |
| `GET` | `/api/courses/nearby/ghost` | same | now auth required |
| `GET` | `/api/courses/{courseId}/ghost-leaderboard` | same | now auth required |
| `GET` | `/api/ghost-runnings/{runningSessionId}/result` | `was/src/main/java/kr/withrun/was/domain/running/controller/GhostRunningResultController.java` | 결과 페이지 deep link/새로고침 시 인증 복구 확인 필요 |
| `GET` | `/api/meta/survey` | `was/src/main/java/kr/withrun/was/domain/meta/CourseMetaController.java` | now auth required |
| `GET` | `/api/meta/course/filter/normal-run` | same | now auth required |
| `GET` | `/api/meta/course/register` | same | now auth required |

문서/테스트 불일치 추가 메모:

- `logout`은 컨트롤러 문서상 쿠키 만료 중심으로 읽히지만 실제 테스트는 인증 없는 요청을 거절합니다.
- `nickname-availability`도 컨트롤러 설명은 공개 API처럼 적혀 있지만 실제 테스트는 인증 없는 요청을 거절합니다.
- 즉, 프론트 문서에는 “Swagger 설명보다 테스트/실제 보안 정책을 신뢰해야 함”을 명시하는 것이 안전합니다.

---

## 3. `/web`에서 실제 수정이 필요한 파일

이 섹션은 “실제 코드 수정 가능성이 높은 파일”만 따로 뽑았습니다.

### 3-1. 최우선 수정 파일

#### 캘린더 API

- `web/src/apis/userCalendar/apis.ts`
  - 아직 `/calendars/${MOCK_USER_ID}`
  - 아직 `/calendars/${MOCK_USER_ID}/summary`
  - `/me`, `/me/summary`, 필요 시 `/me/weekly` 기준으로 수정 필요

- `web/src/apis/userCalendar/types.ts`
  - 주석이 아직 `{userId}` 기준

#### 러닝 세션 API / 타입

- `web/src/apis/runningSessions/apis.ts`
  - `history/past` 호출에 `userId: MOCK_USER_ID` 삽입 중
  - `detail/${MOCK_USER_ID}` 호출 중

- `web/src/apis/runningSessions/types.ts`
  - `CreateRunningSessionRequest.userId`
  - `CompleteRunningSessionRequest.userId`
  - `SplitRecord.userId`
  - 백엔드 계약과 불일치
  - `CreateRunningSessionResponse`는 서버가 내려주는 `navigationBundleUrl`을 아직 반영하지 않음

- `web/src/shared/hooks/Running/useRunningSplits.ts`
  - split 생성 시 `MOCK_USER_ID` 삽입 중

#### 코스 / 고스트 API

- `web/src/apis/course/apis.ts`
  - `getGhostCourseDetailApi(courseId, userId)`가 아직 `/ghost-detail/{userId}` 호출

- `web/src/apis/course/types.ts`
  - ghost detail 주석이 아직 `/ghost-detail/{userId}` 기준

- `web/src/apis/courseLike/apis.ts`
  - 아직 `/like/{userId}` 호출
  - 현재 활성 consumer 는 없지만 wrapper 자체는 stale 상태

### 3-2. create/complete payload를 직접 만드는 화면 파일

- `web/src/features/FreeRun/FreeRunScreen.tsx`
  - create body에 `userId: MOCK_USER_ID`

- `web/src/features/FreeRun/FreeRunningScreen.tsx`
  - complete body에 `userId: MOCK_USER_ID`

- `web/src/features/NormalRun/NormalRunDetailScreen.tsx`
  - create body에 `userId: MOCK_USER_ID`

- `web/src/features/NormalRun/NormalRunSessionScreen.tsx`
  - complete body에 `userId: MOCK_USER_ID`

- `web/src/features/GhostRun/GhostRunDetailScreen.tsx`
  - create body에 `userId: MOCK_USER_ID`
  - ghost 세션 생성 후 `useNormalRunSessionStore`와 `/normal-run-session/${courseId}`로 이동하는 구조라 별도 검증 필요

- `web/src/features/GhostRun/GhostRunSessionScreen.tsx`
  - complete body에 `userId: MOCK_USER_ID`

### 3-3. hook 레벨에서 stale 계약을 쓰는 파일

- `web/src/hooks/Calendar/useCalendar.ts`
  - summary/monthly/history API 모두 의존

- `web/src/hooks/Calendar/useCalendarDetail.ts`
  - running session detail API 의존

- `web/src/hooks/GhostRun/useGhostRunCourseDetail.ts`
  - `MOCK_USER_ID` 기반 ghost-detail 호출

- `web/src/hooks/GhostRun/useGhostRunLeaderboard.ts`
  - 인증 기준 변화에 따른 401 recovery 확인 필요

- `web/src/hooks/NormalRun/useNormalRunCourseDetail.ts`
  - course detail auth 전제 재검증 필요

- `web/src/hooks/NormalRun/useNormalRunMapCourses.ts`
  - nearby auth 전제 재검증 필요

- `web/src/hooks/GhostRun/useGhostRunMapCourses.ts`
  - nearby ghost auth 전제 재검증 필요

- `web/src/hooks/Auth/useLogout.ts`
  - `logout(accessToken | null)` 실패를 삼키고 로컬 세션만 정리하는 현재 UX 재검토 필요

- `web/src/hooks/Auth/bootstrapAuthSession.ts`
  - auth bootstrap은 현재 계약과 대체로 맞지만, 인증 강제 API가 늘어난 상태에서 app start 시점 race/refresh 재시도 회귀 확인 필요

---

## 4. `/web`에서 수정이 필요할 가능성은 낮지만 반드시 재검증해야 하는 위치

### 인증/부트스트랩

- `web/src/shared/libs/axiosClient.ts`
  - 401 refresh/retry 흐름이 auth 강제된 API 증가 상황에서도 정상인지 확인 필요

- `web/src/apis/auth/apis.ts`
  - `/auth/logout`은 이제 실질적으로 access token 전제

- `web/src/hooks/Auth/bootstrapAuthSession.ts`
- `web/src/hooks/Auth/useAuthBootstrap.ts`
- `web/src/features/Auth/AuthCallbackScreen.tsx`
- `web/src/routes/AuthGuards.tsx`
- `web/src/App.tsx`

### 프로필 / 홈 / 온보딩

- `web/src/features/Profile/ProfileScreen.tsx`
- `web/src/features/Profile/ProfileEditDialog.tsx`
- `web/src/features/Home/HomeScreen.tsx`
- `web/src/features/Onboarding/OnboardingFormStep.tsx`

이 파일들은 경로 변경보다는 “인증이 반드시 살아 있는 상태”를 전제로 동작하는지 재검증 필요

### 코스 / 러닝 화면

- `web/src/pages/private/NormalRunDetail.tsx`
- `web/src/pages/private/NormalRunSession.tsx`
- `web/src/pages/private/GhostRunDetail.tsx`
- `web/src/pages/private/GhostRunSession.tsx`
- `web/src/pages/private/FreeRunSessionPage.tsx`
- `web/src/pages/private/CalendarPage.tsx`
- `web/src/pages/private/CalendarDetailPage.tsx`

이 페이지 파일들은 주로 hook/feature 연결만 담당하지만, 라우트 진입/새로고침/deep link 상황에서 401 또는 404를 어떻게 처리하는지 함께 확인해야 합니다.

---

## 5. 현재 활성 UI consumer를 찾지 못한 API

아래 항목은 백엔드 계약은 바뀌었지만, 현재 `web/src/**` 안에서 실제 사용처를 찾지 못했습니다.

- `GET /api/users/nickname-availability`
  - wrapper 자체도 없음
  - 다만 닉네임 입력 UI는 다음 파일에 존재하므로 후속 연결 후보로 문서화 필요
    - `web/src/features/Onboarding/OnboardingFormStep.tsx`
    - `web/src/features/Profile/ProfileEditDialog.tsx`
- `POST /api/courses/{courseId}/like`
  - `web/src/apis/courseLike/apis.ts` wrapper는 있지만 실제 caller 없음
- `DELETE /api/courses/{courseId}/like`
  - wrapper 없음
- `POST /api/courses/{courseId}/bookmark`
- `DELETE /api/courses/{courseId}/bookmark`
- `GET /api/courses/bookmarks`
- `POST /api/courses/{courseId}/reviews`
- `GET /api/ghost-runnings/{runningSessionId}/result`
  - `web/src/apis/course/apis.ts`에 wrapper는 있지만 caller 없음
- `GET /api/meta/survey`
  - wrapper는 있으나 active UI consumer 없음

- `GET /api/calendars/me/weekly`
  - 백엔드 active endpoint 존재
  - 현재 `web/src` active consumer 없음
  - 홈 주간 통계는 아직 fixture 기반

문서상으로는 “현재 웹 활성 사용처 없음”으로 적는 것이 맞고, 없는 화면까지 있다고 가정해서 쓰면 안 됩니다.

---

## 6. 화면 단위 회귀 검증 대상

다음 라우트는 실제 수정이 없더라도 회귀 검증 대상입니다.

- `/login`
- `/auth/callback`
- `/onboarding`
- `/home`
- `/profile`
- `/normal-run`
- `/normal-run/:courseId`
- `/normal-run-session/:courseId`
- `/ghost-run`
- `/ghost-run/:courseId`
- `/ghost-run-session/:courseId`
- `/free-run`
- `/free-run-session`
- `/calendar`
- `/calendar/:runningSessionId`

### 특히 주의할 검증 포인트

- 로그인 직후 첫 API 호출이 401 없이 연결되는지
- 새로고침 후 `axiosClient`의 reissue -> retry 흐름이 정상인지
- detail page deep link 진입 시 auth bootstrap 후 데이터가 살아나는지
- logout 시 서버 cookie 정리 실패와 로컬 세션 정리가 UX상 문제 없는지
- calendar / running / ghost detail 경로가 새 계약 기준으로 호출되는지

---

## 7. 기존 문서 중 stale 상태인 파일

### `web/docs/calendar-flow.md`

아직 아래 내용을 포함합니다.

- `GET /calendars/{userId}/summary`
- `GET /calendars/{userId}`
- `GET /running-sessions/history/past?userId=1`
- `GET /running-sessions/{id}/detail/{userId}`
- `MOCK_USER_ID = 1`

즉, 현재 백엔드 계약과 가장 직접적으로 충돌하는 문서입니다.

### `web/docs/free-run-flow.md`

아직 아래 내용을 포함합니다.

- `POST /running-sessions` body에 `userId`
- `PATCH /running-sessions/{id}` body에 `userId`
- 여러 화면에서 `MOCK_USER_ID = 1`
- 결과/세션 흐름 설명이 현재 계약과 어긋남

### `web/docs/normal-run-course-deviation.md`

이 문서는 path 자체보다 `completeRunningSession()` 기반 force-stop 흐름 설명이 중심이라 직접 충돌은 덜하지만,
`CompleteRunningSessionRequest` 구조 변경이 반영되어야 합니다.

---

## 8. 별도 이슈로 같이 볼 만한 항목

이번 auth/userId 리팩터와 별개지만, 분석 중 아래 구조적 이상도 확인됐습니다.

- `web/src/features/GhostRun/GhostRunDetailScreen.tsx`
  - 고스트 세션을 시작하면서 `useNormalRunSessionStore`를 사용
  - 이동도 `/normal-run-session/${courseId}`로 수행
- 반면
  - `web/src/features/GhostRun/GhostRunSessionScreen.tsx`는 `useGhostRunSessionStore`를 사용
  - `web/src/routes/privateRoutes.tsx`에는 `/ghost-run-session/:courseId` 라우트가 존재

즉, ghost 세션 시작 -> 세션 화면 연결이 현재도 일관되지 않을 가능성이 높습니다.
이번 문서 범위의 직접 수정 대상은 아니지만, 회귀 검증에서는 반드시 같이 봐야 합니다.

추가 메모:

- 이 이슈는 단순 계약 변경 메모가 아니라 실제 사용자 플로우 회귀 가능성이 높은 구조 문제입니다.
- 따라서 `/ghost-run/:courseId` -> 시작 -> 세션 화면 진입은 독립 테스트 케이스로 분리하는 것이 좋습니다.

---

## 9. 작업 우선순위 제안

### 1순위: 바로 수정해야 하는 API 래퍼/타입

- `web/src/apis/userCalendar/apis.ts`
- `web/src/apis/runningSessions/apis.ts`
- `web/src/apis/runningSessions/types.ts`
- `web/src/apis/course/apis.ts`
- `web/src/apis/courseLike/apis.ts`
- `web/src/shared/hooks/Running/useRunningSplits.ts`

### 2순위: payload를 직접 만드는 feature 화면

- `web/src/features/FreeRun/FreeRunScreen.tsx`
- `web/src/features/FreeRun/FreeRunningScreen.tsx`
- `web/src/features/NormalRun/NormalRunDetailScreen.tsx`
- `web/src/features/NormalRun/NormalRunSessionScreen.tsx`
- `web/src/features/GhostRun/GhostRunDetailScreen.tsx`
- `web/src/features/GhostRun/GhostRunSessionScreen.tsx`

### 3순위: 회귀 검증

- auth bootstrap / logout
- home / profile / onboarding
- normal run / ghost run / free run
- calendar list / detail
- meta/course register modal

---

## 10. 한 줄 결론

현재 `/web`에서 가장 먼저 손봐야 하는 것은 `calendar`, `runningSessions`, `ghost-detail`, `courseLike` API 래퍼와 관련 타입/feature 파일입니다. 그 다음은 인증이 강제된 `course detail`, `nearby`, `meta`, `logout` 흐름을 화면 단위로 다시 검증해야 합니다.
