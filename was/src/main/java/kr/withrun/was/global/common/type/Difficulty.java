package kr.withrun.was.global.common.type;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Difficulty {
    EASY(1, "EASY"),
    MEDIUM(2, "MEDIUM"),
    HARD(3, "HARD");

    private final int index;
    private final String label;
    
}
