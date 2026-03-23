-- Dummy data for testing ghost-detail and ghost-leaderboard APIs on course_id = 100.
-- Target endpoints:
--   GET /api/courses/100/ghost-detail/{userId}
--   GET /api/courses/100/ghost-leaderboard?size=20

BEGIN;

DROP TABLE IF EXISTS temp_course_100_ghost_seed;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM courses
    WHERE course_id = 100
      AND deleted_at IS NULL
  ) THEN
    RAISE EXCEPTION 'course_id 100 does not exist or is deleted';
  END IF;
END
$$;

CREATE TEMP TABLE temp_course_100_ghost_seed (
  nickname VARCHAR(40) PRIMARY KEY,
  birth_date DATE NOT NULL,
  gender VARCHAR(16) NOT NULL,
  height_cm NUMERIC(5, 2) NOT NULL,
  weight_kg NUMERIC(5, 2) NOT NULL,
  started_at TIMESTAMP NOT NULL,
  ended_at TIMESTAMP NOT NULL,
  duration_sec INTEGER NOT NULL,
  distance_m INTEGER NOT NULL,
  avg_speed_mps NUMERIC(6, 3) NOT NULL,
  avg_pace_sec_per_km INTEGER NOT NULL,
  calories_kcal INTEGER NOT NULL,
  elevation_gain_m INTEGER NOT NULL,
  start_latitude NUMERIC(9, 6) NOT NULL,
  start_longitude NUMERIC(9, 6) NOT NULL,
  end_latitude NUMERIC(9, 6) NOT NULL,
  end_longitude NUMERIC(9, 6) NOT NULL,
  snapshot_image_url TEXT NOT NULL,
  point INTEGER NOT NULL,
  leaderboard_created_at TIMESTAMP NOT NULL
);

INSERT INTO temp_course_100_ghost_seed (
  nickname,
  birth_date,
  gender,
  height_cm,
  weight_kg,
  started_at,
  ended_at,
  duration_sec,
  distance_m,
  avg_speed_mps,
  avg_pace_sec_per_km,
  calories_kcal,
  elevation_gain_m,
  start_latitude,
  start_longitude,
  end_latitude,
  end_longitude,
  snapshot_image_url,
  point,
  leaderboard_created_at
)
VALUES
  (
    'ghost-course100-alpha',
    DATE '1995-02-14',
    'MALE',
    178.00,
    69.50,
    TIMESTAMP '2026-03-21 06:30:00',
    TIMESTAMP '2026-03-21 06:51:00',
    1260,
    10020,
    7.952,
    126,
    740,
    38,
    37.566100,
    126.973800,
    37.570200,
    126.981400,
    'https://seed.withrun.local/ghost-api/course-100/ghost-course100-alpha.png',
    1320,
    TIMESTAMP '2026-03-21 06:52:00'
  ),
  (
    'ghost-course100-bravo',
    DATE '1997-07-22',
    'FEMALE',
    165.00,
    54.20,
    TIMESTAMP '2026-03-21 07:00:00',
    TIMESTAMP '2026-03-21 07:22:30',
    1350,
    10010,
    7.415,
    135,
    705,
    41,
    37.566100,
    126.973800,
    37.570200,
    126.981400,
    'https://seed.withrun.local/ghost-api/course-100/ghost-course100-bravo.png',
    1250,
    TIMESTAMP '2026-03-21 07:23:00'
  ),
  (
    'ghost-course100-charlie',
    DATE '1993-11-03',
    'MALE',
    182.00,
    77.80,
    TIMESTAMP '2026-03-21 07:10:00',
    TIMESTAMP '2026-03-21 07:32:30',
    1350,
    10040,
    7.437,
    134,
    730,
    45,
    37.566100,
    126.973800,
    37.570200,
    126.981400,
    'https://seed.withrun.local/ghost-api/course-100/ghost-course100-charlie.png',
    1250,
    TIMESTAMP '2026-03-21 07:33:00'
  ),
  (
    'ghost-course100-delta',
    DATE '1998-01-09',
    'FEMALE',
    170.00,
    60.40,
    TIMESTAMP '2026-03-21 07:40:00',
    TIMESTAMP '2026-03-21 08:03:40',
    1420,
    10060,
    7.085,
    141,
    760,
    43,
    37.566100,
    126.973800,
    37.570200,
    126.981400,
    'https://seed.withrun.local/ghost-api/course-100/ghost-course100-delta.png',
    1120,
    TIMESTAMP '2026-03-21 08:04:00'
  ),
  (
    'ghost-course100-echo',
    DATE '1991-05-17',
    'MALE',
    175.50,
    71.30,
    TIMESTAMP '2026-03-21 08:10:00',
    TIMESTAMP '2026-03-21 08:34:00',
    1440,
    10000,
    6.944,
    144,
    790,
    49,
    37.566100,
    126.973800,
    37.570200,
    126.981400,
    'https://seed.withrun.local/ghost-api/course-100/ghost-course100-echo.png',
    980,
    TIMESTAMP '2026-03-21 08:34:30'
  ),
  (
    'ghost-course100-foxtrot',
    DATE '1999-09-28',
    'FEMALE',
    168.40,
    58.70,
    TIMESTAMP '2026-03-21 08:40:00',
    TIMESTAMP '2026-03-21 09:06:00',
    1560,
    10030,
    6.429,
    155,
    820,
    52,
    37.566100,
    126.973800,
    37.570200,
    126.981400,
    'https://seed.withrun.local/ghost-api/course-100/ghost-course100-foxtrot.png',
    840,
    TIMESTAMP '2026-03-21 09:06:30'
  );

INSERT INTO users (
  nickname,
  birth_date,
  gender,
  height_cm,
  weight_kg,
  created_at,
  updated_at,
  deleted_at
)
SELECT
  seed.nickname,
  seed.birth_date,
  seed.gender,
  seed.height_cm,
  seed.weight_kg,
  NOW(),
  NOW(),
  NULL
FROM temp_course_100_ghost_seed seed
ON CONFLICT (nickname) DO UPDATE
SET birth_date = EXCLUDED.birth_date,
    gender = EXCLUDED.gender,
    height_cm = EXCLUDED.height_cm,
    weight_kg = EXCLUDED.weight_kg,
    updated_at = NOW(),
    deleted_at = NULL;

DELETE FROM course_ghost_leaderboard leaderboard
USING users user_row
JOIN temp_course_100_ghost_seed seed
  ON seed.nickname = user_row.nickname
WHERE leaderboard.user_id = user_row.user_id
  AND leaderboard.course_id = 100;

DELETE FROM running_sessions session_row
USING users user_row
JOIN temp_course_100_ghost_seed seed
  ON seed.nickname = user_row.nickname
WHERE session_row.user_id = user_row.user_id
  AND session_row.course_id = 100
  AND session_row.snapshot_image_url = seed.snapshot_image_url;

WITH inserted_sessions AS (
  INSERT INTO running_sessions (
    user_id,
    course_id,
    mode,
    ghost_target_running_session_id,
    started_at,
    ended_at,
    duration_sec,
    distance_m,
    avg_speed_mps,
    avg_pace_sec_per_km,
    calories_kcal,
    elevation_gain_m,
    snapshot_image_url,
    start_latitude,
    start_longitude,
    end_latitude,
    end_longitude,
    coordinates,
    is_public,
    is_completed,
    created_at,
    updated_at,
    deleted_at
  )
  SELECT
    user_row.user_id,
    100,
    'COURSE',
    NULL,
    seed.started_at,
    seed.ended_at,
    seed.duration_sec,
    seed.distance_m,
    seed.avg_speed_mps,
    seed.avg_pace_sec_per_km,
    seed.calories_kcal,
    seed.elevation_gain_m,
    seed.snapshot_image_url,
    seed.start_latitude,
    seed.start_longitude,
    seed.end_latitude,
    seed.end_longitude,
    NULL,
    FALSE,
    TRUE,
    seed.ended_at,
    seed.ended_at,
    NULL
  FROM temp_course_100_ghost_seed seed
  JOIN users user_row
    ON user_row.nickname = seed.nickname
  RETURNING running_session_id, user_id, snapshot_image_url
)
INSERT INTO course_ghost_leaderboard (
  user_id,
  course_id,
  running_session_id,
  point,
  created_at
)
SELECT
  inserted_sessions.user_id,
  100,
  inserted_sessions.running_session_id,
  seed.point,
  seed.leaderboard_created_at
FROM inserted_sessions
JOIN temp_course_100_ghost_seed seed
  ON seed.snapshot_image_url = inserted_sessions.snapshot_image_url;

-- Verification query: returns the seeded user IDs and ready-to-call URLs.
WITH ranked_rows AS (
  SELECT
    leaderboard.course_ghost_leaderboard_id,
    leaderboard.user_id,
    leaderboard.running_session_id,
    leaderboard.point,
    leaderboard.created_at,
    RANK() OVER (ORDER BY leaderboard.point DESC) AS rank
  FROM course_ghost_leaderboard leaderboard
  WHERE leaderboard.course_id = 100
), seeded_rows AS (
  SELECT
    user_row.user_id,
    user_row.nickname,
    ranked_rows.running_session_id,
    ranked_rows.course_ghost_leaderboard_id,
    ranked_rows.point,
    ranked_rows.rank,
    ranked_rows.created_at
  FROM temp_course_100_ghost_seed seed
  JOIN users user_row
    ON user_row.nickname = seed.nickname
  JOIN ranked_rows
    ON ranked_rows.user_id = user_row.user_id
)
SELECT
  seeded_rows.user_id,
  seeded_rows.nickname,
  seeded_rows.running_session_id,
  seeded_rows.course_ghost_leaderboard_id,
  seeded_rows.point,
  seeded_rows.rank,
  seeded_rows.created_at,
  '/api/courses/100/ghost-detail/' || seeded_rows.user_id AS ghost_detail_path,
  '/api/courses/100/ghost-leaderboard?size=20' AS ghost_leaderboard_path
FROM seeded_rows
ORDER BY seeded_rows.point DESC, seeded_rows.course_ghost_leaderboard_id ASC;

DROP TABLE IF EXISTS temp_course_100_ghost_seed;

COMMIT;
