## 1. 인증 및 사용자 관리

### 1.1 회원가입/로그인

- 유저는 **Google 또는 Kakao 소셜 로그인**으로 회원가입/로그인을 한다.
- 유저는 이름 (닉네임), 생년월일, 성별, 키, 몸무게,
사용자 옵션
    1. 목적 - 여러개 선택 가능
    1-1. 다이어트
    1-2. 건강유지
    1-3. 대회준비
    1-4. 체력향상

    2. 코스 난이도 (경사, 코스모양): 쉬움, 보통, 어려움

    3. 코스 타입 (한강변, 공원, 산/둘레길, 트랙, 도심, 기타) - 여러개 선택 가능

    4. 어떤 시간대에 러닝하는지 (새벽, 아침, 낮, 저녁) - 여러개 선택 가능
    
    를 필수로 입력한다.
    

### 1.2 프로필 관리

- 유저는 자신의 프로필 정보(닉네임, 키, 몸무게 등) 를 수정한다.
- 유저는 자신의 러닝 선호도를 설정한다:
    - 러닝 목적 - 여러개 선택 가능
    - 선호 난이도
    - 선호 거리
    - 선호 시간대 - 여러개 선택 가능
    - 코스 타입 (한강변, 공원, 산/둘레길, 트랙, 도심, 기타) - 여러개 선택 가능

---

## 2. 러닝 세션

### 2.1 러닝 시작/종료

- 유저는 **자유 러닝(FREE)**, **코스 러닝(COURSE)**, **고스트 러닝(GHOST)**을 시작한다.
- **코스 러닝(COURSE)**과 **고스트 러닝(GHOST)**은 `courses.status = OFFICIAL`인 코스만 사용할 수 있다.
- 유저는 러닝을 시작하면 `시작 시간`이 기록된다.
- 유저는 러닝을 종료하면 `종료 시간`, `운동 시간`, `거리`, `평균 속도`, `소모 칼로리`이 계산되어 저장된다.
- 러닝 중 1KM 마다 의 페이스, 심박수, 고도 상승이 테이블에 기록된다.

### 2.2 GPS 추적

- 유저는 러닝 중 GPS 좌표가 1초마다 `running_gps_samples`에 저장된다 (`RDS PostgreSQL`).
- `running_gps_samples`는 `append-only` raw telemetry table이다.
- 주요 조회는 특정 러닝 세션의 최근 10분 raw GPS 데이터 조회다.
- 유저의 러닝 경로는 저장된다.

### 2.3 생체 정보

- 유저는 러닝 중 생체 정보가 1초마다 `running_health_samples`에 저장된다 (`RDS PostgreSQL`).
- `running_health_samples`는 `append-only` raw telemetry table이다.
- 주요 조회는 특정 러닝 세션의 최근 10분 raw health 데이터 조회다.

### 2.4 러닝 모드

- **자유 러닝 (FREE)**: 특정 코스 없이 자유롭게 러닝
- **코스 러닝 (COURSE)**: `courses.status = OFFICIAL`인 등록 코스를 따라 러닝
- **고스트 러닝 (GHOST)**:
    - 유저는 다른 사용자의 과거 러닝 기록과 경쟁한다.
    - 유저는 자신의 과거 러닝 기록과 경쟁한다.
    - `COURSE`와 동일하게 `courses.status = OFFICIAL`인 코스만 사용할 수 있다.

### 2.5 러닝 데이터 저장

- 유저는 러닝 종료 후 기록이 자동으로 저장된다.
    1. 유저는 러닝 기록을 경쟁모드로 실행했을때는 강제 공개.
    2. 유저는 러닝 기록을 일반모드로 실행했을때는 비공개.
- `running_sessions`, `running_session_splits`는 세션 결과와 구간 요약을 담는 summary/read model이다.
- `running_health_samples`, `running_gps_samples`는 러닝 중 raw telemetry를 담는 `append-only` 테이블이다.

### 2.6 러닝 기록을 코스로 등록

- 유저는 자신의 러닝 코스를 **커뮤니티 코스(`COMMUNITY`)로 즉시 등록**한다.
- 등록된 코스는 다른 유저들이 이용할 수 있다.
- 등록된 코스는 좋아요 10개 이상이면 **공식 코스(`OFFICIAL`)로 승격**된다.

---

## 3. 코스 관리

### 3.1 코스 유형

1. **정식 코스 (`OFFICIAL`)**: 관리자가 직접 생성하여 등록
2. **커뮤니티 코스 (`COMMUNITY`)**: 사용자가 등록한 코스 (다른 유저 이용 가능)
3. **개인 코스 (`PRIVATE`)**: 사용자만 볼 수 있는 코스

### 3.2 코스 생성

- **관리자**는 코스를 직접 생성하여 `courses` 테이블에 삽입한다.
- **유저**는 러닝 후 커뮤니티 코스로 즉시 등록한다:
    - 코스, 코스 제목이 저장된다.
    - 난이도 (쉬움, 보통, 어려움) 한개만 선택 가능, 코스 타입(한강변, 공원, 산/둘레길, 트랙, 도심, 기타) 등 여러개 선택 가능
    - 코스 스냅샷 이미지가 저장된다.

### 3.3 코스 검색 및 조회

- 유저는 코스 목록을 조회한다
    - 정식 코스
    - 커뮤니티 코스
- 유저는 코스를 거리, 코스 타입, 난이도 등으로 필터링한다.
- 유저는 코스 상세 정보를 조회한다 (경로, 거리, 난이도).
- 유저는 코스의 경로를 지도에서 확인한다.

### 3.4 코스 좋아요/북마크

- 유저는 코스를 이용하고 난 후에 **좋아요(like)** 또는 **종아요 취소(unlike)**를 누른다.
- 유저는 코스를 **북마크**에 추가한다.
- 유저는 북마크가 되어있는 코스 목록을 조회한다.

### 3.5 코스 평점 및 리뷰

- 유저는 코스에 **1~5점 평점**을 부여한다
평점을 부여하면 추가로 설문이 진행되며, 유저는 코스에 평점을 부여하면 체크박스 형태로 코스 타입을 선택 할 수 있다.
- 유저는 코스에 **난이도** (쉬움, 보통, 어려움)을 제출 할 수 있다.

### 3.6 코스 승격 시스템

- 커뮤니티 코스가 **좋아요 10개 이상**이면 공식 코스로 승격된다.

---

## 4. 러닝 기록 및 통계

### 4.1 러닝 기록 조회

- 유저는 자신의 러닝 기록 목록을 조회한다.
- 유저는 특정 러닝 기록의 상세 정보를 조회한다 (경로, 거리, 시간, 페이스, 칼로리 등).
- 유저는 러닝 기록을 날짜별로 필터링한다.

### 4.2 일일 통계

- 유저는 **일일 러닝 통계**를 조회한다:
    - 총 시간
    - 코스 러닝 횟수
    - 코스 스냅샷
    - 총 거리
    - 총 시간
    - 총 칼로리

### 4.3 러닝 생체 데이터

- 워치는 1초마다 생체 정보와 GPS 좌표를 raw telemetry table에 저장한다 (`RDS PostgreSQL`).
- 저장 데이터: 심박수, 속도, 페이스, 고도, GPS 좌표 등.

---

## 5. 경쟁 및 소셜 기능

### 5.1 고스트 러닝

- 유저는 2가지 방식으로 경쟁 할 수 있다.
    1. 유저는 다른 사용자의 과거 러닝 기록과 실시간 경쟁한다.
    2. 유저는 자신의 과거 러닝 기록과 경쟁한다. (`courses.status = OFFICIAL`인 코스 기준)
- 경쟁 결과는 러닝 세션에 기록된다.
- 코스별 고스트 리더보드는 `course_ghost_leaderboard`에 사용자, 코스, 러닝 세션 기준 점수로 저장된다.

### 5.2 코스 투표

- 유저는 코스에 like, unlike를 한다.
- 코스의 like/unlike 수가 승격 임계값에 도달하면 관리자 개입 없이 자동으로 공식 코스로 승격된다.

---

## 6. 관리자 기능

### 6.1 코스 관리

- 관리자는 코스를 직접 생성하여 등록한다.
- 관리자는 부적절한 코스를 삭제하거나 비공개 처리한다.

### 6.2 리워드 가챠 운영

- 관리자는 기존 `reward_items` 생성 API로 보상 아이템 카탈로그를 먼저 등록한다.
- 관리자는 draw pool 생성/수정 시 `cardsPerDraw`, `missWeight`, 그리고 각 아이템의 `weight`를 입력한다.
- 아이템 확률은 `reward_items`가 아니라 `reward_draw_pool_items.weight`에 저장된다.
- 꽝 비중은 pool 단위 설정인 `reward_draw_pools.miss_weight`로 관리한다.
- 운영 반영 순서는 **보상 아이템 생성 -> pool 생성/수정 -> pool 활성화**다.

---

## 7. 데이터 동기화 (Watch ↔ Phone ↔ Web)

### 7.1 워치 데이터 수집

- 워치는 Health Services를 통해 러닝 중 생체 데이터를 수집한다 (심박수, 거리, 칼로리, 속도, 페이스).
- 워치는 1초마다 데이터를 수집하고, Mobile이 이를 배치와 retry 기준에 따라 업로드한다.
- 워치는 수집한 데이터를 폰으로 전송한다 (Data Layer API).

### 7.2 폰-웹 동기화

- 폰은 워치에서 수신한 데이터를 웹 UI로 전달한다 (WebView bridge).
- 폰은 pending queue를 소유하고 60초 배치를 관리한다.
- 웹 UI는 러닝 데이터를 실시간으로 표시한다.
- 웹 UI는 relay / UI 역할을 수행하며 queue owner가 아니다.
- WAS는 업로드된 배치를 받아 raw telemetry를 저장하고, 성공 시점 기준으로 재시도 가능한 응답 계약을 제공한다.
- 웹 UI는 워치로 명령을 전송한다 (러닝 시작/일시정지/종료 등).

---

## 8. 데이터베이스 테이블 요약

### 8.1 현재 사용 중인 테이블

- `users`: 사용자 정보
- `user_auth_accounts`: 소셜 로그인 계정 연동
- `user_running_preferences`: 사용자 러닝 선호도 기본값
- `user_preference_purposes`: 사용자 러닝 목적 매핑
- `user_preference_course_types`: 사용자 선호 코스 타입 매핑
- `user_preference_time_slots`: 사용자 선호 시간대 매핑
- `courses`: 코스 정보
- `course_difficulties`: 코스 난이도 1:1 매핑
- `course_type_map`: 코스 타입 매핑
- `course_likes`: 코스 좋아요 매핑
- `course_bookmarks`: 코스 북마크
- `course_reviews`: 코스 평점 및 난이도 피드백
- `course_review_course_types`: 리뷰 코스 타입 설문
- `running_sessions`: 러닝 세션 summary/read model
- `running_session_splits`: 1km 구간별 summary/read model
- `running_health_samples`: 1초 주기 raw 생체/주행 telemetry 데이터
- `running_gps_samples`: 1초 주기 raw GPS telemetry 데이터
- `course_ghost_leaderboard`: 코스별 고스트 리더보드 점수
- `ghost_running_results`: 고스트 러닝 결과
- `user_calendars`: 일일 러닝 통계
- `reward_items`: 리워드 보상 카탈로그 메타데이터
- `reward_draw_pools`: cardsPerDraw / missWeight 를 포함한 운영 pool 헤더
- `reward_draw_pool_items`: pool 안 item별 weight 확률 구성
- `reward_gacha_draws`: 사용자 1회 draw 헤더
- `reward_gacha_draw_cards`: draw 별 카드 결과 행
- `reward_point_balances`: 현재 포인트 잔액
- `reward_point_histories`: 포인트 적립/차감 이력

현재 schema source에는 서버 측 동기화 체크포인트 전용 테이블을 두지 않는다.
