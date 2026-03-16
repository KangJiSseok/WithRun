package kr.withrun.was.domain.running.controller;

import jakarta.validation.Valid;
import kr.withrun.was.domain.running.dto.CreateRunningGpsSampleRequest;
import kr.withrun.was.domain.running.dto.CreateRunningGpsSampleResponse;
import kr.withrun.was.domain.running.entity.RunningGpsSample;
import kr.withrun.was.domain.running.service.RunningGpsSampleService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/running-sessions")
@RequiredArgsConstructor
public class RunningGpsSampleController {

    final private RunningGpsSampleService runningGpsSampleService;


}