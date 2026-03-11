package kr.withrun.was.domain.course.util;

import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class NearbyCourseCursorCodec {

    private static final String DELIMITER = ":";

    private NearbyCourseCursorCodec() {
    }

    public static String encode(int lastDistanceFromUserM, long lastCourseId) {
        String payload = lastDistanceFromUserM + DELIMITER + lastCourseId;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public static CursorPayload decode(String cursor) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split(DELIMITER, -1);
            if (parts.length != 2) {
                throw invalidCursor();
            }

            int lastDistanceFromUserM = Integer.parseInt(parts[0]);
            long lastCourseId = Long.parseLong(parts[1]);
            return new CursorPayload(lastDistanceFromUserM, lastCourseId);
        } catch (IllegalArgumentException exception) {
            throw invalidCursor();
        }
    }

    private static CustomException invalidCursor() {
        return new CustomException(ResponseCode.INVALID_CURSOR);
    }

    public record CursorPayload(int lastDistanceFromUserM, long lastCourseId) {
    }
}
