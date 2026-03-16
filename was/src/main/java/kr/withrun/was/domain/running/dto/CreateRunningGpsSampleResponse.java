package kr.withrun.was.domain.running.dto;

import kr.withrun.was.domain.running.entity.RunningGpsSample;
import kr.withrun.was.domain.running.entity.RunningSession;

import java.time.LocalDateTime;

public record CreateRunningGpsSampleResponse(
        Long runningGpsSampleId,
        Long userId,
        Long runningSessionId,
        Double bearingDeg,
        Double accuracyM,
        Double altitudeM,
        Double longitude,
        Double latitude,
        LocalDateTime sampledAt
) {

    public static CreateRunningGpsSampleResponse from(RunningGpsSample runningGpsSample) {
        return new CreateRunningGpsSampleResponse(
                runningGpsSample.getId(),
                runningGpsSample.getUser().getId(),
                runningGpsSample.getRunningSession().getId(),
                runningGpsSample.getBearingDeg(),
                runningGpsSample.getAccuracyM(),
                runningGpsSample.getAltitudeM(),
                runningGpsSample.getLongitude(),
                runningGpsSample.getLatitude(),
                runningGpsSample.getSampledAt()
        );
    }

}
