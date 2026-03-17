package kr.withrun.was.domain.running.dto;

import java.time.LocalDateTime;

public record CreateRunningHealthSampleRequest (
        LocalDateTime sampledAt,

        Short heartRate,

        Double speedMps,

        Integer paceSecPerKm,

        Integer distanceM,
        
        Double caloriesKcal,

        Short cadenceSpm
) {
}
