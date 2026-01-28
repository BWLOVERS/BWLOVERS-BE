package com.capstone.bwlovers.ai.dto.request;

import lombok.Getter;

import java.util.List;

@Getter
public class AiSaveSelectedRequest {
    private String resultId;
    private List<String> selectedContractNames; // 선택 특약 이름 리스트
}
