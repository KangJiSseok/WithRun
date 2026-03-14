package kr.withrun.was.domain.course.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;

import java.util.ArrayList;
import java.util.List;

public record Coordinates(
        List<Double> latitudes,
        List<Double> longitudes,
        List<Double> elevations
) {

    public Coordinates {
        validateElevations(elevations);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Coordinates fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isObject()) {
            return createCoordinates(
                    toDoubleList(node.get("latitudes")),
                    toDoubleList(node.get("longitudes")),
                    toDoubleList(node.get("elevations"))
            );
        }

        if (!node.isArray()) {
            throw invalidCoordinatesFormat();
        }

        if (isSinglePointTuple(node)) {
            return fromSinglePointTuple(node);
        }

        if (isParallelListShape(node)) {
            return createCoordinates(
                    toDoubleList(node.get(0)),
                    toDoubleList(node.get(1)),
                    node.size() > 2 ? toDoubleList(node.get(2)) : null
            );
        }

        return fromPointSequence(node);
    }

    private static Coordinates fromSinglePointTuple(JsonNode node) {
        List<Double> latitudes = new ArrayList<>();
        List<Double> longitudes = new ArrayList<>();
        latitudes.add(toDouble(node.get(0)));
        longitudes.add(toDouble(node.get(1)));

        Double elevation = requireElevationValue(node.size() > 2 ? toDouble(node.get(2)) : null);
        return createCoordinates(
                latitudes,
                longitudes,
                elevation == null ? null : new ArrayList<>(List.of(elevation))
        );
    }

    private static Coordinates fromPointSequence(JsonNode node) {
        List<Double> latitudes = new ArrayList<>();
        List<Double> longitudes = new ArrayList<>();
        List<Double> elevations = new ArrayList<>();
        boolean hasElevation = false;

        for (JsonNode pointNode : node) {
            if (pointNode == null || pointNode.isNull()) {
                latitudes.add(null);
                longitudes.add(null);
                elevations.add(null);
                continue;
            }

            if (pointNode.isArray()) {
                latitudes.add(pointNode.size() > 0 ? toDouble(pointNode.get(0)) : null);
                longitudes.add(pointNode.size() > 1 ? toDouble(pointNode.get(1)) : null);
                Double elevation = requireElevationValue(pointNode.size() > 2 ? toDouble(pointNode.get(2)) : null);
                elevations.add(elevation);
                hasElevation = true;
                continue;
            }

            if (pointNode.isObject()) {
                latitudes.add(firstPresent(pointNode, "latitude", "lat"));
                longitudes.add(firstPresent(pointNode, "longitude", "lng", "lon"));
                Double elevation = requireElevationValue(firstPresent(pointNode, "elevation", "altitude", "alt"));
                elevations.add(elevation);
                hasElevation = true;
                continue;
            }

            throw invalidCoordinatesFormat();
        }

        return createCoordinates(latitudes, longitudes, hasElevation ? elevations : null);
    }

    private static boolean isSinglePointTuple(JsonNode node) {
        if (node.size() < 2 || node.size() > 3) {
            return false;
        }

        for (JsonNode valueNode : node) {
            if (!isNumericOrNull(valueNode)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isParallelListShape(JsonNode node) {
        if (node.size() < 2 || node.size() > 3) {
            return false;
        }

        JsonNode latitudeNode = node.get(0);
        JsonNode longitudeNode = node.get(1);
        JsonNode elevationNode = node.size() > 2 ? node.get(2) : null;
        if (!isNumericArray(latitudeNode) || !isNumericArray(longitudeNode)) {
            return false;
        }
        if (elevationNode != null && !elevationNode.isNull() && !isNumericArray(elevationNode)) {
            return false;
        }

        List<Double> latitudeValues = toDoubleList(latitudeNode);
        List<Double> longitudeValues = toDoubleList(longitudeNode);
        if (latitudeValues == null || longitudeValues == null || latitudeValues.size() != longitudeValues.size()) {
            return false;
        }

        return fitsRange(latitudeValues, -90D, 90D) && fitsRange(longitudeValues, -180D, 180D);
    }

    private static boolean isNumericArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return false;
        }

        for (JsonNode elementNode : node) {
            if (!isNumericOrNull(elementNode)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isNumericOrNull(JsonNode node) {
        return node == null || node.isNull() || node.isNumber();
    }

    private static boolean fitsRange(List<Double> values, double minimum, double maximum) {
        for (Double value : values) {
            if (value == null) {
                continue;
            }
            if (value < minimum || value > maximum) {
                return false;
            }
        }

        return true;
    }

    private static Double firstPresent(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode fieldNode = node.get(fieldName);
            if (fieldNode != null && !fieldNode.isNull()) {
                return toDouble(fieldNode);
            }
        }

        return null;
    }

    private static List<Double> toDoubleList(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isArray()) {
            throw invalidCoordinatesFormat();
        }

        List<Double> values = new ArrayList<>();
        for (JsonNode valueNode : node) {
            values.add(toDouble(valueNode));
        }
        return values;
    }

    private static Double toDouble(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isNumber()) {
            throw invalidCoordinateValue();
        }

        return node.doubleValue();
    }

    private static Coordinates createCoordinates(
            List<Double> latitudes,
            List<Double> longitudes,
            List<Double> elevations
    ) {
        validateCoordinateLengths(latitudes, longitudes, elevations);
        return new Coordinates(latitudes, longitudes, elevations);
    }

    private static void validateElevations(List<Double> elevations) {
        if (elevations == null) {
            throw missingCoordinateElevation();
        }

        for (Double elevation : elevations) {
            if (elevation == null) {
                throw missingCoordinateElevation();
            }
        }
    }

    private static void validateCoordinateLengths(
            List<Double> latitudes,
            List<Double> longitudes,
            List<Double> elevations
    ) {
        if (latitudes != null && longitudes != null && latitudes.size() != longitudes.size()) {
            throw invalidCoordinatesLength();
        }
        if (latitudes != null && elevations != null && latitudes.size() != elevations.size()) {
            throw invalidCoordinatesLength();
        }
        if (longitudes != null && elevations != null && longitudes.size() != elevations.size()) {
            throw invalidCoordinatesLength();
        }
    }

    private static Double requireElevationValue(Double elevation) {
        if (elevation == null) {
            throw missingCoordinateElevation();
        }

        return elevation;
    }

    private static CustomException invalidCoordinatesFormat() {
        return new CustomException(ResponseCode.INVALID_COURSE_COORDINATES_FORMAT);
    }

    private static CustomException invalidCoordinatesLength() {
        return new CustomException(ResponseCode.INVALID_COURSE_COORDINATES_LENGTH);
    }

    private static CustomException invalidCoordinateValue() {
        return new CustomException(ResponseCode.INVALID_COURSE_COORDINATE_VALUE);
    }

    private static CustomException missingCoordinateElevation() {
        return new CustomException(ResponseCode.MISSING_COURSE_COORDINATE_ELEVATION);
    }
}
