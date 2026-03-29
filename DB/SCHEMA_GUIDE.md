# DB Schema Guide

`DB/schema.sql` 기준으로 정리한 현재 스키마 안내 문서입니다. 이번 버전은 WAS Entity 구조와 Enum 네이밍을 기준으로 재설계한 결과를 반영합니다.

## 설계 기준

- 기준 소스: `was/src/main/java/kr/withrun/was/domain/**`의 현재 Entity / Enum 구조
- 공통 감사 컬럼: `BaseEntity`를 상속한 Entity 테이블은 `created_at`, `updated_at`, `deleted_at`를 가집니다. `BaseEntity`를 상속하지 않는 테이블은 Entity에 명시적으로 선언된 컬럼만 둡니다.
- 시간 타입: Entity가 `LocalDateTime` / `BaseEntity`의 `TIMESTAMP` 정의를 사용하므로 스키마도 `TIMESTAMP` 기준으로 맞췄습니다.
- Enum 저장 정책: Enum 컬럼은 모두 `VARCHAR`에 저장하며, 값은 현재 Java Enum 상수명 그대로 대문자 문자열을 사용합니다.
- 테이블 네이밍: 명시적 `@Table(name = ...)`는 그대로 따르고, `CourseTypeMap`은 Spring Boot/Hibernate 기본 snake_case 규칙을 따라 `course_type_map`으로 정리했습니다.
- 테이블 정리: 현재 Entity에 없는 `user_preference_surfaces`, `course_surface_maps`는 이번 스키마에서 제거했습니다.
- 현재 schema source에는 서버 측 동기화 체크포인트 전용 테이블을 두지 않습니다.

## 감사 컬럼 규칙

- `BaseEntity` 상속 테이블: `created_at`, `updated_at`, `deleted_at`
- `course_bookmarks`: Entity가 `created_at`만 직접 선언
- `course_likes`: 감사 컬럼 없음
- `course_ghost_leaderboard`: Entity가 `created_at`만 직접 선언합니다.

## Enum 컬럼 값

- `gender` (`users.gender`): `MALE`, `FEMALE`
- `preferred_difficulty`, `difficulty`, `submitted_difficulty`: `EASY`, `MEDIUM`, `HARD`
- `purpose`: `DIET`, `HEALTH_MAINTENANCE`, `RACE_PREPARATION`, `ENDURANCE_IMPROVEMENT`
- `course_type`: `RIVERSIDE`, `PARK`, `MOUNTAIN_TRAIL`, `TRACK`, `URBAN`, `OTHER`
- `time_slot`: `DAWN`, `MORNING`, `AFTERNOON`, `EVENING`
- `status` (`courses.status`): `OFFICIAL`, `COMMUNITY`, `PRIVATE`
- `mode`: `FREE`, `COURSE`, `GHOST`
- `result_status`: `WIN`, `LOSE`, `DRAW`, `INCOMPLETE`

## 관계 모델 요약

### 공유 PK 1:1 관계

- `user_auth_accounts.user_id` -> `users.user_id`
- `user_running_preferences.user_id` -> `users.user_id`
- `course_difficulties.course_id` -> `courses.course_id`

### 일반 FK / 매핑 테이블

- 사용자 선호 다중 선택: `user_preference_purposes`, `user_preference_course_types`, `user_preference_time_slots`
- 코스 타입 매핑: `course_type_map`
- 리뷰 다중 선택 매핑: `course_review_course_types`
- 고스트 리더보드 매핑: `course_ghost_leaderboard`
- 사용자 집계: `user_calendars`

### 러닝 세션 관계

- `running_sessions.user_id` -> `users.user_id`
- `running_sessions.course_id` -> `courses.course_id`
- `running_sessions.ghost_target_running_session_id` -> `running_sessions.running_session_id`
- 샘플 테이블(`running_health_samples`, `running_gps_samples`)은 모두 `running_sessions`를 부모로 둡니다.
- `course_ghost_leaderboard`는 `users`, `courses`, `running_sessions`를 함께 참조하는 코스별 고스트 점수 보드입니다.
- `ghost_running_results.running_session_id`는 세션당 1건만 허용됩니다.

## 테이블 상세

### users

- 목적: 사용자 프로필의 기준 엔터티
- 핵심 컬럼:
  - `user_id`: PK
  - `nickname`: 유니크 닉네임
  - `birth_date`: 생년월일
  - `gender`: `MALE`, `FEMALE`
  - `height_cm`, `weight_kg`: 신체 정보
- 주요 제약:
  - `uq_users_nickname`
  - `chk_users_birth_date`
  - `chk_users_gender`
  - `chk_users_height`, `chk_users_weight`

### user_auth_accounts

- 목적: 사용자 소셜 로그인 계정 연동 정보
- 관계: `users`와 공유 PK 1:1
- 핵심 컬럼:
  - `user_id`: PK이자 FK
  - `provider`, `provider_user_id`: 외부 인증 식별자
  - `email`, `profile_image_url`: 부가 프로필 정보
- 주요 제약:
  - `fk_user_auth_accounts_user` (`ON DELETE CASCADE`)
  - `uq_user_auth_accounts_provider_identity`

### user_running_preferences

- 목적: 사용자 러닝 선호 기본값
- 관계: `users`와 공유 PK 1:1
- 핵심 컬럼:
  - `user_id`: PK이자 FK
  - `preferred_difficulty`: `EASY`, `MEDIUM`, `HARD`
  - `preferred_distance_km`: 선호 거리(km)
- 주요 제약:
  - `fk_user_running_preferences_user` (`ON DELETE CASCADE`)
  - `chk_user_running_preferences_difficulty`
  - `chk_user_running_preferences_distance`

### user_preference_purposes

- 목적: 사용자 다중 선택 러닝 목적
- 핵심 컬럼:
  - `user_preference_purpose_id`: PK
  - `user_id`: `user_running_preferences.user_id` FK
  - `purpose`: `DIET`, `HEALTH_MAINTENANCE`, `RACE_PREPARATION`, `ENDURANCE_IMPROVEMENT`
- 주요 제약:
  - `fk_user_preference_purpose_user_running_preference` (`ON DELETE CASCADE`)
  - `uq_user_preference_purposes_user_purpose`
  - `chk_user_preference_purposes_purpose`

### user_preference_course_types

- 목적: 사용자 다중 선택 선호 코스 타입
- 핵심 컬럼:
  - `user_preference_course_type_id`: PK
  - `user_id`: `user_running_preferences.user_id` FK
  - `course_type`: `RIVERSIDE`, `PARK`, `MOUNTAIN_TRAIL`, `TRACK`, `URBAN`, `OTHER`
- 주요 제약:
  - `fk_user_preference_course_type_user_running_preference` (`ON DELETE CASCADE`)
  - `uq_user_preference_course_types_user_type`
  - `chk_user_preference_course_types_course_type`

### user_preference_time_slots

- 목적: 사용자 다중 선택 선호 시간대
- 핵심 컬럼:
  - `user_preference_time_slot_id`: PK
  - `user_id`: `user_running_preferences.user_id` FK
  - `time_slot`: `DAWN`, `MORNING`, `AFTERNOON`, `EVENING`
- 주요 제약:
  - `fk_user_preference_time_slot_user_running_preference` (`ON DELETE CASCADE`)
  - `uq_user_preference_time_slots_user_slot`
  - `chk_user_preference_time_slots_time_slot`

### courses

- 목적: 재사용 가능한 러닝 코스 메타데이터
- 핵심 컬럼:
  - `course_id`: PK
  - `created_by_user_id`: 코스 생성 사용자 FK
  - `title`: 코스 제목
  - `status`: `OFFICIAL`, `COMMUNITY`, `PRIVATE`
  - `distance_m`, `elevation_gain_m`: 코스 기본 지표
  - `start_latitude`, `start_longitude`: 시작 좌표
  - `end_latitude`, `end_longitude`: 종료 좌표, nullable
  - `coordinates`: JSONB 경로 데이터
  - `snapshot_image_url`, `promoted_at`
- 주요 제약:
  - `fk_courses_created_by` (`ON DELETE SET NULL`)
  - `chk_courses_status`
  - 거리/고도/좌표 범위 체크

### reward_items

- 목적: 리워드 가챠와 인벤토리에서 사용하는 보상 아이템 카탈로그 메타데이터 저장
- 핵심 컬럼:
  - `reward_item_id`: PK
  - `title`: 관리자 입력 제목
  - `image_url`: 저장된 S3 object key 또는 URL 문자열
  - `is_active`: 관리자 사용 가능 여부
  - `created_at`, `updated_at`, `deleted_at`: `BaseEntity` 감사 컬럼
- 주요 제약:
  - `title` 길이 100자 제한
  - `image_url` not null
  - `is_active` 기본값 `TRUE`
- 비고:
  - 기본 목록 조회 인덱스로 `(is_active, created_at desc)`를 사용합니다.
  - soft delete 된 항목은 애플리케이션 조회에서 제외합니다.

### course_difficulties

- 목적: 코스 난이도 1:1 저장
- 관계: `courses`와 공유 PK 1:1
- 핵심 컬럼:
  - `course_id`: PK이자 FK
  - `difficulty`: `EASY`, `MEDIUM`, `HARD`
- 주요 제약:
  - `fk_course_difficulties_course` (`ON DELETE CASCADE`)
  - `chk_course_difficulties_difficulty`

### course_type_map

- 목적: 코스 다중 선택 타입 매핑
- 해석 포인트: `CourseTypeMap` Entity는 명시적 테이블명을 두지 않아 Spring 기본 네이밍 결과인 `course_type_map`을 사용합니다.
- 핵심 컬럼:
  - `course_type_map_id`: PK
  - `course_id`: `courses.course_id` FK
  - `course_type`: `RIVERSIDE`, `PARK`, `MOUNTAIN_TRAIL`, `TRACK`, `URBAN`, `OTHER`
- 주요 제약:
  - `fk_course_type_maps_course` (`ON DELETE CASCADE`)
  - `uq_course_type_map_course_type`
  - `chk_course_type_map_course_type`

### running_sessions

- 목적: 자유/코스/고스트 러닝 세션 요약 정보
- 핵심 컬럼:
  - `running_session_id`: PK
  - `user_id`: 세션 소유 사용자 FK
  - `course_id`: `COURSE`, `GHOST` 모드용 FK, nullable
  - `mode`: `FREE`, `COURSE`, `GHOST`
  - `ghost_target_running_session_id`: 고스트 타깃 세션 FK, nullable
  - `started_at`, `ended_at`, `duration_sec`
  - `distance_m`, `avg_speed_mps`, `avg_pace_sec_per_km`
  - `calories_kcal`, `elevation_gain_m`
  - `snapshot_image_url`
  - `start_latitude`, `start_longitude`, `end_latitude`, `end_longitude`
  - `coordinates`: JSONB 경로 데이터
  - `is_public`, `is_completed`
- 주요 제약:
  - `fk_running_sessions_user` (`ON DELETE CASCADE`)
  - `fk_running_sessions_course` (`ON DELETE SET NULL`)
  - `fk_running_sessions_ghost_target_running_session` (`ON DELETE SET NULL`)
  - `chk_running_sessions_mode`
  - 시간/거리/속도/페이스/칼로리/고도 범위 체크
  - `chk_running_sessions_course_mode`
  - `chk_running_sessions_ghost_mode`
  - `chk_running_sessions_visibility_for_ghost`
  - `chk_running_sessions_completed_state`
- 비고:
  - `COURSE`와 `GHOST`는 `courses.status = OFFICIAL`인 코스만 사용할 수 있으며, 이 교차 테이블 규칙은 WAS 애플리케이션 검증으로 강제합니다.
  - `running_sessions`는 raw sample의 부모이면서 세션 결과를 담는 summary/read model입니다.

### running_session_splits

- 목적: 세션별 1km 구간 상세 지표 저장
- 핵심 컬럼:
  - `running_session_split_id`: PK
  - `running_session_id`: 세션 FK
  - `split_index`: 세션 내 구간 순번
  - `split_distance_m`, `split_duration_sec`, `split_pace_sec_per_km`
  - `avg_heart_rate`: 평균 심박수(`INTEGER`), nullable
  - `elevation_gain_m`: 구간 누적 상승 고도
- 주요 제약:
  - `fk_running_session_splits_running_session` (`ON DELETE CASCADE`)
  - `uq_running_session_splits_session_index`
  - `chk_running_session_splits_index`
  - `chk_running_session_splits_distance`
  - `chk_running_session_splits_duration`
  - `chk_running_session_splits_pace`
  - `chk_running_session_splits_avg_heart_rate`
  - `chk_running_session_splits_elevation`
- 비고:
  - `running_session_splits`는 raw telemetry를 직접 대체하지 않고, 구간 단위 요약을 담는 summary/read model입니다.

### running_health_samples

- 목적: 세션별 raw health/주행 telemetry 저장
- 핵심 컬럼:
  - `running_health_sample_id`: PK
  - `running_session_id`: 세션 FK
  - `sampled_at`: 샘플링 시각
  - `user_id`: 사용자 FK
  - `heart_rate`, `speed_mps`, `pace_sec_per_km`
  - `distance_m`, `calories_kcal`, `cadence_spm`
- 주요 제약:
  - `fk_running_health_samples_running_session`, `fk_running_health_samples_user` (둘 다 `ON DELETE CASCADE`)
  - `uq_running_health_samples_session_time`
  - 심박/속도/페이스/거리/칼로리/케이던스 체크
- 비고:
  - `append-only` raw telemetry table로 사용합니다.
  - `1초 주기` 샘플 저장을 전제로 합니다.
  - 핵심 read path는 `특정 running_session_id의 최근 10분 조회`입니다.
  - 기본 hot path index는 기존 UNIQUE `(running_session_id, sampled_at)`를 우선 활용합니다.
  - 동일 키의 별도 `DESC` 인덱스는 기본값으로 두지 않습니다.
  - 서버 측 동기화 체크포인트 전용 테이블 없이 raw row 저장/조회 기준선을 설명합니다.
  - `BRIN`, time partitioning, retention 상세값은 운영 이슈가 커질 때 후속 이슈에서 재검토합니다.

### running_gps_samples

- 목적: 세션별 raw GPS telemetry 저장
- 핵심 컬럼:
  - `running_gps_sample_id`: PK
  - `running_session_id`: 세션 FK
  - `sampled_at`: 샘플링 시각
  - `user_id`: 사용자 FK
  - `latitude`, `longitude`
  - `altitude_m`, `accuracy_m`, `bearing_deg`
- 주요 제약:
  - `fk_running_gps_samples_running_session`, `fk_running_gps_samples_user` (둘 다 `ON DELETE CASCADE`)
  - `uq_running_gps_samples_session_time`
  - 위도/경도/정확도/방향값 범위 체크
- 비고:
  - `append-only` raw telemetry table로 사용합니다.
  - `1초 주기` 샘플 저장을 전제로 합니다.
  - 핵심 read path는 `특정 running_session_id의 최근 10분 조회`입니다.
  - 기본 hot path index는 기존 UNIQUE `(running_session_id, sampled_at)`를 우선 활용합니다.
  - 동일 키의 별도 `DESC` 인덱스는 기본값으로 두지 않습니다.
  - 서버 측 동기화 체크포인트 전용 테이블 없이 raw row 저장/조회 기준선을 설명합니다.
  - `BRIN`, time partitioning, retention 상세값은 운영 이슈가 커질 때 후속 이슈에서 재검토합니다.

### course_ghost_leaderboard

- 목적: 코스별 고스트 경쟁 점수를 사용자/세션 단위로 저장
- 핵심 컬럼:
  - `course_ghost_leaderboard_id`: PK
  - `user_id`: `users.user_id` FK
  - `course_id`: `courses.course_id` FK
  - `running_session_id`: `running_sessions.running_session_id` FK
  - `point`: 리더보드 점수
  - `created_at`: 생성 시각
- 주요 제약:
  - `uq_course_ghost_leaderboard_user_course_session` 유니크 (`user_id`, `course_id`, `running_session_id`)
  - `fk_course_ghost_leaderboard_user`, `fk_course_ghost_leaderboard_course`, `fk_course_ghost_leaderboard_running_session` (모두 `ON DELETE CASCADE`)
  - `chk_course_ghost_leaderboard_point`

### ghost_running_results

- 목적: 고스트 러닝 결과 저장
- 핵심 컬럼:
  - `ghost_running_result_id`: PK
  - `running_session_id`: 결과가 귀속되는 세션 FK
  - `ghost_target_running_session_id`: 비교 대상 세션 FK
  - `target_user_id`: 대상 사용자 FK, nullable
  - `result_status`: `WIN`, `LOSE`, `DRAW`, `INCOMPLETE`
  - `time_gap_sec`, `distance_gap_m`
- 주요 제약:
  - `fk_ghost_running_results_running_session` (`ON DELETE CASCADE`)
  - `fk_ghost_running_results_ghost_target_running_session` (`ON DELETE RESTRICT`)
  - `fk_ghost_running_results_target_user` (`ON DELETE SET NULL`)
  - `uq_ghost_running_results_session`
  - `chk_ghost_running_results_status`
  - `chk_ghost_running_results_diff_session`

### course_likes

- 목적: 사용자-코스 좋아요 관계 저장
- 핵심 컬럼:
  - `course_like_id`: PK
  - `course_id`, `user_id`: 코스/사용자 FK
- 감사 컬럼:
  - 없음 (`CourseLike` Entity는 `BaseEntity`를 상속하지 않음)
- 주요 제약:
  - `fk_course_likes_course`, `fk_course_likes_user` (둘 다 `ON DELETE CASCADE`)
  - `uq_course_likes_course_user`

### course_bookmarks

- 목적: 사용자 북마크 코스 저장
- 핵심 컬럼:
  - `course_bookmark_id`: PK
  - `course_id`, `user_id`: 코스/사용자 FK
  - `created_at`: 생성 시각
- 감사 컬럼:
  - `created_at`만 사용 (`CourseBookmark` Entity가 직접 선언)
- 주요 제약:
  - `fk_course_bookmarks_course`, `fk_course_bookmarks_user` (둘 다 `ON DELETE CASCADE`)
  - `uq_course_bookmarks_course_user`

### course_reviews

- 목적: 코스 평점/리뷰 및 제출 난이도 저장
- 핵심 컬럼:
  - `course_review_id`: PK
  - `course_id`, `user_id`: 코스/리뷰어 FK
  - `rating`: 1~5 점수
  - `submitted_difficulty`: `EASY`, `MEDIUM`, `HARD`, nullable
- 주요 제약:
  - `fk_course_reviews_course`, `fk_course_reviews_user` (둘 다 `ON DELETE CASCADE`)
  - `uq_course_reviews_course_user`
  - `chk_course_reviews_rating`
  - `chk_course_reviews_submitted_difficulty`

### course_review_course_types

- 목적: 리뷰 작성 시 선택한 코스 타입 설문 응답 저장
- 핵심 컬럼:
  - `course_review_course_type_id`: PK
  - `course_review_id`: `course_reviews.course_review_id` FK
  - `course_type`: `RIVERSIDE`, `PARK`, `MOUNTAIN_TRAIL`, `TRACK`, `URBAN`, `OTHER`
- 주요 제약:
  - `fk_course_reviews_course_types_review` (`ON DELETE CASCADE`)
  - `uq_course_review_course_types_review_type`
  - `chk_course_review_course_types_course_type`

### user_calendars

- 목적: 사용자 일자별 러닝 집계 통계 저장
- 핵심 컬럼:
  - `user_calendar_id`: PK
  - `user_id`: 사용자 FK
  - `calendar_date`: 집계 날짜
  - `total_distance_m`, `total_duration_sec`, `total_calories_kcal`
  - `course_run_count`, `free_run_count`, `ghost_run_count`
  - `representative_snapshot_url`
- 주요 제약:
  - `fk_user_calendars_user` (`ON DELETE CASCADE`)
  - `uq_user_calendars_user_date`
  - 거리/시간/칼로리/횟수 음수 방지 체크
