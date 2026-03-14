package kr.withrun.was.domain.course.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.course.dto.BookmarkedCoursesRequest;
import kr.withrun.was.domain.course.dto.BookmarkedCoursesResponse;
import kr.withrun.was.domain.course.dto.CourseBookmarkAddResponse;
import kr.withrun.was.domain.course.dto.CourseBookmarkRemoveResponse;
import kr.withrun.was.domain.course.service.CourseBookmarkService;
import kr.withrun.was.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

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

    @GetMapping("/bookmarks/{userId}")
    public ResponseEntity<ApiResponse<BookmarkedCoursesResponse>> findBookmarkedCourses(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") @Positive @Max(50) Integer size,
            @RequestParam(required = false) String cursor
    ) {
        return ApiResponse.successEntity(
                courseBookmarkService.findBookmarkedCourses(
                        new BookmarkedCoursesRequest(userId, size, cursor)
                )
        );
    }
}
