package kr.withrun.was.domain.file.service;

import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileService {

    private final S3Client s3Client;

    @Value("${app.s3.bucket:}")
    private String bucket;

    public String uploadCourseSnapshot(byte[] snapshotImageBytes, String directory) {
        validateBucketConfigured();

        if (snapshotImageBytes == null || snapshotImageBytes.length == 0) {
            throw new CustomException(ResponseCode.EMPTY_FILE);
        }

        String objectKey = buildObjectKey(directory, ".png");

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType("image/png")
                .contentLength((long) snapshotImageBytes.length)
                .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(snapshotImageBytes));
        } catch (S3Exception exception) {
            log.error("S3 파일 업로드 실패. bucket={}, key={}", bucket, objectKey, exception);
            throw new CustomException(ResponseCode.S3_UPLOAD_FAILED);
        } catch (RuntimeException exception) {
            log.error("스냅샷 바이트 업로드 중 예외 발생. bucket={}, key={}", bucket, objectKey, exception);
            throw new CustomException(ResponseCode.S3_UPLOAD_FAILED);
        }

        return s3Client.utilities()
                .getUrl(GetUrlRequest.builder().bucket(bucket).key(objectKey).build())
                .toExternalForm();
    }

    private void validateBucketConfigured() {
        if (!StringUtils.hasText(bucket)) {
            throw new CustomException(ResponseCode.S3_BUCKET_NOT_CONFIGURED);
        }
    }

    private String buildObjectKey(String directory, String extension) {
        String normalizedDirectory = normalizeDirectory(directory);
        String objectDirectory = StringUtils.hasText(normalizedDirectory)
                ? normalizedDirectory
                : "course-snapshots/";
        return objectDirectory + UUID.randomUUID() + extension;
    }

    private String normalizeDirectory(String directory) {
        if (!StringUtils.hasText(directory)) {
            return "";
        }

        String normalized = directory.trim().replace('\\', '/');
        normalized = normalized.replaceAll("/{2,}", "/");
        normalized = normalized.replaceAll("^/+", "");
        normalized = normalized.replaceAll("/+$", "");

        if (!StringUtils.hasText(normalized)) {
            return "";
        }

        if (normalized.contains("..")) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }

        return normalized + "/";
    }
}
