package com.capstone.bwlovers.ai.cache;

public final class AiCacheKeys {
    private AiCacheKeys() {}

    public static String listKey(String resultId) {
        return "ai:recommend:list:" + resultId;
    }

    public static String detailKey(String resultId, String itemId) {
        return "ai:recommend:detail:" + resultId + ":" + itemId;
    }
}
