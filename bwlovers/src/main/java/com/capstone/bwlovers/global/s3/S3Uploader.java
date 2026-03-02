package com.capstone.bwlovers.global.s3;

import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region}")
    private String region;

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5MB

    public String uploadProfileImage(MultipartFile file, Long userId) {

        validate(file);

        String contentType = file.getContentType();
        String ext = toExt(contentType);
        String key = "profiles/" + userId + "/" + UUID.randomUUID() + ext;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

        } catch (S3Exception e) {
            String code = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null;

            if ("AccessDenied".equals(code)) {
                throw new CustomException(ExceptionCode.S3_ACCESS_DENIED);
            }
            if ("NoSuchBucket".equals(code)) {
                throw new CustomException(ExceptionCode.S3_BUCKET_NOT_FOUND);
            }
            throw new CustomException(ExceptionCode.S3_UPLOAD_FAILED);

        } catch (SdkClientException e) {
            throw new CustomException(ExceptionCode.S3_CLIENT_ERROR);

        } catch (IOException e) {
            throw new CustomException(ExceptionCode.S3_UPLOAD_FAILED);
        }

        return publicUrl(key);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ExceptionCode.S3_EMPTY_FILE);
        }
        if (file.getSize() > MAX_BYTES) {
            throw new CustomException(ExceptionCode.S3_FILE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new CustomException(ExceptionCode.S3_UNSUPPORTED_FILE_TYPE);
        }
    }

    private String toExt(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    private String publicUrl(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }
}