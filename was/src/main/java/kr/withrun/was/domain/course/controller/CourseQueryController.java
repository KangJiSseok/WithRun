package kr.withrun.was.domain.course.controller;

import jakarta.validation.Valid;
import kr.withrun.was.domain.course.dto.NearbyCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyCoursesResponse;
import kr.withrun.was.domain.course.service.NearbyCourseQueryService;
import kr.withrun.was.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseQueryController {

    private final NearbyCourseQueryService nearbyCourseQueryService;

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<NearbyCoursesResponse>> findNearbyCourses(
            @Valid @ModelAttribute NearbyCoursesRequest request
    ) {
        return ApiResponse.successEntity(nearbyCourseQueryService.findNearbyCourses(request));
    }
}
