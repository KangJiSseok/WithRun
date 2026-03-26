package kr.withrun.was.domain.navigation.service.valhalla;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.NavigationManeuver;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.SnappedRoutePoint;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaVoiceInstruction;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaRequestPoint;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaRouteTraceResult;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaTraceRequest;
import kr.withrun.was.domain.navigation.service.input.CourseTraceResampler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Valhalla trace client")
class ValhallaTraceClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("trace_route 는 고정된 재시도 사다리와 한국어 directions 옵션을 사용한다")
    @Test
    void retriesTraceRouteWithRelaxedOptionsAndResampledInput() throws Exception {
        List<String> requestBodies = new ArrayList<>();
        AtomicInteger requestCount = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/trace_route", exchange -> {
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            int current = requestCount.incrementAndGet();
            if (current < 3) {
                writeJson(exchange, 200, "{}");
                return;
            }

            writeJson(exchange, 200, """
                    {
                      "trip": {
                        "legs": [
                          {
                            "shape": "%s",
                            "maneuvers": [
                              {"begin_shape_index": 0, "end_shape_index": 2, "instruction": "직진하세요"}
                            ]
                          }
                        ]
                      }
                    }
                    """.formatted(encodeShape(List.of(
                    new SnappedRoutePoint(37.5665, 126.9780),
                    new SnappedRoutePoint(37.5671, 126.9792),
                    new SnappedRoutePoint(37.5701, 126.9824)
            ))));
        });
        server.start();

        try {
            ValhallaTraceClient client = new ValhallaTraceClient(
                    "http://localhost:" + server.getAddress().getPort(),
                    new CourseTraceResampler()
            );

            ValhallaTraceRequest request = new ValhallaTraceRequest(
                    List.of(
                            new ValhallaRequestPoint(37.5665000, 126.9780000),
                            new ValhallaRequestPoint(37.5665004, 126.9780004),
                            new ValhallaRequestPoint(37.5665008, 126.9780008),
                            new ValhallaRequestPoint(37.5671000, 126.9792000),
                            new ValhallaRequestPoint(37.5701000, 126.9824000)
                    ),
                    "pedestrian",
                    "map_snap",
                    5,
                    20,
                    500,
                    "ko-KR",
                    "kilometers"
            );

            ValhallaRouteTraceResult result = client.requestRouteTrace(request);

            assertThat(result.shape()).hasSize(3);
            assertThat(requestBodies).hasSize(3);

            JsonNode firstRequest = objectMapper.readTree(requestBodies.get(0));
            JsonNode secondRequest = objectMapper.readTree(requestBodies.get(1));
            JsonNode thirdRequest = objectMapper.readTree(requestBodies.get(2));

            assertThat(firstRequest.path("shape_match").asText()).isEqualTo("map_snap");
            assertThat(firstRequest.path("costing").asText()).isEqualTo("pedestrian");
            assertThat(firstRequest.path("directions_options").path("language").asText()).isEqualTo("ko-KR");
            assertThat(firstRequest.path("directions_options").path("units").asText()).isEqualTo("kilometers");
            assertThat(firstRequest.path("trace_options").path("gps_accuracy").asInt()).isEqualTo(5);
            assertThat(firstRequest.path("trace_options").path("search_radius").asInt()).isEqualTo(20);
            assertThat(firstRequest.path("shape").get(0).has("type")).isFalse();

            assertThat(secondRequest.path("trace_options").path("gps_accuracy").asInt()).isEqualTo(10);
            assertThat(secondRequest.path("trace_options").path("search_radius").asInt()).isEqualTo(35);

            assertThat(thirdRequest.path("trace_options").path("gps_accuracy").asInt()).isEqualTo(10);
            assertThat(thirdRequest.path("trace_options").path("search_radius").asInt()).isEqualTo(35);
            assertThat(thirdRequest.path("shape").size()).isLessThan(firstRequest.path("shape").size());
        } finally {
            server.stop(0);
        }
    }

    @DisplayName("여러 leg 가 와도 하나의 연속 경로로 평탄화한다")
    @Test
    void flattensMultipleLegsIntoSingleLogicalRoute() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/trace_route", exchange -> writeJson(exchange, 200, """
                {
                  "trip": {
                    "legs": [
                      {
                        "shape": "%s",
                        "maneuvers": [
                          {
                            "begin_shape_index": 0,
                            "end_shape_index": 2,
                            "instruction": "출발 후 직진",
                            "verbal_transition_alert_instruction": "잠시 후 직진",
                            "verbal_pre_transition_instruction": "직진하세요",
                            "verbal_post_transition_instruction": "계속 직진하세요",
                            "verbal_multi_cue": true,
                            "verbal_succinct_transition_instruction": "직진",
                            "type": 1,
                            "bearing_before": 15,
                            "bearing_after": 30,
                            "voiceInstructions": [
                              {
                                "distanceAlongGeometry": 120.0,
                                "announcement": "120m 앞 직진",
                                "ssmlAnnouncement": "<speak>120m 앞 직진</speak>"
                              }
                            ]
                          }
                        ]
                      },
                      {
                        "shape": "%s",
                        "maneuvers": [
                          {
                            "begin_shape_index": 0,
                            "end_shape_index": 1,
                            "instruction": "좌회전",
                            "verbal_transition_alert_instruction": "잠시 후 좌회전",
                            "verbal_pre_transition_instruction": "좌회전하세요",
                            "verbal_post_transition_instruction": "좌회전 후 계속 이동하세요",
                            "verbal_multi_cue": false,
                            "verbal_succinct_transition_instruction": "좌회전",
                            "type": 8,
                            "bearing_before": 30,
                            "bearing_after": 275,
                            "voiceInstructions": [
                              {
                                "distanceAlongGeometry": 40.0,
                                "announcement": "40m 앞 좌회전",
                                "ssmlAnnouncement": "<speak>40m 앞 좌회전</speak>"
                              }
                            ]
                          }
                        ]
                      }
                    ]
                  }
                }
                """.formatted(
                encodeShape(List.of(
                        new SnappedRoutePoint(37.5665, 126.9780),
                        new SnappedRoutePoint(37.5671, 126.9792),
                        new SnappedRoutePoint(37.5680, 126.9800)
                )),
                encodeShape(List.of(
                        new SnappedRoutePoint(37.5680, 126.9800),
                        new SnappedRoutePoint(37.5690, 126.9812)
                ))
        )));
        server.start();

        try {
            ValhallaTraceClient client = new ValhallaTraceClient(
                    "http://localhost:" + server.getAddress().getPort(),
                    new CourseTraceResampler()
            );

            ValhallaRouteTraceResult result = client.requestRouteTrace(new ValhallaTraceRequest(
                    List.of(
                            new ValhallaRequestPoint(37.5665, 126.9780),
                            new ValhallaRequestPoint(37.5690, 126.9812)
                    ),
                    "pedestrian",
                    "map_snap",
                    5,
                    20,
                    500,
                    "ko-KR",
                    "kilometers"
            ));

            assertThat(result.shape()).extracting(SnappedRoutePoint::lat, SnappedRoutePoint::lon)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(37.5665, 126.9780),
                            org.assertj.core.groups.Tuple.tuple(37.5671, 126.9792),
                            org.assertj.core.groups.Tuple.tuple(37.5680, 126.9800),
                            org.assertj.core.groups.Tuple.tuple(37.5690, 126.9812)
                    );
            assertThat(result.maneuvers()).containsExactly(
                    new NavigationManeuver(
                            0,
                            2,
                            "출발 후 직진",
                            "잠시 후 직진",
                            "직진하세요",
                            "계속 직진하세요",
                            true,
                            "직진",
                            1,
                            15,
                            30,
                            List.of(new ValhallaVoiceInstruction(120.0, "120m 앞 직진", "<speak>120m 앞 직진</speak>"))
                    ),
                    new NavigationManeuver(
                            2,
                            3,
                            "좌회전",
                            "잠시 후 좌회전",
                            "좌회전하세요",
                            "좌회전 후 계속 이동하세요",
                            false,
                            "좌회전",
                            8,
                            30,
                            275,
                            List.of(new ValhallaVoiceInstruction(40.0, "40m 앞 좌회전", "<speak>40m 앞 좌회전</speak>"))
                    )
            );
        } finally {
            server.stop(0);
        }
    }

    private static void writeJson(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static String encodeShape(List<SnappedRoutePoint> points) {
        StringBuilder encoded = new StringBuilder();
        long previousLatitude = 0;
        long previousLongitude = 0;
        for (SnappedRoutePoint point : points) {
            long latitude = Math.round(point.lat() * 1_000_000);
            long longitude = Math.round(point.lon() * 1_000_000);
            encodeValue(encoded, latitude - previousLatitude);
            encodeValue(encoded, longitude - previousLongitude);
            previousLatitude = latitude;
            previousLongitude = longitude;
        }
        return encoded.toString();
    }

    private static void encodeValue(StringBuilder encoded, long value) {
        long shifted = value < 0 ? ~(value << 1) : (value << 1);
        while (shifted >= 0x20) {
            encoded.append((char) ((0x20 | (shifted & 0x1f)) + 63));
            shifted >>= 5;
        }
        encoded.append((char) (shifted + 63));
    }
}
