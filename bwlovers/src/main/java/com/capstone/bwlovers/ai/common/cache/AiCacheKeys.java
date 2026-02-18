package com.capstone.bwlovers.ai.common.cache;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AiCacheKeys {

    private static final String RECOMMEND_PREFIX = "ai:v2:recommend:";
    private static final String SIMULATION_PREFIX = "ai:v2:simulation:";

    /**
     * 추천 리스트 키 생성
     * 결과: ai:v2:recommend:list:{resultId}
     */
    public static String recommendListKey(String resultId) {
        return RECOMMEND_PREFIX + "list:" + resultId;
    }

    /**
     * 추천 아이템 상세 키 생성
     * 결과: ai:v2:recommend:detail:{resultId}:{itemId}
     */
    public static String recommendDetailKey(String resultId, String itemId) {
        return RECOMMEND_PREFIX + "detail:" + resultId + ":" + itemId;
    }

    /**
     * 분석 리스트 키 생성
     * 결과: ai:v2:simulation:result:{simulationId}
     */
    public static String simulationResultKey(String simulationId) {
        return SIMULATION_PREFIX + "result:" + simulationId;
    }

    /**
     * 분석 결과 상세 키 생성
     * 결과: ai:v2:simulation:status:{simulationId}
     */
    public static String simulationStatusKey(String simulationId) {
        return SIMULATION_PREFIX + "status:" + simulationId;
    }

    // OCR Job
    public static final String OCR_JOB_PREFIX = "analysis:ocr:job:";
}