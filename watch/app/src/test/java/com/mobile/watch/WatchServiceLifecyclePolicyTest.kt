package com.mobile.watch

import androidx.health.services.client.data.ExerciseTrackedStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchServiceLifecyclePolicyTest {

    @Test
    fun does_not_start_new_workout_for_null_command() {
        assertFalse(WatchServiceLifecyclePolicy.shouldStartWorkout(action = null))
        assertTrue(WatchServiceLifecyclePolicy.shouldRecoverService(action = null))
    }

    @Test
    fun starts_workout_only_for_start_action() {
        assertTrue(WatchServiceLifecyclePolicy.shouldStartWorkout(ExerciseSessionService.ACTION_START))
        assertFalse(WatchServiceLifecyclePolicy.shouldRecoverService(ExerciseSessionService.ACTION_START))
    }

    @Test
    fun stops_exercise_only_when_owned_exercise_is_active() {
        assertTrue(
            WatchServiceLifecyclePolicy.shouldAttemptStop(
                ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS,
            ),
        )
        assertFalse(
            WatchServiceLifecyclePolicy.shouldAttemptStop(
                ExerciseTrackedStatus.NO_EXERCISE_IN_PROGRESS,
            ),
        )
        assertFalse(
            WatchServiceLifecyclePolicy.shouldAttemptStop(
                ExerciseTrackedStatus.OTHER_APP_IN_PROGRESS,
            ),
        )
    }
}
