package kr.withrun.was.domain.course.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CourseType {
    RIVERSIDE(1, "RIVERSIDE"),
    PARK(2, "PARK"),
    MOUNTAIN_TRAIL(3, "MOUNTAIN_TRAIL"),
    TRACK(4, "TRACK"),
    URBAN(5, "URBAN"),
    OTHER(6, "OTHER");

    private final int index;
    private final String label;

}
