package kr.withrun.was.domain.navigation.service.bundle;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleDocument;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleGenerationResult;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaRouteTraceResult;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaTraceAttributes;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaTraceRequest;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.service.artifact.NavigationBundleStore;
import kr.withrun.was.domain.navigation.service.input.CourseTraceRequestMapper;
import kr.withrun.was.domain.navigation.service.valhalla.ValhallaTraceClient;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import kr.withrun.was.domain.navigation.type.NavigationBundleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
/**
 * 코스 좌표를 기반으로 latest navigation bundle 을 동기 생성하고 저장하는 서비스입니다.
 */
public class NavigationBundleGenerationService {

    private static final Logger log = LoggerFactory.getLogger(NavigationBundleGenerationService.class);

    private final CourseRepository courseRepository;
    private final CourseTraceRequestMapper courseTraceRequestMapper;
    private final ValhallaTraceClient valhallaTraceClient;
    private final NavigationBundleDocumentBuilder navigationBundleDocumentBuilder;
    private final NavigationBundleDocumentValidator navigationBundleDocumentValidator;
    private final NavigationBundleStore navigationBundleStore;
    private final String cdnBaseUrl;
    private final boolean traceAttributesEnabled;

    /**
     * 번들 생성 파이프라인에 필요한 구성 요소를 주입받는 생성자입니다.
     */
    public NavigationBundleGenerationService(
            CourseRepository courseRepository,
            CourseTraceRequestMapper courseTraceRequestMapper,
            ValhallaTraceClient valhallaTraceClient,
            NavigationBundleDocumentBuilder navigationBundleDocumentBuilder,
            NavigationBundleDocumentValidator navigationBundleDocumentValidator,
            NavigationBundleStore navigationBundleStore,
            @Value("${app.cdn.base-url:}") String cdnBaseUrl,
            @Value("${app.navigation.trace-attributes-enabled:true}") boolean traceAttributesEnabled
    ) {
        this.courseRepository = courseRepository;
        this.courseTraceRequestMapper = courseTraceRequestMapper;
        this.valhallaTraceClient = valhallaTraceClient;
        this.navigationBundleDocumentBuilder = navigationBundleDocumentBuilder;
        this.navigationBundleDocumentValidator = navigationBundleDocumentValidator;
        this.navigationBundleStore = navigationBundleStore;
        this.cdnBaseUrl = cdnBaseUrl;
        this.traceAttributesEnabled = traceAttributesEnabled;
    }

    @Transactional
    /**
     * 코스를 읽어 Valhalla 호출부터 S3 저장, URL 반영까지 한 번에 수행합니다.
     */
    public NavigationBundleGenerationResult generate(Long courseId) {
        Course course = courseRepository.findNotDeletedCourse(courseId)
                .orElseThrow(() -> new NavigationBundleGenerationException(
                        NavigationBundleFailureCode.COURSE_NOT_FOUND,
                        "Course not found for latest navigation bundle generation: " + courseId
                ));

        ValhallaTraceRequest traceRequest = courseTraceRequestMapper.map(course);
        ValhallaRouteTraceResult routeTraceResult = valhallaTraceClient.requestRouteTrace(traceRequest);
        ValhallaTraceAttributes traceAttributes = fetchTraceAttributes(traceRequest);
        NavigationBundleDocument document = navigationBundleDocumentBuilder.build(course, routeTraceResult, traceAttributes);

        navigationBundleDocumentValidator.validate(document);
        String storageKey = navigationBundleStore.storeLatest(courseId, document);
        course.updateNavigationBundleUrl(buildDownloadUrl(storageKey));
        courseRepository.save(course);

        return new NavigationBundleGenerationResult(
                courseId,
                storageKey,
                document.metadata().generatedAt(),
                NavigationBundleStatus.READY
        );
    }

    private ValhallaTraceAttributes fetchTraceAttributes(ValhallaTraceRequest traceRequest) {
        if (!traceAttributesEnabled) {
            return emptyTraceAttributes();
        }

        try {
            return valhallaTraceClient.requestTraceAttributes(traceRequest);
        } catch (NavigationBundleGenerationException exception) {
            log.warn("trace_attributes enrichment skipped. failureCode={}, message={}", exception.getFailureCode(), exception.getMessage());
            return emptyTraceAttributes();
        }
    }

    private ValhallaTraceAttributes emptyTraceAttributes() {
        return new ValhallaTraceAttributes(List.of(), List.of(), List.of());
    }

    /**
     * 저장된 S3 key 를 외부에서 사용할 다운로드 URL 형태로 변환합니다.
     */
    private String buildDownloadUrl(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            return null;
        }

        String normalizedKey = trimLeadingSlash(storageKey);
        if (!StringUtils.hasText(cdnBaseUrl)) {
            return normalizedKey;
        }

        return trimTrailingSlash(cdnBaseUrl) + "/" + normalizedKey;
    }

    /**
     * key 앞에 붙은 불필요한 슬래시를 제거합니다.
     */
    private String trimLeadingSlash(String value) {
        String normalized = value;
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    /**
     * base URL 끝에 붙은 불필요한 슬래시를 제거합니다.
     */
    private String trimTrailingSlash(String value) {
        String normalized = value;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
