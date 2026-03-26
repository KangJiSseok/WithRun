package kr.withrun.was.domain.navigation.dto.internal.bundle;

import java.time.Instant;

/**
 * 저장된 navigation bundle 의 메타데이터를 표현합니다.
 */
public record NavigationBundleMetadata(
        double distanceMeters,
        Instant generatedAt,
        String valhallaVersion,
        String language
) {
}
