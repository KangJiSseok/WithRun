package kr.withrun.was.domain.running.dto;

import kr.withrun.was.domain.running.entity.RunningHealthSample;

import java.time.LocalDateTime;

public record RunningHealthSampleResponse(
        LocalDateTime sampledAt,
        Short heartRate,
        Double speedMps,
        Integer paceSecPerKm,
        Integer distanceM,
        Double caloriesKcal,
        Short cadenceSpm
) {
    public static RunningHealthSampleResponse from(RunningHealthSample runningHealthSample) {
        return new RunningHealthSampleResponse(
                runningHealthSample.getSampledAt(),
                runningHealthSample.getHeartRate(),
                runningHealthSample.getSpeedMps(),
                runningHealthSample.getPaceSecPerKm(),
                runningHealthSample.getDistanceM(),
                runningHealthSample.getCaloriesKcal(),
                runningHealthSample.getCadenceSpm()
        );
    }
}
