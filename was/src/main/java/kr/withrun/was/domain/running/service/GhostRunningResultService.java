package kr.withrun.was.domain.running.service;

import kr.withrun.was.domain.running.dto.GhostRunningResultResponse;
import kr.withrun.was.domain.running.repository.GhostRunningResultRepository;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GhostRunningResultService {

    private final GhostRunningResultRepository ghostRunningResultRepository;

    public GhostRunningResultResponse findGhostRunningResult(Long runningSessionId) {
        return ghostRunningResultRepository.findByRunningSessionId(runningSessionId)
                .map(GhostRunningResultResponse::from)
                .orElseThrow(() -> new CustomException(ResponseCode.GHOST_RUNNING_RESULT_NOT_FOUND));
    }

}
