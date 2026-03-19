package kr.withrun.was.global.common.type;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "체감 난이도 분류")
public enum Difficulty {
    @Schema(description = "쉬운 난이도")
    EASY(1, "EASY"),

    @Schema(description = "보통 난이도")
    MEDIUM(2, "MEDIUM"),

    @Schema(description = "어려운 난이도")
    HARD(3, "HARD");

    private final int index;
    private final String label;
    
}
