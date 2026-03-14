package kr.withrun.was.domain.course.controller;

import jakarta.validation.Valid;
import kr.withrun.was.domain.course.dto.CreateCourseReviewRequest;
import kr.withrun.was.domain.course.dto.CreateCourseReviewResponse;
import kr.withrun.was.domain.course.service.CourseReviewService;
import kr.withrun.was.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static kr.withrun.was.global.response.ResponseCode.CREATED;

@Validated
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseReviewController {

    private final CourseReviewService courseReviewService;

    @PostMapping("/{courseId}/reviews")
    public ResponseEntity<ApiResponse<CreateCourseReviewResponse>> createCourseReview(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateCourseReviewRequest request
    ) {
        return ApiResponse.successEntity(CREATED, courseReviewService.createCourseReview(courseId, request));
    }
}
