package kr.withrun.was.domain.navigation.service.bundle;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleDocument;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleGenerationResult;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaRequestPoint;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaRouteTraceResult;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaTraceAttributes;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaTraceRequest;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.service.artifact.NavigationBundleStore;
import kr.withrun.was.domain.navigation.service.input.CourseTraceRequestMapper;
import kr.withrun.was.domain.navigation.service.valhalla.ValhallaTraceClient;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import kr.withrun.was.domain.navigation.type.NavigationBundleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("최신 네비게이션 번들 생성 서비스")
class NavigationBundleGenerationServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseTraceRequestMapper courseTraceRequestMapper;

    @Mock
    private ValhallaTraceClient valhallaTraceClient;

    @Mock
    private NavigationBundleDocumentBuilder navigationBundleDocumentBuilder;

    @Mock
    private NavigationBundleDocumentValidator navigationBundleDocumentValidator;

    @Mock
    private NavigationBundleStore navigationBundleStore;

    private NavigationBundleGenerationService navigationBundleGenerationService;

    @BeforeEach
    void setUp() {
        navigationBundleGenerationService = new NavigationBundleGenerationService(
                courseRepository,
                courseTraceRequestMapper,
                valhallaTraceClient,
                navigationBundleDocumentBuilder,
                navigationBundleDocumentValidator,
                navigationBundleStore,
                "https://cdn.withrun.kr",
                true
        );
    }

    @DisplayName("코스 기준으로 latest bundle 생성 파이프라인을 완성한다")
    @Test
    void generatesLatestNavigationBundle() {
        Course course = course(10L);
        ValhallaTraceRequest traceRequest = new ValhallaTraceRequest(
                List.of(new ValhallaRequestPoint(37.5, 127.0), new ValhallaRequestPoint(37.6, 127.1)),
                "pedestrian",
                "map_snap",
                5,
                20,
                500,
                "ko-KR",
                "kilometers"
        );
        ValhallaRouteTraceResult routeTraceResult = new ValhallaRouteTraceResult(List.of(), List.of());
        ValhallaTraceAttributes traceAttributes = new ValhallaTraceAttributes(List.of(), List.of(), List.of(12.0));
        NavigationBundleDocument document = new NavigationBundleDocument(
                List.of(),
                List.of(),
                new kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleMetadata(
                        1200d,
                        Instant.parse("2026-03-24T07:35:00Z"),
                        "valhalla-test",
                        "ko-KR"
                ),
                List.of(),
                List.of(),
                List.of(12.0)
        );

        when(courseRepository.findNotDeletedCourse(10L)).thenReturn(Optional.of(course));
        when(courseTraceRequestMapper.map(course)).thenReturn(traceRequest);
        when(valhallaTraceClient.requestRouteTrace(traceRequest)).thenReturn(routeTraceResult);
        when(valhallaTraceClient.requestTraceAttributes(traceRequest)).thenReturn(traceAttributes);
        when(navigationBundleDocumentBuilder.build(course, routeTraceResult, traceAttributes)).thenReturn(document);
        when(navigationBundleStore.storeLatest(10L, document)).thenReturn("navigation/latest/10.json");

        NavigationBundleGenerationResult result = navigationBundleGenerationService.generate(10L);

        assertThat(result.courseId()).isEqualTo(10L);
        assertThat(result.storageKey()).isEqualTo("navigation/latest/10.json");
        assertThat(result.status()).isEqualTo(NavigationBundleStatus.READY);
        assertThat(result.generatedAt()).isEqualTo(Instant.parse("2026-03-24T07:35:00Z"));
        assertThat(course.getNavigationBundleUrl()).isEqualTo("https://cdn.withrun.kr/navigation/latest/10.json");
        verify(navigationBundleDocumentValidator).validate(document);
    }

    @DisplayName("trace_attributes 보강이 실패해도 기본 번들 생성은 계속 진행한다")
    @Test
    void continuesWhenTraceAttributesFails() {
        Course course = course(10L);
        ValhallaTraceRequest traceRequest = new ValhallaTraceRequest(
                List.of(new ValhallaRequestPoint(37.5, 127.0), new ValhallaRequestPoint(37.6, 127.1)),
                "pedestrian",
                "map_snap",
                5,
                20,
                500,
                "ko-KR",
                "kilometers"
        );
        ValhallaRouteTraceResult routeTraceResult = new ValhallaRouteTraceResult(List.of(), List.of());
        ValhallaTraceAttributes emptyTraceAttributes = new ValhallaTraceAttributes(List.of(), List.of(), List.of());
        NavigationBundleDocument document = new NavigationBundleDocument(
                List.of(),
                List.of(),
                new kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleMetadata(
                        1200d,
                        Instant.parse("2026-03-24T07:35:00Z"),
                        "valhalla-test",
                        "ko-KR"
                ),
                List.of(),
                List.of(),
                List.of()
        );

        when(courseRepository.findNotDeletedCourse(10L)).thenReturn(Optional.of(course));
        when(courseTraceRequestMapper.map(course)).thenReturn(traceRequest);
        when(valhallaTraceClient.requestRouteTrace(traceRequest)).thenReturn(routeTraceResult);
        when(valhallaTraceClient.requestTraceAttributes(traceRequest)).thenThrow(
                new NavigationBundleGenerationException(
                        NavigationBundleFailureCode.VALHALLA_TRACE_ATTRIBUTES_FAILED,
                        "trace_attributes failed"
                )
        );
        when(navigationBundleDocumentBuilder.build(eq(course), eq(routeTraceResult), any(ValhallaTraceAttributes.class))).thenReturn(document);
        when(navigationBundleStore.storeLatest(10L, document)).thenReturn("navigation/latest/10.json");

        NavigationBundleGenerationResult result = navigationBundleGenerationService.generate(10L);

        assertThat(result.status()).isEqualTo(NavigationBundleStatus.READY);
        verify(navigationBundleDocumentBuilder).build(course, routeTraceResult, emptyTraceAttributes);
    }

    @DisplayName("코스가 없으면 COURSE_NOT_FOUND 실패로 종료한다")
    @Test
    void failsWhenCourseDoesNotExist() {
        when(courseRepository.findNotDeletedCourse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> navigationBundleGenerationService.generate(99L))
                .isInstanceOf(NavigationBundleGenerationException.class)
                .extracting("failureCode")
                .isEqualTo(NavigationBundleFailureCode.COURSE_NOT_FOUND);
    }

    private static Course course(Long id) {
        Course course = Course.builder()
                .title("Course " + id)
                .status(CourseStatus.OFFICIAL)
                .distanceM(5000)
                .elevationGainM(20)
                .startLatitude(37.5)
                .startLongitude(127.0)
                .endLatitude(37.6)
                .endLongitude(127.1)
                .coordinates(new Coordinates(List.of(37.5, 37.6), List.of(127.0, 127.1), List.of(10.0, 12.0)))
                .build();
        setId(course, id);
        return course;
    }

    private static void setId(Course course, Long id) {
        try {
            java.lang.reflect.Field field = Course.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(course, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to set course id", exception);
        }
    }
}
