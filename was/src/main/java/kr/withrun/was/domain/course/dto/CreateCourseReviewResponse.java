package kr.withrun.was.domain.course.dto;

import kr.withrun.was.domain.course.entity.CourseReview;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.global.common.type.Difficulty;

import java.time.LocalDateTime;
import java.util.List;

public record CreateCourseReviewResponse(
        Long courseReviewId,
        Long courseId,
        Integer rating,
        List<CourseType> courseTypes,
        Difficulty submittedDifficulty,
        LocalDateTime createdAt
) {

    public static CreateCourseReviewResponse from(CourseReview courseReview, List<CourseType> courseTypes) {
        return new CreateCourseReviewResponse(
                courseReview.getId(),
                courseReview.getCourse().getId(),
                courseReview.getRating(),
                courseTypes,
                courseReview.getDifficulty(),
                courseReview.getCreatedAt()
        );
    }
}
