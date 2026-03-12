package kr.withrun.was.domain.course.controller;

import kr.withrun.was.domain.course.dto.CourseBookmarkAddResponse;
import kr.withrun.was.domain.course.dto.CourseBookmarkRemoveResponse;
import kr.withrun.was.domain.course.dto.CourseLikeStatusResponse;
import kr.withrun.was.domain.course.service.CourseBookmarkService;
import kr.withrun.was.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseBookmarkController {

    private final CourseBookmarkService courseBookmarkService;

    @PostMapping("/{courseId}/bookmark/{userId}")
    public ResponseEntity<ApiResponse<CourseBookmarkAddResponse>> addBookmarkCourse(
            @PathVariable Long courseId,
            @PathVariable Long userId
    ) {
        return ApiResponse.successEntity(courseBookmarkService.addBookmark(courseId, userId));
    }

    @DeleteMapping("/{courseId}/bookmark/{userId}")
    public ResponseEntity<ApiResponse<CourseBookmarkRemoveResponse>> removeBookmarkCourse(
            @PathVariable Long courseId,
            @PathVariable Long userId
    ) {
        return ApiResponse.successEntity(courseBookmarkService.removeBookmark(courseId, userId));
    }
}
