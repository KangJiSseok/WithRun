# Home Tab + OAuth Integration Checklist

## 문서 목적

이 문서는 웹/모바일 OAuth 연동, 홈 탭 재설계, 프로필/회원탈퇴 구현 현황을 체크리스트로 정리한 문서다.

기준 시점:
- 2026-03-26

원칙:
- 실제로 구현된 항목만 체크한다.
- `lint`, `build`, `compile` 등으로 확인한 항목만 검증 완료로 적는다.
- 저장 코스, 프로필 이미지 업로드처럼 아직 범위 밖인 기능은 미완료로 둔다.

---

## 1. Route / Auth Bootstrap

- [x] `/login` public route 추가
- [x] `/auth/callback` public route 추가
- [x] `/` entry route가 auth bootstrap 결과 기준으로 분기하도록 수정
- [x] 기존 로컬 onboarding 완료 플래그 기반 분기 제거
- [x] 공통 auth guard 추가
- [x] 비로그인 상태면 `/onboarding`으로 이동하도록 정리
- [x] 로그인 + `profileCompleted=false`면 `/onboarding` 유지
- [x] 로그인 + `profileCompleted=true`면 private route 허용
- [x] `/home`, `/calendar`, `/free-run`, `/normal-run`, `/ghost-run`, `/profile`에 동일 guard 적용
- [x] 앱 시작 시 auth bootstrap 실행 구조 추가
- [x] bootstrap 상태를 `idle | bootstrapping | authenticated | unauthenticated`로 관리

완료 기준 메모:
- 새로고침 시 entry/auth guard가 세션 복구 상태에 맞춰 동작하도록 정리됨
- 온보딩 흐름은 다시 `intro -> login -> form` 구조로 복원됨

---

## 2. Auth Store / Axios

- [x] `web/src/stores/Auth/` 추가
- [x] `accessToken`, `user`, `status` 전역 상태 관리 추가
- [x] localStorage 기반 auth persist 추가
- [x] `bootstrapAuth`, `setAuthenticated`, `setAccessToken`, `updateUser`, `clearAuth` 구현
- [x] `web/src/apis/auth/` 추가
- [x] `reissueAccessToken` 추가
- [x] `getCurrentUser` 추가
- [x] `logout` 추가
- [x] `axiosClient`에서 access token 자동 부착
- [x] 일반 API 401 시 `reissue` 1회 후 원 요청 1회 재시도
- [x] `/auth/reissue`, `/auth/me`, `/auth/logout`은 재귀 재시도 제외
- [x] refresh 실패 시 auth 초기화 후 로그인 흐름으로 복귀
- [x] shared in-flight refresh promise 처리 추가

검증:
- [x] `web npm run lint`
- [x] `web npm run build`

---

## 3. Login / Callback

- [x] `/login` page 추가
- [x] 카카오 로그인 버튼 추가
- [x] 구글 로그인 버튼 추가
- [x] `cancelled`, `access_denied`, `server_error` 에러 배너 처리 추가
- [x] 브라우저 환경에서 `window.location.assign(${server}/oauth2/authorization/{provider})` 사용
- [x] WebView 환경에서 브리지 `startSocialLogin(url)` 사용
- [x] `/auth/callback` page 추가
- [x] callback에서 `error` query 분기 처리 추가
- [x] callback에서 `reissue -> me` 순서로 세션 복구
- [x] `profileCompleted`에 따라 `/onboarding` 또는 `/home` 이동
- [x] 실패 시 auth 초기화 후 온보딩으로 복귀
- [x] 백엔드 OAuth success/failure redirect를 shared callback route 기준으로 정리

백엔드 연계:
- [x] 모바일 OAuth 전용 진입점 `GET /mobile/oauth2/authorization/{provider}` 추가
- [x] OAuth success handler에서 웹/모바일 callback 분기 추가
- [x] OAuth failure handler에서 웹/모바일 callback 분기 추가

검증:
- [x] `was ./gradlew compileJava`
- [x] Swagger에서 `/auth/me`, `/auth/reissue`, `/auth/logout` 확인
- [ ] 카카오/구글 로그인 전체 수동 회귀 확인
- [ ] 모바일 WebView 로그인 전체 수동 회귀 확인

---

## 4. Onboarding DTO Alignment

- [x] 온보딩 폼 값을 백엔드 DTO 기준으로 재정렬
- [x] `PUT /api/users/me` 저장 연결
- [x] `GET/PUT /api/users/me/running-preferences` 저장 연결
- [x] 목적을 복수 선택 구조로 변경
- [x] 시간대를 복수 선택 구조로 변경
- [x] 코스 타입을 복수 선택 구조로 변경
- [x] 거리/난이도는 단일 값으로 유지
- [x] `DIET`, `HEALTH_MAINTENANCE`, `RACE_PREPARATION`, `ENDURANCE_IMPROVEMENT` 매핑 적용
- [x] `DAWN`, `MORNING`, `AFTERNOON`, `EVENING` 매핑 적용
- [x] `EASY`, `MEDIUM`, `HARD` 매핑 적용
- [x] `RIVERSIDE`, `PARK`, `MOUNTAIN_TRAIL`, `TRACK`, `URBAN`, `OTHER` 매핑 적용
- [x] 기존 `wellness`, `fitness`, `noon`, `trail` 값 제거
- [x] 저장 순서를 `프로필 -> 러닝 선호도 -> me 재조회 -> home 이동`으로 정리
- [x] 온보딩 form 단계에서도 배경 애니메이션 유지
- [x] 온보딩 새로고침 시 입력값은 초기화되고 로그인 상태는 유지되도록 정리

메모:
- 온보딩 draft persistence는 넣지 않음
- 온보딩 완료 여부 source of truth는 `GET /api/auth/me.profileCompleted`

---

## 5. Home Tab Redesign

- [x] 기존 홈 디버그 화면 제거
- [x] 홈 전용 상단 헤더 구현
- [x] 좌측 프로필 버튼을 `/profile`로 연결
- [x] 우측 북마크 버튼은 비활성 시각 요소로 유지
- [x] 닉네임 기반 인사 문구 적용
- [x] 홈 배경을 기본 밝은 톤으로 조정
- [x] 상단 4카드 UI 구현
- [x] 홈 통계 타입 `HomeSummaryStats` 분리
- [x] 현재 버전용 더미 통계 adapter 구현
- [x] `HEALTH CONNECT ON/OFF` 배지 구현
- [x] health badge를 브리지/워치 최근 메시지 기준으로 계산
- [x] 일반 브라우저에서는 health badge가 OFF로 표시되도록 처리
- [x] 홈에서 러닝 선호도 조회 연결
- [x] 홈에서 러닝 선호도 수정/저장 연결
- [x] 선호 시간대 섹션 추가
- [x] 코스 타입 `OTHER` 포함
- [x] 인사 영역 우측 장식 요소 제거

추후 예정:
- [ ] 홈 통계 백엔드 요약 API 연결
- [ ] 북마크 버튼 실제 기능 연결

---

## 6. Mobile WebView OAuth / Deep Link

- [x] `mobile/src/bridge/bridge.ts`에 `startSocialLogin(url)` 추가
- [x] `mobile/src/bridge/types.ts` 동기화
- [x] Android `withrun://auth/callback` 딥링크 설정 추가
- [x] iOS `withrun://auth/callback` 딥링크 설정 추가
- [x] Expo `scheme: withrun` 설정 추가
- [x] WebView에서 딥링크 수신 시 `/auth/callback`으로 변환하는 처리 추가
- [x] 웹 callback 로직을 모바일에서도 재사용하도록 정리

검증:
- [x] `mobile npx tsc --noEmit`
- [ ] Android 실기기/에뮬레이터에서 OAuth 수동 확인
- [ ] iOS 시뮬레이터/실기기에서 OAuth 수동 확인

---

## 7. Profile / My Page

- [x] `/profile` private route 추가
- [x] 홈/캘린더 프로필 버튼을 `/profile`로 연결
- [x] `GET /api/users/me` 기반 프로필 조회 화면 구현
- [x] 프로필 수정 모달 구현
- [x] `PUT /api/users/me` 기반 프로필 수정 연결
- [x] 수정 성공 후 auth store와 화면 상태 동기화
- [x] 로그아웃 버튼 구현
- [x] `POST /api/auth/logout` 연결
- [x] 회원탈퇴 버튼 구현
- [x] `DELETE /api/users/me` 백엔드 API 구현
- [x] 회원탈퇴 시 refresh cookie 만료 처리
- [x] 회원탈퇴 시 소셜 계정 연결 정보 제거
- [x] 회원탈퇴 성공 후 auth store clear + `/onboarding` 이동
- [x] 프로필 placeholder avatar 적용
- [x] birthDate가 없는 경우에만 수정 모달에서 입력받도록 처리

추후 예정:
- [ ] 프로필 이미지 조회/업로드
- [ ] 저장된 코스 섹션
- [ ] 회원탈퇴 확인 모달을 shadcn dialog 기반으로 고도화

---

## 8. 기타 안정화

- [x] `FreeRunPage` 가이드 노출 로직을 초기 state 기반으로 정리
- [x] 러닝 GPS hook에서 latest ref 동기화를 effect로 이동
- [x] 러닝 Health hook에서 latest ref 동기화를 effect로 이동
- [x] OAuth success/failure 테스트 기대값을 shared callback 기준으로 수정
- [x] 변경된 핵심 로직에 주석 보강

---

## 9. 검증 현황

- [x] `was ./gradlew compileJava`
- [x] `web npm run lint`
- [x] `web npm run build`
- [x] `mobile npx tsc --noEmit`
- [x] `http://localhost:5173/` 응답 확인
- [x] `http://localhost:8080/swagger-ui/index.html` 응답 확인
- [x] Swagger에 `DELETE /api/users/me` 반영 확인

남은 수동 확인:
- [ ] 웹 카카오 로그인 전체 플로우
- [ ] 웹 구글 로그인 전체 플로우
- [ ] 회원탈퇴 후 동일 소셜 계정 재로그인 플로우
- [ ] 모바일 WebView OAuth 복귀 플로우
