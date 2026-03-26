package kr.withrun.was.domain.navigation.dto.internal.valhalla;

/**
 * Valhalla 요청에 전달하는 위경도 좌표 한 점을 표현합니다.
 */
public record ValhallaRequestPoint(
        double lat,
        double lon
) {
}
