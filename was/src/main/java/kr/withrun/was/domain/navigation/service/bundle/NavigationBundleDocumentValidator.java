package kr.withrun.was.domain.navigation.service.bundle;

import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleDocument;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuver;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
/**
 * 생성된 latest navigation bundle 문서가 최소 요구사항을 만족하는지 검증합니다.
 */
public class NavigationBundleDocumentValidator {

    /**
     * shape, edge, 거리 정보가 유효한지 확인하고 문제가 있으면 예외를 던집니다.
     */
    public void validate(NavigationBundleDocument document) {
        if (document.shape() == null || document.shape().size() < 2) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                    "Latest navigation bundle shape must contain at least two points"
            );
        }

        if (document.metadata() == null || document.metadata().distanceMeters() <= 0d) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                    "Latest navigation bundle metadata distance must be positive"
            );
        }

        validateManeuvers(document.maneuvers(), document.shape().size(), document.metadata().distanceMeters());

        if (document.shapeLengths() != null && document.shapeLengths().stream().anyMatch(length -> length == null || length < 0d)) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                    "Latest navigation bundle shape lengths must be non-negative"
            );
        }
    }

    private void validateManeuvers(List<NavigationBundleManeuver> maneuvers, int shapeSize, double totalDistanceMeters) {
        if (maneuvers == null || maneuvers.isEmpty()) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                    "Latest navigation bundle maneuvers must not be empty"
            );
        }

        int previousEndShapeIndex = -1;
        double previousEndDistanceMeters = -1d;
        for (NavigationBundleManeuver maneuver : maneuvers) {
            if (maneuver.beginShapeIndex() < 0
                    || maneuver.endShapeIndex() < maneuver.beginShapeIndex()
                    || maneuver.endShapeIndex() >= shapeSize) {
                throw new NavigationBundleGenerationException(
                        NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                        "Latest navigation bundle maneuver shape range is invalid"
                );
            }
            if (maneuver.beginShapeIndex() < previousEndShapeIndex) {
                throw new NavigationBundleGenerationException(
                        NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                        "Latest navigation bundle maneuvers must be ordered"
                );
            }
            if (maneuver.startDistanceMeters() > maneuver.endDistanceMeters()
                    || maneuver.endDistanceMeters() < previousEndDistanceMeters
                    || maneuver.distanceMeters() < 0d) {
                throw new NavigationBundleGenerationException(
                        NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                        "Latest navigation bundle maneuver distances are invalid"
                );
            }

            previousEndShapeIndex = maneuver.endShapeIndex();
            previousEndDistanceMeters = maneuver.endDistanceMeters();
        }

        if (maneuvers.get(0).beginShapeIndex() != 0 || maneuvers.get(maneuvers.size() - 1).endShapeIndex() != shapeSize - 1) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                    "Latest navigation bundle maneuvers must cover the full route"
            );
        }

        if (Math.abs(maneuvers.get(maneuvers.size() - 1).cumulativeDistanceMeters() - totalDistanceMeters) > 1d) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                    "Latest navigation bundle total distance and maneuver coverage must match"
            );
        }
    }
}
