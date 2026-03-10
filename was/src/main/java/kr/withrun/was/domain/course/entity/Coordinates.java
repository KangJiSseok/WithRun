package kr.withrun.was.domain.course.entity;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class Coordinates {

    private List<Double> latitudes;
    private List<Double> longitudes;
    private List<Double> elevations;

    public List<Double> getLatitudes() {
        return latitudes;
    }

    public List<Double> getLongitudes() {
        return longitudes;
    }

    public List<Double> getElevations() {
        return elevations;
    }

}
