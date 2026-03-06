package com.runninghealth.wear.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.runninghealth.wear.data.HealthPayload
import com.runninghealth.wear.exercise.ExerciseRepository
import kotlinx.coroutines.flow.StateFlow

class ExerciseViewModel : ViewModel() {
    val exerciseState: StateFlow<String> = ExerciseRepository.exerciseState
    val lastPayload: StateFlow<HealthPayload?> = ExerciseRepository.lastPayload
    val capabilityState: StateFlow<String> = ExerciseRepository.capabilityState
    val permissionState: StateFlow<String> = ExerciseRepository.permissionState

    fun updatePermissionState(granted: Boolean) {
        ExerciseRepository.permissionState.value = if (granted) "GRANTED" else "DENIED"
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ExerciseViewModel::class.java)) {
                        return modelClass.cast(ExerciseViewModel())
                            ?: throw IllegalStateException("Failed to create ExerciseViewModel")
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
