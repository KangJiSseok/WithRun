package kr.withrun.was.domain.course.controller;

import kr.withrun.was.domain.course.dto.CourseLikeStatusResponse;
import kr.withrun.was.domain.course.dto.CourseSurveyResponse;
import kr.withrun.was.domain.course.service.CourseLikeService;
import kr.withrun.was.domain.course.service.CourseService;
import kr.withrun.was.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static kr.withrun.was.global.response.ResponseCode.CREATED;

@Validated
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseLikeController {

    private final CourseLikeService courseLikeService;

    @PostMapping("/{courseId}/like/{userId}")
    public ResponseEntity<ApiResponse<CourseLikeStatusResponse>> likeCourse(
            @PathVariable Long courseId,
            @PathVariable Long userId
    ) {
        return ApiResponse.successEntity(courseLikeService.likeCourse(courseId, userId));
    }

    @DeleteMapping("/{courseId}/like/{userId}")
    public ResponseEntity<ApiResponse<CourseLikeStatusResponse>> unlikeCourse(
            @PathVariable Long courseId,
            @PathVariable Long userId
    ) {
        return ApiResponse.successEntity(courseLikeService.unlikeCourse(courseId, userId));
    }
}
