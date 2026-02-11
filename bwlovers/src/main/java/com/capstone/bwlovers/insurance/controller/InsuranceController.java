package com.capstone.bwlovers.insurance.controller;

import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.insurance.dto.request.InsuranceSelectionSaveRequest;
import com.capstone.bwlovers.insurance.dto.request.UpdateMemoRequest;
import com.capstone.bwlovers.insurance.dto.response.UpdateMemoResponse;
import com.capstone.bwlovers.insurance.service.InsuranceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceService insuranceService;

    /**
     * 보험-특약 저장 POST /insurances/selected
     */
    @PostMapping("/insurances/selected")
    public ResponseEntity<Long> saveSelected(@AuthenticationPrincipal User user,
                                             @Valid @RequestBody InsuranceSelectionSaveRequest request) {
        return ResponseEntity.ok(insuranceService.saveSelected(user.getUserId(), request));
    }

    /**
     * 보험 메모 수정 PATCH /users/me/insurances/{insuranceId}/memo
     */
    @PatchMapping("users/me/insurances/{insuranceId}/memo")
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
     * 보험 삭제 DELETE /users/me/insurances/{insuranceId}
     */
    @DeleteMapping("/users/me/insurances/{insuranceId}")
    public ResponseEntity<Void> deleteInsurance(@AuthenticationPrincipal User user,
                                                @PathVariable Long insuranceId) {
        insuranceService.deleteInsurance(user.getUserId(), insuranceId);
        return ResponseEntity.noContent().build();
    }

}
