-- Minimal PostgreSQL bootstrap for GET /api/courses/nearby
-- Covers the exact query path used by NearbyCourseQueryService:
--   users -> courses -> course_difficulties -> course_type_map
--
-- Coordinates are seeded as array-shaped JSON to match the current runtime fix:
--   [[latitude, longitude, elevation], ...]

BEGIN;

CREATE TABLE IF NOT EXISTS users (
  user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nickname VARCHAR(40) NOT NULL,
  birth_date DATE NOT NULL,
  gender VARCHAR(16) NOT NULL,
  height_cm NUMERIC(5, 2) NOT NULL,
  weight_kg NUMERIC(5, 2) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT uq_users_nickname UNIQUE (nickname),
  CONSTRAINT chk_users_birth_date CHECK (birth_date < CURRENT_DATE),
  CONSTRAINT chk_users_gender CHECK (gender IN ('MALE', 'FEMALE')),
  CONSTRAINT chk_users_height CHECK (height_cm > 0 AND height_cm <= 300),
  CONSTRAINT chk_users_weight CHECK (weight_kg > 0 AND weight_kg <= 500)
);

CREATE TABLE IF NOT EXISTS courses (
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

CREATE TABLE IF NOT EXISTS course_difficulties (
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

CREATE TABLE IF NOT EXISTS course_type_map (
  course_type_map_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  course_id BIGINT NOT NULL,
  course_type VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_course_type_map_course
    FOREIGN KEY (course_id)
    REFERENCES courses (course_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_course_type_map_course_type UNIQUE (course_id, course_type),
  CONSTRAINT chk_course_type_map_course_type
    CHECK (course_type IN ('RIVERSIDE', 'PARK', 'MOUNTAIN_TRAIL', 'TRACK', 'URBAN', 'OTHER'))
);

INSERT INTO users (
  nickname,
  birth_date,
  gender,
  height_cm,
  weight_kg,
  is_active,
  created_at,
  updated_at,
  deleted_at
)
VALUES (
  'nearby-course-seeder',
  DATE '1997-03-21',
  'MALE',
  178.00,
  72.50,
  TRUE,
  NOW(),
  NOW(),
  NULL
)
ON CONFLICT (nickname) DO UPDATE
SET birth_date = EXCLUDED.birth_date,
    gender = EXCLUDED.gender,
    height_cm = EXCLUDED.height_cm,
    weight_kg = EXCLUDED.weight_kg,
    is_active = EXCLUDED.is_active,
    updated_at = NOW(),
    deleted_at = NULL;

WITH seed_user AS (
  SELECT user_id
  FROM users
  WHERE nickname = 'nearby-course-seeder'
), seeded_courses AS (
  INSERT INTO courses (
    created_by_user_id,
    title,
    status,
    distance_m,
    elevation_gain_m,
    snapshot_image_url,
    start_latitude,
    start_longitude,
    end_latitude,
    end_longitude,
    coordinates,
    promoted_at,
    created_at,
    updated_at,
    deleted_at
  )
  SELECT
    seed_user.user_id,
    course_seed.title,
    course_seed.status,
    course_seed.distance_m,
    course_seed.elevation_gain_m,
    course_seed.snapshot_image_url,
    course_seed.start_latitude,
    course_seed.start_longitude,
    course_seed.end_latitude,
    course_seed.end_longitude,
    course_seed.coordinates,
    NOW(),
    NOW(),
    NOW(),
    course_seed.deleted_at
  FROM seed_user
  CROSS JOIN (
    VALUES
      (
        'Han River Riverside 10K',
        'OFFICIAL',
        10000,
        18,
        'https://example.com/course/han-river-riverside-10k.png',
        37.566500::NUMERIC(9, 6),
        126.978000::NUMERIC(9, 6),
        37.570800::NUMERIC(9, 6),
        126.984800::NUMERIC(9, 6),
        JSONB '[[37.5665,126.9780,8.0],[37.5678,126.9801,8.5],[37.5691,126.9822,9.0],[37.5708,126.9848,8.2]]',
        NULL::TIMESTAMP
      ),
      (
        'Yeouido Park Tempo 9K',
        'OFFICIAL',
        9000,
        22,
        'https://example.com/course/yeouido-park-tempo-9k.png',
        37.528200::NUMERIC(9, 6),
        126.932400::NUMERIC(9, 6),
        37.531600::NUMERIC(9, 6),
        126.936900::NUMERIC(9, 6),
        JSONB '[[37.5282,126.9324,11.0],[37.5291,126.9338,12.0],[37.5303,126.9353,11.5],[37.5316,126.9369,10.8]]',
        NULL::TIMESTAMP
      ),
      (
        'Seokchon Lake Easy 5K',
        'OFFICIAL',
        5000,
        10,
        'https://example.com/course/seokchon-lake-easy-5k.png',
        37.508700::NUMERIC(9, 6),
        127.100100::NUMERIC(9, 6),
        37.510400::NUMERIC(9, 6),
        127.104300::NUMERIC(9, 6),
        JSONB '[[37.5087,127.1001,14.0],[37.5093,127.1017,14.1],[37.5099,127.1030,14.3],[37.5104,127.1043,14.0]]',
        NULL::TIMESTAMP
      ),
      (
        'Namsan Hill Repeats 11K',
        'OFFICIAL',
        11000,
        140,
        'https://example.com/course/namsan-hill-repeats-11k.png',
        37.551200::NUMERIC(9, 6),
        126.988200::NUMERIC(9, 6),
        37.558100::NUMERIC(9, 6),
        126.995700::NUMERIC(9, 6),
        JSONB '[[37.5512,126.9882,38.0],[37.5530,126.9900,52.0],[37.5554,126.9925,79.0],[37.5581,126.9957,102.0]]',
        NULL::TIMESTAMP
      ),
      (
        'Jamsil Track Intervals',
        'OFFICIAL',
        10000,
        6,
        'https://example.com/course/jamsil-track-intervals.png',
        37.515800::NUMERIC(9, 6),
        127.073000::NUMERIC(9, 6),
        37.516100::NUMERIC(9, 6),
        127.073400::NUMERIC(9, 6),
        JSONB '[[37.5158,127.0730,19.0],[37.5160,127.0732,19.1],[37.5161,127.0734,19.0]]',
        NULL::TIMESTAMP
      ),
      (
        'Jamsil Stadium Quick Loop 1K',
        'OFFICIAL',
        10000,
        4,
        'https://example.com/course/jamsil-stadium-quick-loop-1k.png',
        37.515900::NUMERIC(9, 6),
        127.072700::NUMERIC(9, 6),
        37.516400::NUMERIC(9, 6),
        127.073300::NUMERIC(9, 6),
        JSONB '[[37.5158,127.0730,19.0],[37.5160,127.0729,18.9],[37.5162,127.0731,19.0],[37.5164,127.0733,18.8]]',
        NULL::TIMESTAMP
      ),
      (
        'Olympic Park One K Starter',
        'OFFICIAL',
        10900,
        7,
        'https://example.com/course/olympic-park-one-k-starter.png',
        37.517000::NUMERIC(9, 6),
        127.073600::NUMERIC(9, 6),
        37.517700::NUMERIC(9, 6),
        127.074200::NUMERIC(9, 6),
        JSONB '[[37.5170,127.0736,20.1],[37.5158,127.0730,19.0],[37.5174,127.0740,20.2],[37.5177,127.0742,20.0]]',
        NULL::TIMESTAMP
      ),
      (
        'Sports Complex Recovery 1K',
        'OFFICIAL',
        9990,
        3,
        'https://example.com/course/sports-complex-recovery-1k.png',
        37.515500::NUMERIC(9, 6),
        127.072200::NUMERIC(9, 6),
        37.516000::NUMERIC(9, 6),
        127.072900::NUMERIC(9, 6),
        JSONB '[[37.5155,127.0722,18.2],[37.5158,127.0730,19.0],[37.5158,127.0727,18.4],[37.5160,127.0729,18.2]]',
        NULL::TIMESTAMP
      ),
      (
        'Songpa Riverside Short 1.2K',
        'OFFICIAL',
        1200,
        5,
        'https://example.com/course/songpa-riverside-short-1-2k.png',
        37.516300::NUMERIC(9, 6),
        127.073800::NUMERIC(9, 6),
        37.517100::NUMERIC(9, 6),
        127.074500::NUMERIC(9, 6),
        JSONB '[[37.5163,127.0738,19.4],[37.5165,127.0740,19.6],[37.5168,127.0742,19.7],[37.5171,127.0745,19.5]]',
        NULL::TIMESTAMP
      ),
      (
        'Jamsil Warmup Out and Back 1.1K',
        'OFFICIAL',
        1100,
        4,
        'https://example.com/course/jamsil-warmup-out-and-back-1-1k.png',
        37.516000::NUMERIC(9, 6),
        127.072900::NUMERIC(9, 6),
        37.516800::NUMERIC(9, 6),
        127.073700::NUMERIC(9, 6),
        JSONB '[[37.5160,127.0729,18.9],[37.5162,127.0731,19.1],[37.5165,127.0734,19.2],[37.5168,127.0737,19.0]]',
        NULL::TIMESTAMP
      ),
      (
        'Bundang Community 10K',
        'COMMUNITY',
        10000,
        24,
        'https://example.com/course/bundang-community-10k.png',
        37.378400::NUMERIC(9, 6),
        127.112200::NUMERIC(9, 6),
        37.382100::NUMERIC(9, 6),
        127.116800::NUMERIC(9, 6),
        JSONB '[[37.3784,127.1122,25.0],[37.3796,127.1138,25.5],[37.3809,127.1151,25.2],[37.3821,127.1168,24.9]]',
        NULL::TIMESTAMP
      ),
      (
        'Deleted Test Course',
        'OFFICIAL',
        10000,
        15,
        'https://example.com/course/deleted-test-course.png',
        37.570000::NUMERIC(9, 6),
        126.990000::NUMERIC(9, 6),
        37.571500::NUMERIC(9, 6),
        126.992000::NUMERIC(9, 6),
        JSONB '[[37.5700,126.9900,10.0],[37.5715,126.9920,10.2]]',
        NOW()
      )
  ) AS course_seed(
    title,
    status,
    distance_m,
    elevation_gain_m,
    snapshot_image_url,
    start_latitude,
    start_longitude,
    end_latitude,
    end_longitude,
    coordinates,
    deleted_at
  )
  WHERE NOT EXISTS (
    SELECT 1
    FROM courses existing_course
    WHERE existing_course.title = course_seed.title
  )
  RETURNING course_id, title
)
SELECT COUNT(*)
FROM seeded_courses;

INSERT INTO course_difficulties (course_id, difficulty, created_at, updated_at, deleted_at)
SELECT
  course.course_id,
  difficulty_seed.difficulty,
  NOW(),
  NOW(),
  NULL
FROM courses course
JOIN (
  VALUES
    ('Han River Riverside 10K', 'EASY'),
    ('Yeouido Park Tempo 9K', 'MEDIUM'),
    ('Seokchon Lake Easy 5K', 'EASY'),
    ('Namsan Hill Repeats 11K', 'HARD'),
    ('Jamsil Track Intervals', 'MEDIUM'),
    ('Jamsil Stadium Quick Loop 1K', 'EASY'),
    ('Olympic Park One K Starter', 'EASY'),
    ('Sports Complex Recovery 1K', 'EASY'),
    ('Songpa Riverside Short 1.2K', 'EASY'),
    ('Jamsil Warmup Out and Back 1.1K', 'EASY'),
    ('Bundang Community 10K', 'MEDIUM'),
    ('Deleted Test Course', 'EASY')
) AS difficulty_seed(title, difficulty)
  ON difficulty_seed.title = course.title
ON CONFLICT (course_id) DO UPDATE
SET difficulty = EXCLUDED.difficulty,
    updated_at = NOW(),
    deleted_at = NULL;

INSERT INTO course_type_map (course_id, course_type, created_at, updated_at, deleted_at)
SELECT
  course.course_id,
  type_seed.course_type,
  NOW(),
  NOW(),
  NULL
FROM courses course
JOIN (
  VALUES
    ('Han River Riverside 10K', 'RIVERSIDE'),
    ('Han River Riverside 10K', 'PARK'),
    ('Yeouido Park Tempo 9K', 'PARK'),
    ('Yeouido Park Tempo 9K', 'URBAN'),
    ('Seokchon Lake Easy 5K', 'PARK'),
    ('Namsan Hill Repeats 11K', 'MOUNTAIN_TRAIL'),
    ('Namsan Hill Repeats 11K', 'URBAN'),
    ('Jamsil Track Intervals', 'TRACK'),
    ('Jamsil Stadium Quick Loop 1K', 'TRACK'),
    ('Olympic Park One K Starter', 'PARK'),
    ('Sports Complex Recovery 1K', 'TRACK'),
    ('Songpa Riverside Short 1.2K', 'RIVERSIDE'),
    ('Jamsil Warmup Out and Back 1.1K', 'TRACK'),
    ('Bundang Community 10K', 'URBAN'),
    ('Deleted Test Course', 'OTHER')
) AS type_seed(title, course_type)
  ON type_seed.title = course.title
ON CONFLICT (course_id, course_type) DO UPDATE
SET updated_at = NOW(),
    deleted_at = NULL;

COMMIT;

-- Example request after seeding:
-- GET /api/courses/nearby?latitude=37.5665&longitude=126.9780&radiusM=5000&targetDistanceM=10000
