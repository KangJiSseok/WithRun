# DB Schema Guide

`DB/schema.sql` 기준으로 작성한 스키마 안내 문서입니다. 백엔드 개발 시 테이블 책임, 핵심 컬럼, 제약/인덱스 포인트를 빠르게 확인하는 용도입니다.

## VARCHAR 문자열 타입 (JPA EnumType.STRING)

모든 ENUM 값은 JPA `EnumType.STRING` 방식으로 `VARCHAR` 컬럼에 저장됩니다:

- `auth_provider` (VARCHAR(16)): `google`, `kakao`
- `gender` (VARCHAR(16)): `male`, `female`, `other`
- `running_mode` (VARCHAR(16)): `free`, `course`, `ghost`
- `course_status` (VARCHAR(16)): `official`, `community`, `private`
- `course_difficulty` (VARCHAR(16)): `easy`, `medium`, `hard`
- `surface_type` (VARCHAR(16)): `paved`, `unpaved`, `mixed`
- `course_type` (VARCHAR(32)): `riverside`, `park`, `mountain_trail`, `track`, `urban`, `other`
- `time_slot` (VARCHAR(16)): `dawn`, `morning`, `afternoon`, `evening`
- `purpose` (VARCHAR(32)): `diet`, `health_maintenance`, `race_preparation`, `endurance_improvement`

## 관계형 모델 설명

### 비식별관계 (Non-Identifying Relationship)

모든 테이블은 **비식별관계**로 설계되어 있습니다:
- 자식 테이블은 부모 테이블의 PK를 **FK로만 참조**하며, 자신만의 독립적인 PK를 가집니다.
- 기존 복합 PK는 **UNIQUE 제약조건**으로 변경되어 비즈니스 로직상 유일성은 보장됩니다.
- ERD Cloud 등의 도구에서 기본키가 정확히 인식됩니다.

## 테이블 상세

### users

- 목적: 사용자 프로필의 기준 엔터티를 저장합니다.
- 주요 컬럼 설명:
  - `user_id`: PK, identity 기반 사용자 식별자
  - `nickname`: 사용자 표시명, 유니크
  - `birth_date`: 생년월일
  - `gender`: VARCHAR(16) (`male`, `female`, `other`)
  - `height_cm`, `weight_kg`: 신체 정보
  - `is_active`: 계정 활성 상태
  - `created_at`, `updated_at`: 생성/수정 시각
- 주요 제약/인덱스 포인트:
  - `uq_users_nickname` 유니크 제약
  - `chk_users_birth_date` (`birth_date < CURRENT_DATE`)
  - `chk_users_height`, `chk_users_weight` 범위 제약

### user_auth_accounts

- 목적: 사용자와 소셜 로그인 계정 연동 정보를 저장합니다. **users와 1:1 매핑**입니다.
- 주요 컬럼 설명:
  - `user_id`: PK이자 FK, `users.user_id` 참조 (1:1 관계)
  - `provider`: VARCHAR(16) (`google`, `kakao`)
  - `provider_user_id`: 제공자 측 사용자 식별자
  - `email`, `profile_image_url`: 부가 프로필 정보
  - `linked_at`, `last_login_at`: 연동/마지막 로그인 시각
- 주요 제약/인덱스 포인트:
  - `fk_user_auth_accounts_user` (`ON DELETE CASCADE`)
  - `uq_user_auth_accounts_provider_identity` (`provider`, `provider_user_id`) UNIQUE

### user_running_preferences

- 목적: 사용자 단일 선택 러닝 선호 기본값을 저장합니다. **users와 1:1 매핑**입니다.
- 주요 컬럼 설명:
  - `user_id`: PK이자 FK, `users.user_id` 참조 (1:1 관계)
  - `preferred_difficulty`: VARCHAR(16) (`easy`, `medium`, `hard`)
  - `preferred_distance_km`: 선호 거리(km)
  - `created_at`, `updated_at`: 생성/수정 시각
- 주요 제약/인덱스 포인트:
  - `fk_user_running_preferences_user` (`ON DELETE CASCADE`)
  - `chk_user_running_preferences_distance` (0 초과, 100 이하)

### user_preference_purposes

- 목적: 사용자 다중 선택 러닝 목적(`purpose`) 매핑을 저장합니다.
- 주요 컬럼 설명:
  - `user_preference_purpose_id`: PK (비식별관계용 독립 PK)
  - `user_id`: `user_running_preferences.user_id` FK
  - `purpose`: VARCHAR(32) (`diet`, `health_maintenance`, `race_preparation`, `endurance_improvement`)
  - `created_at`: 생성 시각
- 주요 제약/인덱스 포인트:
  - `uq_user_preference_purposes_user_purpose` (`user_id`, `purpose`) UNIQUE
  - `fk_user_preference_purposes_user` (`ON DELETE CASCADE`)

### user_preference_surfaces

- 목적: 사용자 다중 선택 선호 노면(`surface_type`) 매핑을 저장합니다.
- 주요 컬럼 설명:
  - `user_preference_surface_id`: PK (비식별관계용 독립 PK)
  - `user_id`: `user_running_preferences.user_id` FK
  - `surface_type`: VARCHAR(16) (`paved`, `unpaved`, `mixed`)
  - `created_at`: 생성 시각
- 주요 제약/인덱스 포인트:
  - `uq_user_preference_surfaces_user_surface` (`user_id`, `surface_type`) UNIQUE
  - `fk_user_preference_surfaces_user` (`ON DELETE CASCADE`)

### user_preference_course_types

- 목적: 사용자 다중 선택 선호 코스 타입(`course_type`) 매핑을 저장합니다.
- 주요 컬럼 설명:
  - `user_preference_course_type_id`: PK (비식별관계용 독립 PK)
  - `user_id`: `user_running_preferences.user_id` FK
  - `course_type`: VARCHAR(32) (`riverside`, `park`, `mountain_trail`, `track`, `urban`, `other`)
  - `created_at`: 생성 시각
- 주요 제약/인덱스 포인트:
  - `uq_user_preference_course_types_user_type` (`user_id`, `course_type`) UNIQUE
  - `fk_user_preference_course_types_user` (`ON DELETE CASCADE`)

### user_preference_time_slots

- 목적: 사용자 다중 선택 선호 시간대(`time_slot`) 매핑을 저장합니다.
- 주요 컬럼 설명:
  - `user_preference_time_slot_id`: PK (비식별관계용 독립 PK)
  - `user_id`: `user_running_preferences.user_id` FK
  - `time_slot`: VARCHAR(16) (`dawn`, `morning`, `afternoon`, `evening`)
  - `created_at`: 생성 시각
- 주요 제약/인덱스 포인트:
  - `uq_user_preference_time_slots_user_slot` (`user_id`, `time_slot`) UNIQUE
  - `fk_user_preference_time_slots_user` (`ON DELETE CASCADE`)

### courses

- 목적: 재사용 가능한 러닝 코스 메타데이터를 저장합니다.
- 주요 컬럼 설명:
  - `course_id`: PK
  - `created_by_user_id`: 코스 생성 사용자 FK(관리자/사용자)
  - `title`: 코스 제목
  - `status`: VARCHAR(16) (`official`, `community`, `private`)
  - `distance_m`, `elevation_gain_m`: 코스 거리/고도 지표
  - `start_latitude`, `start_longitude`: 시작 좌표
  - `end_latitude`, `end_longitude`: 종료 좌표
  - `coordinates`: JSONB (경로 좌표 배열: `latitudes`, `longitudes`, `elevations`)
  - `snapshot_image_url`: 코스 스냅샷 이미지
  - `is_deleted`, `promoted_at`: 삭제/승격 상태
  - `created_at`, `updated_at`: 생성/수정 시각
- 주요 제약/인덱스 포인트:
  - `fk_courses_created_by` (`ON DELETE SET NULL`)
  - 거리/고도/좌표 범위 체크 제약

### course_difficulties

- 목적: 코스의 난이도를 1:1 매핑으로 저장합니다.
- 주요 컬럼 설명:
  - `course_id`: PK, `courses.course_id` FK
  - `difficulty`: VARCHAR(16) (`easy`, `medium`, `hard`)
  - `created_at`, `updated_at`: 생성/수정 시각
- 주요 제약/인덱스 포인트:
  - `fk_course_difficulties_course` (`ON DELETE CASCADE`)

### course_surface_maps

- 목적: 코스 다중 선택 노면(`surface_type`) 매핑을 저장합니다.
- 주요 컬럼 설명:
  - `course_surface_map_id`: PK (비식별관계용 독립 PK)
  - `course_id`: `courses.course_id` FK
  - `surface_type`: VARCHAR(16) (`paved`, `unpaved`, `mixed`)
  - `created_at`: 생성 시각
- 주요 제약/인덱스 포인트:
  - `uq_course_surface_maps_course_surface` (`course_id`, `surface_type`) UNIQUE
  - `fk_course_surface_maps_course` (`ON DELETE CASCADE`)

### course_type_maps

- 목적: 코스 다중 선택 타입(`course_type`) 매핑을 저장합니다.
- 주요 컬럼 설명:
  - `course_type_map_id`: PK (비식별관계용 독립 PK)
  - `course_id`: `courses.course_id` FK
  - `course_type`: VARCHAR(32) (`riverside`, `park`, `mountain_trail`, `track`, `urban`, `other`)
  - `created_at`: 생성 시각
- 주요 제약/인덱스 포인트:
  - `uq_course_type_maps_course_type` (`course_id`, `course_type`) UNIQUE
  - `fk_course_type_maps_course` (`ON DELETE CASCADE`)

### running_sessions

- 목적: 자유/코스/고스트 러닝 세션 요약 정보를 저장합니다.
- 주요 컬럼 설명:
  - `running_session_id`: PK
  - `user_id`: 세션 소유 사용자 FK
  - `course_id`: 코스 러닝 시 참조 FK
  - `mode`: VARCHAR(16) (`free`, `course`, `ghost`)
  - `ghost_target_running_session_id`: 고스트 타깃 세션 FK(자기참조)
  - `started_at`, `ended_at`, `duration_sec`: 시간 정보
  - `distance_m`, `avg_speed_mps`, `avg_pace_sec_per_km`: 거리/속도/페이스
  - `calories_kcal`, `elevation_gain_m`: 운동 지표
  - `snapshot_image_url`, `slope_heatmap_url`: 시각화 자원
  - `is_public`, `is_completed`: 공개/완료 상태
  - `created_at`, `updated_at`: 생성/수정 시각
- 주요 제약/인덱스 포인트:
  - `fk_running_sessions_user` (`ON DELETE CASCADE`)
  - `fk_running_sessions_course` (`ON DELETE SET NULL`)
  - `fk_running_sessions_ghost_target` (`ON DELETE SET NULL`)
  - 시간/거리/속도/페이스/칼로리/고도 범위 체크
  - 고스트/코스 모드 전용 체크 (`chk_running_sessions_course_mode`, `chk_running_sessions_ghost_mode`, `chk_running_sessions_visibility_for_ghost`)
  - 완료 상태 체크 (`chk_running_sessions_completed_state`)

### running_session_splits

- 목적: 세션 내 구간(`split`)별 상세 지표를 저장합니다.
- 주요 컬럼 설명:
  - `running_session_split_id`: PK
  - `running_session_id`: `running_sessions.running_session_id` FK
  - `split_index`: 구간 순번
  - `split_distance_m` (0~1000m, 마지막 구간 1km 미만 허용), `split_duration_sec`, `split_pace_sec_per_km`: 구간 지표
  - `avg_heart_rate`, `elevation_gain_m`: 심박/고도 지표
  - `created_at`: 생성 시각
- 주요 제약/인덱스 포인트:
  - `fk_running_session_splits_session` (`ON DELETE CASCADE`)
  - `uq_running_session_splits_index` (`running_session_id`, `split_index`)
  - 구간 순번/거리/시간/페이스/심박/고도 체크 제약

### running_health_samples

- 목적: 5초 주기 생체/주행 샘플 시계열 데이터를 저장합니다.
- 주요 컬럼 설명:
  - `running_health_sample_id`: PK (비식별관계용 독립 PK)
  - `running_session_id`: 세션 FK
  - `sampled_at`: 샘플링 시각
  - `user_id`: 사용자 FK
  - `heart_rate`, `speed_mps`, `pace_sec_per_km`: 핵심 바이탈/속도 지표
  - `distance_m`, `calories_kcal`, `altitude_m`, `cadence_spm`: 누적/센서 지표
  - `source_device`: 데이터 소스(`watch` 기본값)
- 주요 제약/인덱스 포인트:
  - `fk_running_health_samples_session`, `fk_running_health_samples_user` (둘 다 `ON DELETE CASCADE`)
  - `uq_running_health_samples_session_time` (`running_session_id`, `sampled_at`) UNIQUE
  - 심박/속도/페이스/거리/칼로리/케이던스 체크 제약

### running_gps_samples

- 목적: 5초 주기 GPS 시계열 좌표 데이터를 저장합니다.
- 주요 컬럼 설명:
  - `running_gps_sample_id`: PK (비식별관계용 독립 PK)
  - `running_session_id`: 세션 FK
  - `sampled_at`: 샘플링 시각
  - `user_id`: 사용자 FK
  - `latitude`, `longitude`: 좌표
  - `altitude_m`, `accuracy_m`, `heading_deg`: 부가 GPS 지표
- 주요 제약/인덱스 포인트:
  - `fk_running_gps_samples_session`, `fk_running_gps_samples_user` (둘 다 `ON DELETE CASCADE`)
  - `uq_running_gps_samples_session_time` (`running_session_id`, `sampled_at`) UNIQUE
  - 위도/경도/정확도/헤딩 범위 체크 제약

### ghost_running_results

- 목적: 고스트 러닝 결과를 세션 단위로 저장합니다.
- 주요 컬럼 설명:
  - `ghost_running_result_id`: PK
  - `running_session_id`: 결과가 귀속되는 세션 FK
  - `ghost_target_running_session_id`: 비교 대상 세션 FK
  - `target_user_id`: 대상 사용자 FK
  - `result_status`: VARCHAR(16) (`win`, `lose`, `draw`, `incomplete`)
  - `time_gap_sec`, `distance_gap_m`: 결과 차이값
  - `created_at`: 생성 시각
- 주요 제약/인덱스 포인트:
  - `fk_ghost_running_results_session` (`ON DELETE CASCADE`)
  - `fk_ghost_running_results_target_session` (`ON DELETE RESTRICT`)
  - `fk_ghost_running_results_target_user` (`ON DELETE SET NULL`)
  - `uq_ghost_running_results_session` (세션당 1건)
  - 상태값 체크 및 동일 세션 비교 금지 체크

### course_likes

- 목적: 사용자-코스 좋아요/취소 상태를 저장합니다.
- 주요 컬럼 설명:
  - `course_like_id`: PK (비식별관계용 독립 PK)
  - `course_id`, `user_id`: 코스/사용자 FK
  - `is_liked`: 좋아요 상태
  - `updated_at`: 마지막 변경 시각
- 주요 제약/인덱스 포인트:
  - `fk_course_likes_course`, `fk_course_likes_user` (둘 다 `ON DELETE CASCADE`)
  - `uq_course_likes_course_user` (`course_id`, `user_id`) UNIQUE

### course_bookmarks

- 목적: 사용자 북마크 코스를 저장합니다.
- 주요 컬럼 설명:
  - `course_bookmark_id`: PK (비식별관계용 독립 PK)
  - `course_id`, `user_id`: 코스/사용자 FK
  - `created_at`: 북마크 시각
- 주요 제약/인덱스 포인트:
  - `fk_course_bookmarks_course`, `fk_course_bookmarks_user` (둘 다 `ON DELETE CASCADE`)
  - `uq_course_bookmarks_course_user` (`course_id`, `user_id`) UNIQUE

### course_reviews

- 목적: 코스 평점/리뷰 및 제출 난이도 피드백을 저장합니다.
- 주요 컬럼 설명:
  - `course_review_id`: PK
  - `course_id`, `user_id`: 코스/리뷰어 FK
  - `rating`: 1~5 점수
  - `submitted_difficulty`: VARCHAR(16) (`easy`, `medium`, `hard`)
  - `created_at`, `updated_at`: 생성/수정 시각
- 주요 제약/인덱스 포인트:
  - `fk_course_reviews_course`, `fk_course_reviews_user` (둘 다 `ON DELETE CASCADE`)
  - `uq_course_reviews_course_user` (코스당 사용자 1리뷰)
  - `chk_course_reviews_rating` (1~5)

### course_review_course_types

- 목적: 리뷰 시 다중 선택한 코스 타입(`course_type`) 설문 응답을 저장합니다.
- 주요 컬럼 설명:
  - `course_review_course_type_id`: PK (비식별관계용 독립 PK)
  - `course_review_id`: `course_reviews.course_review_id` FK
  - `course_type`: VARCHAR(32) (`riverside`, `park`, `mountain_trail`, `track`, `urban`, `other`)
  - `created_at`: 생성 시각
- 주요 제약/인덱스 포인트:
  - `uq_course_review_course_types_review_type` (`course_review_id`, `course_type`) UNIQUE
  - `fk_course_review_course_types_review` (`ON DELETE CASCADE`)

### user_calendars

- 목적: 사용자 일자별 집계 러닝 통계를 저장합니다.
- 주요 컬럼 설명:
  - `user_calendar_id`: PK (비식별관계용 독립 PK)
  - `user_id`: 사용자 FK
  - `calendar_date`: 날짜
  - `total_distance_m`, `total_duration_sec`, `total_calories_kcal`: 일일 누적 지표
  - `course_run_count`, `free_run_count`, `ghost_run_count`: 모드별 횟수
  - `representative_snapshot_url`: 대표 스냅샷
  - `updated_at`: 마지막 집계 갱신 시각
- 주요 제약/인덱스 포인트:
  - `fk_user_calendars_user` (`ON DELETE CASCADE`)
  - `uq_user_calendars_user_date` (`user_id`, `calendar_date`) UNIQUE
  - 거리/시간/칼로리/횟수 음수 방지 체크 제약

### device_sync_states

- 목적: 사용자별 디바이스 동기화 체크포인트 상태를 저장합니다.
- 주요 컬럼 설명:
  - `device_sync_state_id`: PK
  - `user_id`: 사용자 FK
  - `device_type`: VARCHAR(16) (`watch`, `phone`, `web`)
  - `device_identifier`: 디바이스 식별자
  - `last_synced_sampled_at`, `last_synced_running_session_id`: 마지막 동기화 지점
  - `sync_token`: 증분 동기화 토큰
  - `sync_status`: VARCHAR(16) (`idle`, `syncing`, `error`)
  - `last_error_message`: 최근 오류 메시지
  - `created_at`, `updated_at`: 생성/수정 시각
- 주요 제약/인덱스 포인트:
  - `fk_device_sync_states_user` (`ON DELETE CASCADE`)
  - `fk_device_sync_states_session` (`ON DELETE SET NULL`)
  - `uq_device_sync_states_unique_device` (`user_id`, `device_type`, `device_identifier`)
  - `device_type`, `sync_status` 체크 제약
