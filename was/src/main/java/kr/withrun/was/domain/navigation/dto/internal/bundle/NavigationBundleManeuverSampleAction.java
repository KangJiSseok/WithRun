package kr.withrun.was.domain.navigation.dto.internal.bundle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NavigationBundleManeuverSampleAction {
    STRAIGHT("직진"),
    RIGHT("우회전"),
    LEFT("좌회전"),
    UTURN("유턴"),
    ARRIVAL("도착");

    private final String jsonValue;

    NavigationBundleManeuverSampleAction(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String jsonValue() {
        return jsonValue;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static NavigationBundleManeuverSampleAction fromJsonValue(String jsonValue) {
        for (NavigationBundleManeuverSampleAction value : values()) {
            if (value.jsonValue.equals(jsonValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown NavigationBundleManeuverSampleAction: " + jsonValue);
    }
}
