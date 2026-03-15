package kr.withrun.was.domain.running.util;

import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("러닝 세션 히스토리 커서 코덱")
class RunningSessionHistoryCursorCodecTest {

    @DisplayName("시작 시각과 러닝 세션 ID를 커서로 인코딩하고 복원한다")
    @Test
    void encodesAndDecodesStartedAtAndRunningSessionId() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 3, 15, 19, 30, 15);

        String cursor = RunningSessionHistoryCursorCodec.encode(startedAt, 120L);
        RunningSessionHistoryCursorCodec.CursorPayload payload = RunningSessionHistoryCursorCodec.decode(cursor);

        assertThat(payload.startedAt()).isEqualTo(startedAt);
        assertThat(payload.runningSessionId()).isEqualTo(120L);
    }

    @DisplayName("형식이 잘못된 커서는 INVALID_CURSOR 예외를 던진다")
    @Test
    void rejectsMalformedCursor() {
        assertThatThrownBy(() -> RunningSessionHistoryCursorCodec.decode("%%%"))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_CURSOR);
    }
}
