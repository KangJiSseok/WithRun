-- Core user profile data.
CREATE TABLE users (
  user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nickname VARCHAR(40) NOT NULL,
  birth_date DATE NOT NULL,
  gender VARCHAR(16) NOT NULL,
  height_cm NUMERIC(5, 2) NOT NULL,
  weight_kg NUMERIC(5, 2) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT uq_users_nickname UNIQUE (nickname),
  CONSTRAINT chk_users_birth_date CHECK (birth_date < CURRENT_DATE),
  CONSTRAINT chk_users_gender CHECK (gender IN ('MALE', 'FEMALE')),
  CONSTRAINT chk_users_height CHECK (height_cm > 0 AND height_cm <= 300),
  CONSTRAINT chk_users_weight CHECK (weight_kg > 0 AND weight_kg <= 500)
);

-- Social login account link for each user (1:1 shared PK with users).
CREATE TABLE user_auth_accounts (
  user_id BIGINT PRIMARY KEY,
  provider VARCHAR(16) NOT NULL,
  provider_user_id VARCHAR(191) NOT NULL,
  email VARCHAR(255),
  profile_image_url TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_user_auth_accounts_user
    FOREIGN KEY (user_id)
    REFERENCES users (user_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_user_auth_accounts_provider_identity UNIQUE (provider, provider_user_id)
);

-- Single-select running preferences per user (1:1 shared PK with users).
CREATE TABLE user_running_preferences (
  user_id BIGINT PRIMARY KEY,
  preferred_difficulty VARCHAR(16) NOT NULL,
  preferred_distance_km NUMERIC(5, 2) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_user_running_preferences_user
    FOREIGN KEY (user_id)
    REFERENCES users (user_id)
    ON DELETE CASCADE,
  CONSTRAINT chk_user_running_preferences_difficulty
    CHECK (preferred_difficulty IN ('EASY', 'MEDIUM', 'HARD')),
  CONSTRAINT chk_user_running_preferences_distance
    CHECK (preferred_distance_km > 0 AND preferred_distance_km <= 100)
);

-- Multi-select preference mapping: running purpose.
CREATE TABLE user_preference_purposes (
  user_preference_purpose_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL,
  purpose VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_user_preference_purpose_user_running_preference
    FOREIGN KEY (user_id)
    REFERENCES user_running_preferences (user_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_user_preference_purposes_user_purpose UNIQUE (user_id, purpose),
  CONSTRAINT chk_user_preference_purposes_purpose
    CHECK (purpose IN ('DIET', 'HEALTH_MAINTENANCE', 'RACE_PREPARATION', 'ENDURANCE_IMPROVEMENT'))
);

-- Multi-select preference mapping: preferred course type.
CREATE TABLE user_preference_course_types (
  user_preference_course_type_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL,
  course_type VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_user_preference_course_type_user_running_preference
    FOREIGN KEY (user_id)
    REFERENCES user_running_preferences (user_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_user_preference_course_types_user_type UNIQUE (user_id, course_type),
  CONSTRAINT chk_user_preference_course_types_course_type
    CHECK (course_type IN ('RIVERSIDE', 'PARK', 'MOUNTAIN_TRAIL', 'TRACK', 'URBAN', 'OTHER'))
);

-- Multi-select preference mapping: preferred running time slot.
CREATE TABLE user_preference_time_slots (
  user_preference_time_slot_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL,
  time_slot VARCHAR(16) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_user_preference_time_slot_user_running_preference
    FOREIGN KEY (user_id)
    REFERENCES user_running_preferences (user_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_user_preference_time_slots_user_slot UNIQUE (user_id, time_slot),
  CONSTRAINT chk_user_preference_time_slots_time_slot
    CHECK (time_slot IN ('DAWN', 'MORNING', 'AFTERNOON', 'EVENING'))
);

-- Reusable running course metadata.
CREATE TABLE courses (
  course_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  created_by_user_id BIGINT,
  title VARCHAR(120) NOT NULL,
  status VARCHAR(16) NOT NULL,
  distance_m INTEGER NOT NULL,
  elevation_gain_m INTEGER NOT NULL DEFAULT 0,
  snapshot_image_url TEXT,
  start_latitude NUMERIC(9, 6) NOT NULL,
  start_longitude NUMERIC(9, 6) NOT NULL,
  end_latitude NUMERIC(9, 6),
  end_longitude NUMERIC(9, 6),
  coordinates JSONB,
  promoted_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_courses_created_by
    FOREIGN KEY (created_by_user_id)
    REFERENCES users (user_id)
    ON DELETE SET NULL,
  CONSTRAINT chk_courses_status
    CHECK (status IN ('OFFICIAL', 'COMMUNITY', 'PRIVATE')),
  CONSTRAINT chk_courses_distance CHECK (distance_m > 0),
  CONSTRAINT chk_courses_elevation_gain CHECK (elevation_gain_m >= 0),
  CONSTRAINT chk_courses_start_lat CHECK (start_latitude BETWEEN -90 AND 90),
  CONSTRAINT chk_courses_start_lng CHECK (start_longitude BETWEEN -180 AND 180),
  CONSTRAINT chk_courses_end_lat CHECK (end_latitude IS NULL OR end_latitude BETWEEN -90 AND 90),
  CONSTRAINT chk_courses_end_lng CHECK (end_longitude IS NULL OR end_longitude BETWEEN -180 AND 180)
);

-- Reward item catalog for gacha/inventory metadata.
CREATE TABLE reward_items (
  reward_item_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  title VARCHAR(100) NOT NULL,
  image_url VARCHAR(500) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP
);

CREATE INDEX idx_reward_items_active_created_at ON reward_items (is_active, created_at DESC);

-- Draw pool header for weight-based reward probability management.
CREATE TABLE reward_draw_pools (
  reward_draw_pool_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP
);

CREATE INDEX idx_reward_draw_pools_active_created_at
  ON reward_draw_pools (is_active, created_at DESC, reward_draw_pool_id DESC);

-- Item membership + weight inside a draw pool.
CREATE TABLE reward_draw_pool_items (
  reward_draw_pool_item_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  reward_draw_pool_id BIGINT NOT NULL,
  reward_item_id BIGINT NOT NULL,
  weight INTEGER NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_reward_draw_pool_items_pool
    FOREIGN KEY (reward_draw_pool_id)
    REFERENCES reward_draw_pools (reward_draw_pool_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_reward_draw_pool_items_reward_item
    FOREIGN KEY (reward_item_id)
    REFERENCES reward_items (reward_item_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_reward_draw_pool_items_pool_reward_item UNIQUE (reward_draw_pool_id, reward_item_id),
  CONSTRAINT chk_reward_draw_pool_items_weight CHECK (weight > 0)
);

CREATE INDEX idx_reward_draw_pool_items_pool_created_at
  ON reward_draw_pool_items (reward_draw_pool_id, created_at DESC, reward_draw_pool_item_id DESC);

-- One-row-per-user current reward point balance.
CREATE TABLE reward_point_balances (
  user_id BIGINT PRIMARY KEY,
  current_balance INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_reward_point_balances_user
    FOREIGN KEY (user_id)
    REFERENCES users (user_id)
    ON DELETE CASCADE,
  CONSTRAINT chk_reward_point_balances_current_balance CHECK (current_balance >= 0)
);

-- Append-only reward point accrual and future deduction history.
CREATE TABLE reward_point_histories (
  reward_point_history_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL,
  delta_point INTEGER NOT NULL,
  reason VARCHAR(32) NOT NULL,
  idempotency_key VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_reward_point_histories_user
    FOREIGN KEY (user_id)
    REFERENCES users (user_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_reward_point_histories_user_key UNIQUE (user_id, idempotency_key),
  CONSTRAINT chk_reward_point_histories_delta_point CHECK (delta_point <> 0),
  CONSTRAINT chk_reward_point_histories_reason CHECK (
    reason IN (
      'DAILY_RUNNING',
      'WEEKLY_STREAK_3_DAYS',
      'WEEKLY_STREAK_5_DAYS',
      'WEEKLY_STREAK_7_DAYS',
      'GHOST_WIN',
      'GACHA_DRAW'
    )
  )
);

CREATE INDEX idx_reward_point_histories_user_created_at
  ON reward_point_histories (user_id, created_at DESC, reward_point_history_id DESC);

-- 1:1 course difficulty mapping (shared PK with courses).
CREATE TABLE course_difficulties (
  course_id BIGINT PRIMARY KEY,
  difficulty VARCHAR(16) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_course_difficulties_course
    FOREIGN KEY (course_id)
    REFERENCES courses (course_id)
    ON DELETE CASCADE,
  CONSTRAINT chk_course_difficulties_difficulty
    CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD'))
);

-- Implicit JPA table name for CourseTypeMap.
CREATE TABLE course_type_map (
  course_type_map_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  course_id BIGINT NOT NULL,
  course_type VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_course_type_maps_course
    FOREIGN KEY (course_id)
    REFERENCES courses (course_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_course_type_map_course_type UNIQUE (course_id, course_type),
  CONSTRAINT chk_course_type_map_course_type
    CHECK (course_type IN ('RIVERSIDE', 'PARK', 'MOUNTAIN_TRAIL', 'TRACK', 'URBAN', 'OTHER'))
);

-- Running session summary/read-model data for free/course/ghost runs.
-- COURSE and GHOST must reference courses.status = 'OFFICIAL'; the cross-table status rule is enforced in WAS validation.
CREATE TABLE running_sessions (
  running_session_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL,
  course_id BIGINT,
  mode VARCHAR(16) NOT NULL,
  ghost_target_running_session_id BIGINT,
  started_at TIMESTAMP NOT NULL,
  ended_at TIMESTAMP,
  duration_sec INTEGER,
  distance_m INTEGER NOT NULL DEFAULT 0,
  avg_speed_mps NUMERIC(6, 3),
  avg_pace_sec_per_km INTEGER,
  calories_kcal INTEGER NOT NULL DEFAULT 0,
  elevation_gain_m INTEGER NOT NULL DEFAULT 0,
  snapshot_image_url TEXT,
  start_latitude NUMERIC(9, 6) NOT NULL,
  start_longitude NUMERIC(9, 6) NOT NULL,
  end_latitude NUMERIC(9, 6),
  end_longitude NUMERIC(9, 6),
  coordinates JSONB,
  is_public BOOLEAN NOT NULL DEFAULT FALSE,
  complete_state VARCHAR(16) NOT NULL DEFAULT 'FAIL',
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_running_sessions_user
    FOREIGN KEY (user_id)
    REFERENCES users (user_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_running_sessions_course
    FOREIGN KEY (course_id)
    REFERENCES courses (course_id)
    ON DELETE SET NULL,
  CONSTRAINT fk_running_sessions_ghost_target_running_session
    FOREIGN KEY (ghost_target_running_session_id)
    REFERENCES running_sessions (running_session_id)
    ON DELETE SET NULL,
  CONSTRAINT chk_running_sessions_mode
    CHECK (mode IN ('FREE', 'COURSE', 'GHOST')),
  CONSTRAINT chk_running_sessions_time_range
    CHECK (ended_at IS NULL OR ended_at >= started_at),
  CONSTRAINT chk_running_sessions_duration
    CHECK (duration_sec IS NULL OR duration_sec > 0),
  CONSTRAINT chk_running_sessions_distance
    CHECK (distance_m >= 0),
  CONSTRAINT chk_running_sessions_speed
    CHECK (avg_speed_mps IS NULL OR avg_speed_mps >= 0),
  CONSTRAINT chk_running_sessions_pace
    CHECK (avg_pace_sec_per_km IS NULL OR avg_pace_sec_per_km > 0),
  CONSTRAINT chk_running_sessions_calories
    CHECK (calories_kcal >= 0),
  CONSTRAINT chk_running_sessions_elevation
    CHECK (elevation_gain_m >= 0),
  CONSTRAINT chk_running_sessions_start_lat
    CHECK (start_latitude BETWEEN -90 AND 90),
  CONSTRAINT chk_running_sessions_start_lng
    CHECK (start_longitude BETWEEN -180 AND 180),
  CONSTRAINT chk_running_sessions_end_lat
    CHECK (end_latitude IS NULL OR end_latitude BETWEEN -90 AND 90),
  CONSTRAINT chk_running_sessions_end_lng
    CHECK (end_longitude IS NULL OR end_longitude BETWEEN -180 AND 180),
  CONSTRAINT chk_running_sessions_course_mode
    CHECK ((mode IN ('COURSE', 'GHOST') AND course_id IS NOT NULL) OR mode NOT IN ('COURSE', 'GHOST')),
  CONSTRAINT chk_running_sessions_ghost_mode
    CHECK ((mode = 'GHOST' AND ghost_target_running_session_id IS NOT NULL) OR mode <> 'GHOST'),
  CONSTRAINT chk_running_sessions_visibility_for_ghost
    CHECK ((mode = 'GHOST' AND is_public) OR mode <> 'GHOST'),
  CONSTRAINT chk_running_sessions_complete_state_enum
    CHECK (complete_state IN ('FAIL', 'SUCCESS', 'GIVEUP')),
  CONSTRAINT chk_running_sessions_complete_state
    CHECK ((ended_at IS NULL AND complete_state = 'FAIL') OR (ended_at IS NOT NULL AND complete_state IN ('FAIL', 'SUCCESS', 'GIVEUP')))
);

-- Per-kilometer split read-model details captured during a running session.
CREATE TABLE running_session_splits (
  running_session_split_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  running_session_id BIGINT NOT NULL,
  split_index INTEGER NOT NULL,
  split_distance_m INTEGER NOT NULL,
  split_duration_sec INTEGER NOT NULL,
  split_pace_sec_per_km INTEGER NOT NULL,
  avg_heart_rate INTEGER,
  elevation_gain_m INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_running_session_splits_running_session
    FOREIGN KEY (running_session_id)
    REFERENCES running_sessions (running_session_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_running_session_splits_session_index UNIQUE (running_session_id, split_index),
  CONSTRAINT chk_running_session_splits_index CHECK (split_index > 0),
  CONSTRAINT chk_running_session_splits_distance CHECK (split_distance_m > 0 AND split_distance_m <= 1000),
  CONSTRAINT chk_running_session_splits_duration CHECK (split_duration_sec > 0),
  CONSTRAINT chk_running_session_splits_pace CHECK (split_pace_sec_per_km > 0),
  CONSTRAINT chk_running_session_splits_avg_heart_rate CHECK (avg_heart_rate IS NULL OR avg_heart_rate BETWEEN 20 AND 260),
  CONSTRAINT chk_running_session_splits_elevation CHECK (elevation_gain_m >= 0)
);

-- Append-only 1-second biometrics and performance samples.
-- Hot path: recent 10-minute reads scoped by (running_session_id, sampled_at).
-- Current schema source does not add a dedicated server-side sync checkpoint table.
CREATE TABLE running_health_samples (
  running_health_sample_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  running_session_id BIGINT NOT NULL,
  sampled_at TIMESTAMP NOT NULL,
  user_id BIGINT NOT NULL,
  heart_rate SMALLINT,
  calories_kcal NUMERIC(8, 2),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_running_health_samples_running_session
    FOREIGN KEY (running_session_id)
    REFERENCES running_sessions (running_session_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_running_health_samples_user
    FOREIGN KEY (user_id)
    REFERENCES users (user_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_running_health_samples_session_time UNIQUE (running_session_id, sampled_at),
  CONSTRAINT chk_running_health_samples_heart_rate CHECK (heart_rate IS NULL OR heart_rate BETWEEN 20 AND 260),
  CONSTRAINT chk_running_health_samples_calories CHECK (calories_kcal IS NULL OR calories_kcal >= 0)
);

-- Append-only 1-second GPS coordinate samples.
-- Hot path: recent 10-minute reads scoped by (running_session_id, sampled_at).
-- Current schema source does not add a dedicated server-side sync checkpoint table.
CREATE TABLE running_gps_samples (
  running_gps_sample_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  running_session_id BIGINT NOT NULL,
  sampled_at TIMESTAMP NOT NULL,
  user_id BIGINT NOT NULL,
  latitude NUMERIC(9, 6) NOT NULL,
  longitude NUMERIC(9, 6) NOT NULL,
  altitude_m NUMERIC(7, 2),
  accuracy_m NUMERIC(6, 2),
  bearing_deg NUMERIC(6, 2),
  speed_mps NUMERIC(6, 3),
  pace_sec_per_km INTEGER,
  distance_m INTEGER,
  cadence_spm SMALLINT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_running_gps_samples_running_session
    FOREIGN KEY (running_session_id)
    REFERENCES running_sessions (running_session_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_running_gps_samples_user
    FOREIGN KEY (user_id)
    REFERENCES users (user_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_running_gps_samples_session_time UNIQUE (running_session_id, sampled_at),
  CONSTRAINT chk_running_gps_samples_lat CHECK (latitude BETWEEN -90 AND 90),
  CONSTRAINT chk_running_gps_samples_lng CHECK (longitude BETWEEN -180 AND 180),
  CONSTRAINT chk_running_gps_samples_accuracy CHECK (accuracy_m IS NULL OR accuracy_m >= 0),
  CONSTRAINT chk_running_gps_samples_bearing CHECK (bearing_deg IS NULL OR (bearing_deg >= 0 AND bearing_deg < 360)),
  CONSTRAINT chk_running_gps_samples_speed CHECK (speed_mps IS NULL OR speed_mps >= 0),
  CONSTRAINT chk_running_gps_samples_pace CHECK (pace_sec_per_km IS NULL OR pace_sec_per_km > 0),
  CONSTRAINT chk_running_gps_samples_distance CHECK (distance_m IS NULL OR distance_m >= 0),
  CONSTRAINT chk_running_gps_samples_cadence CHECK (cadence_spm IS NULL OR cadence_spm >= 0)
);

-- Per-course ghost leaderboard rows keyed by user, course, and source session.
CREATE TABLE course_ghost_leaderboard (
  course_ghost_leaderboard_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  running_session_id BIGINT NOT NULL,
  point INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_course_ghost_leaderboard_user_course_session UNIQUE (user_id, course_id, running_session_id),
  CONSTRAINT fk_course_ghost_leaderboard_user
    FOREIGN KEY (user_id)
    REFERENCES users (user_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_course_ghost_leaderboard_course
    FOREIGN KEY (course_id)
    REFERENCES courses (course_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_course_ghost_leaderboard_running_session
    FOREIGN KEY (running_session_id)
    REFERENCES running_sessions (running_session_id)
    ON DELETE CASCADE,
  CONSTRAINT chk_course_ghost_leaderboard_point CHECK (point >= 0)
);

-- Result metrics for ghost-mode sessions.
CREATE TABLE ghost_running_results (
  ghost_running_result_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  running_session_id BIGINT NOT NULL,
  ghost_target_running_session_id BIGINT NOT NULL,
  target_user_id BIGINT,
  result_status VARCHAR(16) NOT NULL,
  time_gap_sec INTEGER NOT NULL DEFAULT 0,
  distance_gap_m INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_ghost_running_results_running_session
    FOREIGN KEY (running_session_id)
    REFERENCES running_sessions (running_session_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_ghost_running_results_ghost_target_running_session
    FOREIGN KEY (ghost_target_running_session_id)
    REFERENCES running_sessions (running_session_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_ghost_running_results_target_user
    FOREIGN KEY (target_user_id)
    REFERENCES users (user_id)
    ON DELETE SET NULL,
  CONSTRAINT uq_ghost_running_results_session UNIQUE (running_session_id),
  CONSTRAINT chk_ghost_running_results_status
    CHECK (result_status IN ('WIN', 'LOSE', 'DRAW', 'INCOMPLETE')),
  CONSTRAINT chk_ghost_running_results_diff_session
    CHECK (running_session_id <> ghost_target_running_session_id)
);

-- Like/unlike state by user for each course.
CREATE TABLE course_likes (
  course_like_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  course_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_course_likes_course
    FOREIGN KEY (course_id)
    REFERENCES courses (course_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_course_likes_user
    FOREIGN KEY (user_id)
    REFERENCES users (user_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_course_likes_course_user UNIQUE (course_id, user_id)
);

-- User bookmarks for quick course access.
CREATE TABLE course_bookmarks (
  course_bookmark_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  course_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_course_bookmarks_course
    FOREIGN KEY (course_id)
    REFERENCES courses (course_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_course_bookmarks_user
    FOREIGN KEY (user_id)
    REFERENCES users (user_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_course_bookmarks_course_user UNIQUE (course_id, user_id)
);

-- Course ratings and difficulty feedback from users.
CREATE TABLE course_reviews (
  course_review_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  course_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  rating SMALLINT NOT NULL,
  submitted_difficulty VARCHAR(16),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_course_reviews_course
    FOREIGN KEY (course_id)
    REFERENCES courses (course_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_course_reviews_user
    FOREIGN KEY (user_id)
    REFERENCES users (user_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_course_reviews_course_user UNIQUE (course_id, user_id),
  CONSTRAINT chk_course_reviews_rating CHECK (rating BETWEEN 1 AND 5),
  CONSTRAINT chk_course_reviews_submitted_difficulty
    CHECK (submitted_difficulty IS NULL OR submitted_difficulty IN ('EASY', 'MEDIUM', 'HARD'))
);

-- Multi-select survey options linked to each course review.
CREATE TABLE course_review_course_types (
  course_review_course_type_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  course_review_id BIGINT NOT NULL,
  course_type VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_course_reviews_course_types_review
    FOREIGN KEY (course_review_id)
    REFERENCES course_reviews (course_review_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_course_review_course_types_review_type UNIQUE (course_review_id, course_type),
  CONSTRAINT chk_course_review_course_types_course_type
    CHECK (course_type IN ('RIVERSIDE', 'PARK', 'MOUNTAIN_TRAIL', 'TRACK', 'URBAN', 'OTHER'))
);

-- Daily aggregate running statistics per user.
CREATE TABLE user_calendars (
  user_calendar_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL,
  calendar_date DATE NOT NULL,
  total_distance_m INTEGER NOT NULL DEFAULT 0,
  total_duration_sec INTEGER NOT NULL DEFAULT 0,
  total_calories_kcal INTEGER NOT NULL DEFAULT 0,
  course_run_count INTEGER NOT NULL DEFAULT 0,
  free_run_count INTEGER NOT NULL DEFAULT 0,
  ghost_run_count INTEGER NOT NULL DEFAULT 0,
  representative_snapshot_url TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_user_calendars_user
    FOREIGN KEY (user_id)
    REFERENCES users (user_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_user_calendars_user_date UNIQUE (user_id, calendar_date),
  CONSTRAINT chk_user_calendars_distance CHECK (total_distance_m >= 0),
  CONSTRAINT chk_user_calendars_duration CHECK (total_duration_sec >= 0),
  CONSTRAINT chk_user_calendars_calories CHECK (total_calories_kcal >= 0),
  CONSTRAINT chk_user_calendars_course_count CHECK (course_run_count >= 0),
  CONSTRAINT chk_user_calendars_free_count CHECK (free_run_count >= 0),
  CONSTRAINT chk_user_calendars_ghost_count CHECK (ghost_run_count >= 0)
);
