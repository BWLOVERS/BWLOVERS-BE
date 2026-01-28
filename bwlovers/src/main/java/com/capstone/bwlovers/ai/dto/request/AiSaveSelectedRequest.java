package com.capstone.bwlovers.ai.dto.request;

import lombok.Getter;

import java.util.List;

@Getter
public class AiSaveSelectedRequest {
    private String resultId;
    private String itemId;
    private List<String> selectedContractNames;
}
