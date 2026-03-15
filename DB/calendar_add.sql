WITH created_user AS (
    INSERT INTO users (
                       nickname,
                       birth_date,
                       gender,
                       height_cm,
                       weight_kg
        )
        VALUES (
                   'wit135_dummy_runner',
                   DATE '1995-03-11',
                   'MALE',
                   175.0,
                   68.0
               )
        RETURNING user_id
),
     inserted_calendars AS (
         INSERT INTO user_calendars (
                                     user_id,
                                     calendar_date,
                                     total_distance_m,
                                     total_duration_sec,
                                     total_calories_kcal,
                                     course_run_count,
                                     free_run_count,
                                     ghost_run_count,
                                     representative_snapshot_url
             )
             SELECT
                 cu.user_id,
                 seeded.calendar_date,
                 seeded.total_distance_m,
                 seeded.total_duration_sec,
                 seeded.total_calories_kcal,
                 seeded.course_run_count,
                 seeded.free_run_count,
                 seeded.ghost_run_count,
                 seeded.representative_snapshot_url
             FROM created_user cu
                      CROSS JOIN (
                 VALUES
                     (CURRENT_DATE,     5000, 1800, 320, 1, 0, 0, 'https://example.com/day0.png'),
                     (CURRENT_DATE - 1, 6200, 2100, 390, 0, 1, 0, 'https://example.com/day1.png'),
                     (CURRENT_DATE - 2, 7300, 2400, 450, 0, 0, 1, 'https://example.com/day2.png'),
                     -- CURRENT_DATE - 3 intentionally missing -> current streak ends at 3
                     (CURRENT_DATE - 4, 4000, 1500, 280, 1, 1, 0, 'https://example.com/day4.png'),
                     (CURRENT_DATE - 5, 4100, 1600, 290, 0, 1, 0, 'https://example.com/day5.png'),
                     (CURRENT_DATE - 6, 5200, 1900, 330, 1, 0, 1, 'https://example.com/day6.png'),
                     (CURRENT_DATE - 7, 3300, 1400, 240, 0, 0, 1, 'https://example.com/day7.png')
             ) AS seeded (
                          calendar_date,
                          total_distance_m,
                          total_duration_sec,
                          total_calories_kcal,
                          course_run_count,
                          free_run_count,
                          ghost_run_count,
                          representative_snapshot_url
                 )
     )
SELECT user_id
FROM created_user;