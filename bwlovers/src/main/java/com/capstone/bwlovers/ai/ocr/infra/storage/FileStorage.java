package com.capstone.bwlovers.ai.ocr.infra.storage;

import com.capstone.bwlovers.ai.ocr.domain.OcrPageFileRef;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorage {
    List<OcrPageFileRef> saveJobFiles(String jobId, List<MultipartFile> files);
    String getAccessibleUrl(String uri);
    void deleteJobFiles(String jobId, List<OcrPageFileRef> refs);
}