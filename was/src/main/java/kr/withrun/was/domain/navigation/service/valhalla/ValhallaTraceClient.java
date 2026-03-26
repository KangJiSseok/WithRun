package kr.withrun.was.domain.navigation.service.valhalla;

import com.fasterxml.jackson.databind.JsonNode;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.MatchedRoutePoint;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.NavigationManeuver;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.RouteEdge;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.SnappedRoutePoint;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaVoiceInstruction;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaRequestPoint;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaRouteTraceResult;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaTraceAttributes;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaTraceRequest;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.service.input.CourseTraceResampler;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
/**
 * Valhalla API 와 통신해 경로 shape 와 속성 정보를 조회하는 도메인 서비스입니다.
 */
public class ValhallaTraceClient {

    private static final int RELAXED_GPS_ACCURACY = 10;
    private static final int RELAXED_SEARCH_RADIUS = 35;
    private static final Logger log = LoggerFactory.getLogger(ValhallaTraceClient.class);

    private final RestClient restClient;
    private final CourseTraceResampler courseTraceResampler;

    /**
     * Valhalla base URL 기준의 RestClient 를 초기화하는 생성자입니다.
     */
    public ValhallaTraceClient(
            @Value("${valhalla.base-url:http://localhost:8002}") String baseUrl,
            CourseTraceResampler courseTraceResampler
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.courseTraceResampler = courseTraceResampler;
    }

    /**
     * trace_route API 를 호출해 스냅된 경로와 안내 문구를 조회합니다.
     */
    public ValhallaRouteTraceResult requestRouteTrace(ValhallaTraceRequest request) {
        List<ValhallaTraceRequest> attempts = List.of(
                request.withTraceOptions(5, 20),
                request.withTraceOptions(RELAXED_GPS_ACCURACY, RELAXED_SEARCH_RADIUS),
                courseTraceResampler.resample(request).withTraceOptions(RELAXED_GPS_ACCURACY, RELAXED_SEARCH_RADIUS)
        );

        NavigationBundleGenerationException lastFailure = null;
        for (ValhallaTraceRequest attempt : attempts) {
            try {
                JsonNode response = restClient.post()
                        .uri("/trace_route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(buildTraceRoutePayload(attempt))
                        .retrieve()
                        .body(JsonNode.class);

                logTraceRouteResponseSummary(response, attempt);
                return parseRouteTraceResponse(response);
            } catch (NavigationBundleGenerationException exception) {
                lastFailure = exception;
            } catch (Exception exception) {
                lastFailure = new NavigationBundleGenerationException(
                        NavigationBundleFailureCode.VALHALLA_TRACE_ROUTE_FAILED,
                        "Failed to call Valhalla trace_route",
                        exception
                );
            }
        }

        if (lastFailure != null) {
            throw lastFailure;
        }

        throw new NavigationBundleGenerationException(
                NavigationBundleFailureCode.VALHALLA_TRACE_ROUTE_FAILED,
                "Valhalla trace_route failed after all retry attempts"
        );
    }

    /**
     * trace_attributes API 를 호출해 엣지/매칭 포인트/거리 정보를 조회합니다.
     */
    public ValhallaTraceAttributes requestTraceAttributes(ValhallaTraceRequest request) {
        try {
            JsonNode response = restClient.post()
                    .uri("/trace_attributes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildTraceAttributesPayload(request))
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new NavigationBundleGenerationException(
                        NavigationBundleFailureCode.VALHALLA_TRACE_ATTRIBUTES_FAILED,
                        "Valhalla trace_attributes response is empty"
                );
            }

            logTraceAttributesResponseSummary(response, request);
            return new ValhallaTraceAttributes(
                    extractEdges(response.path("edges")),
                    extractMatchedPoints(response.path("matched_points")),
                    extractShapeLengths(response.path("shape_attributes").path("length"))
            );
        } catch (NavigationBundleGenerationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.VALHALLA_TRACE_ATTRIBUTES_FAILED,
                    "Failed to call Valhalla trace_attributes",
                    exception
            );
        }
    }

    private ValhallaRouteTraceResult parseRouteTraceResponse(JsonNode response) {
        if (response == null || response.path("trip").isMissingNode()) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.VALHALLA_TRACE_ROUTE_FAILED,
                    "Valhalla trace_route response is missing trip data"
            );
        }

        List<JsonNode> orderedTrips = collectOrderedTrips(response);
        if (orderedTrips.isEmpty()) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.VALHALLA_TRACE_ROUTE_FAILED,
                    "Valhalla trace_route response is missing route legs"
            );
        }

        List<SnappedRoutePoint> flattenedShape = new ArrayList<>();
        List<NavigationManeuver> flattenedManeuvers = new ArrayList<>();

        for (JsonNode tripNode : orderedTrips) {
            JsonNode legsNode = tripNode.path("legs");
            if (!legsNode.isArray() || legsNode.isEmpty()) {
                continue;
            }

            for (JsonNode legNode : legsNode) {
                List<SnappedRoutePoint> legShape = decodePolyline(legNode.path("shape").asText(""));
                if (legShape.isEmpty()) {
                    throw new NavigationBundleGenerationException(
                            NavigationBundleFailureCode.VALHALLA_TRACE_ROUTE_FAILED,
                            "Valhalla trace_route leg shape must not be empty"
                    );
                }

                int shapeIndexOffset = flattenedShape.isEmpty() ? 0 : flattenedShape.size();
                if (!flattenedShape.isEmpty() && isSamePoint(flattenedShape.get(flattenedShape.size() - 1), legShape.get(0))) {
                    shapeIndexOffset -= 1;
                    flattenedShape.addAll(legShape.subList(1, legShape.size()));
                } else {
                    flattenedShape.addAll(legShape);
                }

                JsonNode maneuversNode = legNode.path("maneuvers");
                if (!maneuversNode.isArray()) {
                    continue;
                }
                for (JsonNode maneuverNode : maneuversNode) {
                    flattenedManeuvers.add(new NavigationManeuver(
                            shapeIndexOffset + maneuverNode.path("begin_shape_index").asInt(),
                            shapeIndexOffset + maneuverNode.path("end_shape_index").asInt(),
                            maneuverNode.path("instruction").asText(""),
                            readOptionalText(maneuverNode, "verbal_transition_alert_instruction"),
                            readOptionalText(maneuverNode, "verbal_pre_transition_instruction"),
                            readOptionalText(maneuverNode, "verbal_post_transition_instruction"),
                            readOptionalBoolean(maneuverNode, "verbal_multi_cue"),
                            readOptionalText(maneuverNode, "verbal_succinct_transition_instruction"),
                            readOptionalInt(maneuverNode, "type"),
                            readOptionalInt(maneuverNode, "bearing_before"),
                            readOptionalInt(maneuverNode, "bearing_after"),
                            extractVoiceInstructions(maneuverNode.path("voiceInstructions"))
                    ));
                }
            }
        }

        return new ValhallaRouteTraceResult(List.copyOf(flattenedShape), List.copyOf(flattenedManeuvers));
    }

    private List<JsonNode> collectOrderedTrips(JsonNode response) {
        List<TraceTripSegment> segments = new ArrayList<>();
        segments.add(new TraceTripSegment(response.path("trip"), readOriginalIndex(response.path("trip").path("locations"), 0), 0));

        JsonNode alternatesNode = response.path("alternates");
        if (alternatesNode.isArray()) {
            for (int index = 0; index < alternatesNode.size(); index++) {
                JsonNode alternateTrip = alternatesNode.get(index).path("trip");
                if (!alternateTrip.isMissingNode()) {
                    segments.add(new TraceTripSegment(alternateTrip, readOriginalIndex(alternateTrip.path("locations"), Integer.MAX_VALUE), index + 1));
                }
            }
        }

        return segments.stream()
                .filter(segment -> !segment.tripNode().isMissingNode())
                .sorted(Comparator
                        .comparingInt(TraceTripSegment::startOriginalIndex)
                        .thenComparingInt(TraceTripSegment::sequence))
                .map(TraceTripSegment::tripNode)
                .toList();
    }

    private int readOriginalIndex(JsonNode locationsNode, int fallback) {
        if (!locationsNode.isArray() || locationsNode.isEmpty()) {
            return fallback;
        }

        JsonNode firstLocation = locationsNode.get(0);
        JsonNode originalIndexNode = firstLocation.path("original_index");
        return originalIndexNode.isIntegralNumber() ? originalIndexNode.asInt() : fallback;
    }

    private void logTraceRouteResponseSummary(JsonNode response, ValhallaTraceRequest request) {
        if (!log.isInfoEnabled() || response == null) {
            return;
        }

        List<JsonNode> orderedTrips = collectOrderedTrips(response);
        int tripLegCount = countLegs(response.path("trip"));
        int alternateCount = response.path("alternates").isArray() ? response.path("alternates").size() : 0;
        int totalLegCount = orderedTrips.stream().mapToInt(this::countLegs).sum();
        int totalManeuverCount = orderedTrips.stream().mapToInt(this::countManeuvers).sum();

        log.info(
                "Valhalla trace_route response summary: inputPoints={}, tripLegs={}, alternates={}, orderedTrips={}, totalLegs={}, maneuvers={}, tripStartOriginalIndex={}, alternateStartOriginalIndices={}",
                request.shape().size(),
                tripLegCount,
                alternateCount,
                orderedTrips.size(),
                totalLegCount,
                totalManeuverCount,
                readOriginalIndex(response.path("trip").path("locations"), -1),
                collectAlternateStartOriginalIndices(response.path("alternates"))
        );
    }

    private void logTraceAttributesResponseSummary(JsonNode response, ValhallaTraceRequest request) {
        if (!log.isInfoEnabled() || response == null) {
            return;
        }

        JsonNode matchedPointsNode = response.path("matched_points");
        int matchedPointCount = matchedPointsNode.isArray() ? matchedPointsNode.size() : 0;
        int matchedCount = countMatchedPointsByType(matchedPointsNode, "matched");
        int interpolatedCount = countMatchedPointsByType(matchedPointsNode, "interpolated");
        int unmatchedCount = countMatchedPointsByType(matchedPointsNode, "unmatched");
        int discontinuityCount = countMatchedPointDiscontinuities(matchedPointsNode);

        log.info(
                "Valhalla trace_attributes response summary: inputPoints={}, edges={}, matchedPoints={}, matched={}, interpolated={}, unmatched={}, discontinuities={}, shapeLengths={}",
                request.shape().size(),
                response.path("edges").isArray() ? response.path("edges").size() : 0,
                matchedPointCount,
                matchedCount,
                interpolatedCount,
                unmatchedCount,
                discontinuityCount,
                response.path("shape_attributes").path("length").isArray() ? response.path("shape_attributes").path("length").size() : 0
        );
    }

    private int countLegs(JsonNode tripNode) {
        JsonNode legsNode = tripNode.path("legs");
        return legsNode.isArray() ? legsNode.size() : 0;
    }

    private int countManeuvers(JsonNode tripNode) {
        JsonNode legsNode = tripNode.path("legs");
        if (!legsNode.isArray()) {
            return 0;
        }

        int maneuverCount = 0;
        for (JsonNode legNode : legsNode) {
            JsonNode maneuversNode = legNode.path("maneuvers");
            if (maneuversNode.isArray()) {
                maneuverCount += maneuversNode.size();
            }
        }
        return maneuverCount;
    }

    private List<Integer> collectAlternateStartOriginalIndices(JsonNode alternatesNode) {
        if (!alternatesNode.isArray()) {
            return List.of();
        }

        List<Integer> startOriginalIndices = new ArrayList<>();
        for (JsonNode alternateNode : alternatesNode) {
            startOriginalIndices.add(readOriginalIndex(alternateNode.path("trip").path("locations"), -1));
        }
        return List.copyOf(startOriginalIndices);
    }

    private int countMatchedPointsByType(JsonNode matchedPointsNode, String type) {
        if (!matchedPointsNode.isArray()) {
            return 0;
        }

        int count = 0;
        for (JsonNode matchedPointNode : matchedPointsNode) {
            if (type.equals(matchedPointNode.path("type").asText())) {
                count += 1;
            }
        }
        return count;
    }

    private int countMatchedPointDiscontinuities(JsonNode matchedPointsNode) {
        if (!matchedPointsNode.isArray()) {
            return 0;
        }

        int count = 0;
        for (JsonNode matchedPointNode : matchedPointsNode) {
            if (matchedPointNode.path("begin_route_discontinuity").asBoolean(false)
                    || matchedPointNode.path("end_route_discontinuity").asBoolean(false)) {
                count += 1;
            }
        }
        return count;
    }

    /**
     * 내부 명령 객체를 Valhalla trace_route 요청 바디 형식으로 변환합니다.
     */
    private Map<String, Object> buildTraceRoutePayload(ValhallaTraceRequest request) {
        Map<String, Object> payload = buildCommonPayload(request);
        payload.put("voice_instructions", true);
        payload.put("directions_options", Map.of(
                "language", request.language(),
                "units", request.units()
        ));
        return payload;
    }

    /**
     * 내부 명령 객체를 Valhalla trace_attributes 요청 바디 형식으로 변환합니다.
     */
    private Map<String, Object> buildTraceAttributesPayload(ValhallaTraceRequest request) {
        return buildCommonPayload(request);
    }

    private Map<String, Object> buildCommonPayload(ValhallaTraceRequest request) {
        List<Map<String, Object>> shape = new ArrayList<>();
        for (ValhallaRequestPoint point : request.shape()) {
            shape.add(Map.of(
                    "lat", point.lat(),
                    "lon", point.lon()
            ));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("shape", List.copyOf(shape));
        payload.put("costing", request.costing());
        payload.put("shape_match", request.shapeMatch());
        payload.put("trace_options", Map.of(
                "gps_accuracy", request.gpsAccuracy(),
                "search_radius", request.searchRadius(),
                "turn_penalty_factor", request.turnPenaltyFactor()
        ));
        return payload;
    }

    /**
     * Valhalla polyline 문자열을 위경도 목록으로 복원합니다.
     */
    private List<SnappedRoutePoint> decodePolyline(String encoded) {
        if (encoded.isBlank()) {
            return List.of();
        }

        List<SnappedRoutePoint> decoded = new ArrayList<>();
        int index = 0;
        int latitude = 0;
        int longitude = 0;

        while (index < encoded.length()) {
            int[] latitudeResult = decodeValue(encoded, index);
            latitude += latitudeResult[0];
            index = latitudeResult[1];

            int[] longitudeResult = decodeValue(encoded, index);
            longitude += longitudeResult[0];
            index = longitudeResult[1];

            decoded.add(new SnappedRoutePoint(latitude / 1_000_000d, longitude / 1_000_000d));
        }

        return List.copyOf(decoded);
    }

    /**
     * polyline 한 좌표 성분을 delta 값으로 디코딩합니다.
     */
    private int[] decodeValue(String encoded, int startIndex) {
        int result = 0;
        int shift = 0;
        int index = startIndex;
        int value;

        do {
            value = encoded.charAt(index++) - 63;
            result |= (value & 0x1f) << shift;
            shift += 5;
        } while (value >= 0x20);

        int delta = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
        return new int[]{delta, index};
    }

    private boolean isSamePoint(SnappedRoutePoint left, SnappedRoutePoint right) {
        return Double.compare(left.lat(), right.lat()) == 0
                && Double.compare(left.lon(), right.lon()) == 0;
    }

    /**
     * trace_attributes 응답에서 도로 엣지 정보를 추출합니다.
     */
    private List<RouteEdge> extractEdges(JsonNode edgesNode) {
        if (!edgesNode.isArray()) {
            return List.of();
        }

        List<RouteEdge> edges = new ArrayList<>();
        for (JsonNode edgeNode : edgesNode) {
            List<String> names = new ArrayList<>();
            JsonNode namesNode = edgeNode.path("names");
            if (namesNode.isArray()) {
                for (JsonNode nameNode : namesNode) {
                    names.add(nameNode.asText());
                }
            }

            edges.add(new RouteEdge(
                    edgeNode.path("begin_shape_index").asInt(),
                    edgeNode.path("end_shape_index").asInt(),
                    edgeNode.path("length").asDouble(0d),
                    List.copyOf(names)
            ));
        }

        return List.copyOf(edges);
    }

    /**
     * trace_attributes 응답에서 매칭된 포인트 정보를 추출합니다.
     */
    private List<MatchedRoutePoint> extractMatchedPoints(JsonNode matchedPointsNode) {
        if (!matchedPointsNode.isArray()) {
            return List.of();
        }

        List<MatchedRoutePoint> matchedPoints = new ArrayList<>();
        for (JsonNode matchedPointNode : matchedPointsNode) {
            JsonNode pointNode = matchedPointNode.path("matched_point");
            matchedPoints.add(new MatchedRoutePoint(
                    pointNode.path("lat").asDouble(),
                    pointNode.path("lon").asDouble(),
                    matchedPointNode.path("type").asText(""),
                    matchedPointNode.path("edge_index").isMissingNode() ? null : matchedPointNode.path("edge_index").asInt()
            ));
        }

        return List.copyOf(matchedPoints);
    }

    /**
     * shape 각 구간의 누적 길이 정보를 추출합니다.
     */
    private List<Double> extractShapeLengths(JsonNode shapeLengthsNode) {
        if (!shapeLengthsNode.isArray()) {
            return List.of();
        }

        List<Double> shapeLengths = new ArrayList<>();
        for (JsonNode shapeLengthNode : shapeLengthsNode) {
            shapeLengths.add(shapeLengthNode.asDouble());
        }
        return List.copyOf(shapeLengths);
    }

    private List<ValhallaVoiceInstruction> extractVoiceInstructions(JsonNode voiceInstructionsNode) {
        if (!voiceInstructionsNode.isArray()) {
            return List.of();
        }

        List<ValhallaVoiceInstruction> voiceInstructions = new ArrayList<>();
        for (JsonNode voiceInstructionNode : voiceInstructionsNode) {
            voiceInstructions.add(new ValhallaVoiceInstruction(
                    voiceInstructionNode.path("distanceAlongGeometry").asDouble(0d),
                    voiceInstructionNode.path("announcement").asText(""),
                    voiceInstructionNode.path("ssmlAnnouncement").asText("")
            ));
        }

        return List.copyOf(voiceInstructions);
    }

    private String readOptionalText(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        return fieldNode.isMissingNode() || fieldNode.isNull() ? null : fieldNode.asText();
    }

    private Integer readOptionalInt(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        return fieldNode.isMissingNode() || fieldNode.isNull() ? null : fieldNode.asInt();
    }

    private Boolean readOptionalBoolean(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        return fieldNode.isMissingNode() || fieldNode.isNull() ? null : fieldNode.asBoolean();
    }

    private record TraceTripSegment(JsonNode tripNode, int startOriginalIndex, int sequence) {
    }
}
