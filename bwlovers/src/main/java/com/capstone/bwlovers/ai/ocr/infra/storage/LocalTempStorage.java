//package com.capstone.bwlovers.ai.ocr.infra.storage;
//
//import com.capstone.bwlovers.ai.ocr.domain.OcrPageFileRef;
//import com.capstone.bwlovers.global.exception.CustomException;
//import com.capstone.bwlovers.global.exception.ExceptionCode;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.springframework.util.FileSystemUtils;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.nio.file.*;
//import java.util.ArrayList;
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//public class LocalTempStorage implements FileStorage {
//
//    @Value("${analysis.ocr.local-temp-dir:/tmp/analysis-ocr}")
//    private String baseDir;
//
//    @Override
//    public List<OcrPageFileRef> saveJobFiles(String jobId, List<MultipartFile> files) {
//        try {
//            Path jobDir = Paths.get(baseDir, jobId);
//            Files.createDirectories(jobDir);
//
//            List<OcrPageFileRef> refs = new ArrayList<>();
//            for (int i = 0; i < files.size(); i++) {
//                MultipartFile f = files.get(i);
//
//                String filename = String.format("%02d_%s", i, sanitize(f.getOriginalFilename()));
//                Path dest = jobDir.resolve(filename);
//
//                Files.copy(f.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
//
//                refs.add(OcrPageFileRef.builder()
//                        .pageIndex(i)
//                        .uri(dest.toString())
//                        .build());
//            }
//            return refs;
//
//        } catch (IOException e) {
//            throw new CustomException(ExceptionCode.OCR_TEMP_FILE_SAVE_FAILED);
//        }
//    }
//
//    @Override
//    public String getAccessibleUrl(String uri) {
//        throw new CustomException(ExceptionCode.OCR_LOCAL_STORAGE_NOT_SUPPORTED);
//    }
//
//    @Override
//    public void deleteJobFiles(String jobId, List<OcrPageFileRef> refs) {
//        try {
//            Path jobDir = Paths.get(baseDir, jobId);
//            FileSystemUtils.deleteRecursively(jobDir.toFile());
//        } catch (Exception e) {
//            throw new CustomException(ExceptionCode.OCR_TEMP_FILE_DELETE_FAILED);
//        }
//    }
//
//    private String sanitize(String name) {
//        if (name == null) return "page.jpg";
//        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
//    }
//}
