package kr.withrun.was.domain.course.dto;

import java.util.List;

public record CourseSurveyResponse(
        List<CourseSurveyListResponse> courseTypes,
        List<CourseSurveyListResponse> difficulties
) {
    public record CourseSurveyListResponse(
            Integer index,
            String label
    ) {
    }
}
