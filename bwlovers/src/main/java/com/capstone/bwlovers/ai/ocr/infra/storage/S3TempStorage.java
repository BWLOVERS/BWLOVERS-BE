package com.capstone.bwlovers.ai.ocr.infra.storage;

import com.capstone.bwlovers.ai.ocr.domain.OcrPageFileRef;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class S3TempStorage implements FileStorage {

    private final S3Client s3Client;
    private final S3Presigner presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${analysis.ocr.s3.prefix:tmp/ocr}")
    private String prefix;

    @Override
    public List<OcrPageFileRef> saveJobFiles(String jobId, List<MultipartFile> files) {
        try {
            List<OcrPageFileRef> refs = new ArrayList<>();

            for (int i = 0; i < files.size(); i++) {
                MultipartFile f = files.get(i);

                String safeName = (f.getOriginalFilename() == null) ? "page.jpg"
                        : f.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
                String key = prefix + "/" + jobId + "/" + String.format("%02d_", i) + safeName;

                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(f.getContentType())
                                .build(),
                        RequestBody.fromBytes(f.getBytes())
                );

                refs.add(OcrPageFileRef.builder()
                        .pageIndex(i)
                        .uri(key)
                        .build());
            }
            return refs;

        } catch (Exception e) {
            throw new CustomException(ExceptionCode.OCR_TEMP_FILE_SAVE_FAILED);
        }
    }

    @Override
    public String getAccessibleUrl(String key) {
        try {
            GetObjectRequest getReq = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(5))
                    .getObjectRequest(getReq)
                    .build();

            return presigner.presignGetObject(presignReq).url().toString();

        } catch (Exception e) {
            throw new CustomException(ExceptionCode.OCR_TEMP_FILE_READ_FAILED);
        }
    }

    @Override
    public void deleteJobFiles(String jobId, List<OcrPageFileRef> refs) {
        try {
            for (OcrPageFileRef ref : refs) {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(ref.getUri())
                        .build());
            }
        } catch (Exception e) {
            throw new CustomException(ExceptionCode.OCR_TEMP_FILE_DELETE_FAILED);
        }
    }
}