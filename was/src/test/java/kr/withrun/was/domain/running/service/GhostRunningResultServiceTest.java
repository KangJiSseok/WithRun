package kr.withrun.was.domain.running.service;

import kr.withrun.was.domain.running.dto.GhostRunningResultResponse;
import kr.withrun.was.domain.running.repository.GhostRunningResultRepository;
import kr.withrun.was.domain.running.repository.query.dto.GhostRunningResultRow;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("고스트 러닝 결과 서비스")
class GhostRunningResultServiceTest {

    @Mock
    private GhostRunningResultRepository ghostRunningResultRepository;

    @DisplayName("고스트 러닝 결과 조회 시 결과 상세를 응답한다")
    @Test
    void returnsGhostRunningResultWhenResultExists() {
        GhostRunningResultService ghostRunningResultService = new GhostRunningResultService(ghostRunningResultRepository);
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 8, 8, 15);
        GhostRunningResultRow ghostRunningResult = new GhostRunningResultRow(
                77L,
                901L,
                345L,
                202L,
                GhostResultStatus.WIN,
                14,
                32,
                createdAt
        );
        when(ghostRunningResultRepository.findByRunningSessionId(901L))
                .thenReturn(Optional.of(ghostRunningResult));

        GhostRunningResultResponse response = ghostRunningResultService.findGhostRunningResult(901L);

        assertThat(response).isEqualTo(new GhostRunningResultResponse(
                77L,
                901L,
                345L,
                202L,
                GhostResultStatus.WIN,
                14,
                32,
                createdAt
        ));
    }

    @DisplayName("고스트 러닝 결과가 없으면 GHOST_RUNNING_RESULT_NOT_FOUND 예외를 던진다")
    @Test
    void throwsGhostRunningResultNotFoundWhenResultDoesNotExist() {
        GhostRunningResultService ghostRunningResultService = new GhostRunningResultService(ghostRunningResultRepository);
        when(ghostRunningResultRepository.findByRunningSessionId(901L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ghostRunningResultService.findGhostRunningResult(901L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.GHOST_RUNNING_RESULT_NOT_FOUND);
    }

}
