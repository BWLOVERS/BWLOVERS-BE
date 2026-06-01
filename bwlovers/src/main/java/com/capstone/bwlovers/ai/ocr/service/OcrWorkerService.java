package com.capstone.bwlovers.ai.ocr.service;

import com.capstone.bwlovers.ai.ocr.domain.OcrJobCache;
import com.capstone.bwlovers.ai.ocr.domain.OcrJobStatus;
import com.capstone.bwlovers.ai.ocr.domain.OcrPageFileRef;
import com.capstone.bwlovers.ai.ocr.domain.OcrResult;
import com.capstone.bwlovers.ai.ocr.infra.cache.OcrJobCacheRepository;
import com.capstone.bwlovers.ai.ocr.infra.ocrclient.ClovaOcrClient;
import com.capstone.bwlovers.ai.ocr.infra.llm.OpenAiOcrSummarizerClient;
import com.capstone.bwlovers.ai.ocr.infra.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrWorkerService {

    private final OcrJobCacheRepository cacheRepository;
    private final FileStorage fileStorage;
    private final ClovaOcrClient clovaOcrClient;

    private final OcrTextProcessor textProcessor;
    private final OpenAiOcrSummarizerClient ocrSummarizer;

    @Async("ocrExecutor")
    public void processAsync(String jobId) {
        OcrJobCache cache = cacheRepository.find(jobId).orElse(null);
        if (cache == null) return;

        try {
            cache.setStatus(OcrJobStatus.OCR_RUNNING);
            cacheRepository.save(cache);

            List<String> pageTexts = new ArrayList<>();

            for (OcrPageFileRef ref : cache.getFiles()) {
                String url = fileStorage.getAccessibleUrl(ref.getUri());
                String format = guessFormatFromKey(ref.getUri());

                String text = clovaOcrClient.extractTextByUrl(url, format);

                log.info("[OCR] jobId={} page={} textPreview={}",
                        jobId,
                        ref.getPageIndex(),
                        text == null ? "null" : text.substring(0, Math.min(120, text.length()))
                );

                pageTexts.add(text == null ? "" : text);

                cache.setDonePages(cache.getDonePages() + 1);
                cacheRepository.save(cache);
            }

            cache.setStatus(OcrJobStatus.SUMMARIZING);
            cacheRepository.save(cache);

            String merged = textProcessor.normalizeAndMerge(pageTexts);
            OcrResult result = textProcessor.buildGuardResultIfNeeded(merged);
            if (result == null) {
                result = ocrSummarizer.summarize(merged);
            }

            result = result.normalized();

            cache.setResult(result);
            cache.setStatus(OcrJobStatus.DONE);
            cacheRepository.save(cache);

        } catch (Exception e) {
            log.error("OCR job failed. jobId={}", jobId, e);
            cache.setStatus(OcrJobStatus.FAILED);

            cache.setError(e.getMessage());

            cacheRepository.save(cache);

        } finally {
            try {
                OcrJobCache latest = cacheRepository.find(jobId).orElse(null);
                if (latest != null) {
                    fileStorage.deleteJobFiles(jobId, latest.getFiles());
                }
            } catch (Exception ignored) {
            }
        }
    }

    private String guessFormatFromKey(String key) {
        String lower = key.toLowerCase();
        if (lower.endsWith(".png")) return "png";
        if (lower.endsWith(".jpeg")) return "jpg";
        if (lower.endsWith(".jpg")) return "jpg";
        return "jpg";
    }
}
