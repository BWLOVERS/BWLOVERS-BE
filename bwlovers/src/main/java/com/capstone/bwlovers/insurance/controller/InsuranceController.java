package com.capstone.bwlovers.insurance.controller;

import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.insurance.dto.request.InsuranceSelectionSaveRequest;
import com.capstone.bwlovers.insurance.dto.request.UpdateMemoRequest;
import com.capstone.bwlovers.insurance.dto.response.InsuranceDetailListResponse;
import com.capstone.bwlovers.insurance.dto.response.InsuranceDetailResponse;
import com.capstone.bwlovers.insurance.dto.response.InsuranceListResponse;
import com.capstone.bwlovers.insurance.dto.response.UpdateMemoResponse;
import com.capstone.bwlovers.insurance.service.InsuranceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/insurances")
public class InsuranceController {

    private final InsuranceService insuranceService;

    /**
     * 보험-특약 저장 POST /insurances/selected
     */
    @PostMapping("/selected")
    public ResponseEntity<Long> saveSelected(@AuthenticationPrincipal User user,
                                             @Valid @RequestBody InsuranceSelectionSaveRequest request) {
        return ResponseEntity.ok(insuranceService.saveSelected(user.getUserId(), request));
    }

    /**
     * 보험 메모 수정 PATCH /insurances/{insuranceId}/memo
     */
    @PatchMapping("/{insuranceId}/memo")
    public ResponseEntity<UpdateMemoResponse> updateMemo(@AuthenticationPrincipal User user,
                                                         @PathVariable Long insuranceId,
                                                         @RequestBody UpdateMemoRequest request) {
        String updatedMemo = insuranceService.updateInsuranceMemo(user.getUserId(), insuranceId, request.getMemo());
        UpdateMemoResponse response = UpdateMemoResponse.builder()
                .memo(updatedMemo)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 보험 삭제 DELETE /insurances/{insuranceId}
     */
    @DeleteMapping("/{insuranceId}")
    public ResponseEntity<Void> deleteInsurance(@AuthenticationPrincipal User user,
                                                @PathVariable Long insuranceId) {
        insuranceService.deleteInsurance(user.getUserId(), insuranceId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 마이페이지 보험 리스트 조회 GET /insurances
     */
    @GetMapping
    public ResponseEntity<List<InsuranceListResponse>> getMyInsurances(@AuthenticationPrincipal User user) {
        List<InsuranceListResponse> response = insuranceService.getMyInsuranceList(user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 마이페이지 보험 상세 리스트 조회 GET /insurances
     */
    @GetMapping("/details")
    public ResponseEntity<List<InsuranceDetailListResponse>> getMyInsuranceDetails(@AuthenticationPrincipal User user) {
        List<InsuranceDetailListResponse> response = insuranceService.getMyInsuranceDetails(user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 마이페이지 보험 상세 조회 GET /insurances/{insuranceId}
     */
    @GetMapping("/{insuranceId}")
    public ResponseEntity<InsuranceDetailResponse> getInsuranceDetail(@AuthenticationPrincipal User user,
                                                                      @PathVariable Long insuranceId) {
        InsuranceDetailResponse response = insuranceService.getInsuranceDetail(user.getUserId(), insuranceId);
        return ResponseEntity.ok(response);
    }
}
