package com.mobile.watch

import androidx.health.services.client.data.ExerciseTrackedStatus

internal object WatchServiceLifecyclePolicy {
    fun shouldStartWorkout(action: String?): Boolean {
        return action == ExerciseSessionService.ACTION_START
    }

    fun shouldRecoverService(action: String?): Boolean {
        return action == null
    }

    fun shouldAttemptStop(@ExerciseTrackedStatus trackedStatus: Int): Boolean {
        return trackedStatus == ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS
    }
}
