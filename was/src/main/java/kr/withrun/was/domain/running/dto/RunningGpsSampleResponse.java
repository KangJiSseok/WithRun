package kr.withrun.was.domain.running.dto;

import kr.withrun.was.domain.running.entity.RunningGpsSample;

import java.time.LocalDateTime;

public record RunningGpsSampleResponse(
        LocalDateTime sampledAt,
        Double latitude,
        Double longitude,
        Double altitudeM,
        Double accuracyM,
        Double bearingDeg
) {
    public static RunningGpsSampleResponse from(RunningGpsSample runningGpsSample) {
        return new RunningGpsSampleResponse(
                runningGpsSample.getSampledAt(),
                runningGpsSample.getLatitude(),
                runningGpsSample.getLongitude(),
                runningGpsSample.getAltitudeM(),
                runningGpsSample.getAccuracyM(),
                runningGpsSample.getBearingDeg()
        );
    }
}
