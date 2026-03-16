package com.capstone.bwlovers.simulation.service;

import com.capstone.bwlovers.ai.analysis.dto.response.AnalysisResultResponse;
import com.capstone.bwlovers.ai.analysis.service.AnalysisService;
import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.capstone.bwlovers.simulation.domain.Simulation;
import com.capstone.bwlovers.simulation.domain.SimulationContract;
import com.capstone.bwlovers.simulation.dto.request.SimulationSaveRequest;
import com.capstone.bwlovers.simulation.dto.response.SimulationDetailListResponse;
import com.capstone.bwlovers.simulation.dto.response.SimulationDetailResponse;
import com.capstone.bwlovers.simulation.dto.response.SimulationListResponse;
import com.capstone.bwlovers.simulation.repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final UserRepository userRepository;
    private final SimulationRepository simulationRepository;
    private final AnalysisService analysisService;

    /**
     * Redis의 결과를 DB에 저장
     * 저장: simulationId, question/result, insuranceCompany/productName, 특약리스트(이름+페이지)
     */
    @Transactional
    public Long saveSimulationResult(Long userId, SimulationSaveRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        if (request == null || isBlank(request.getResultId())) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

        String resultId = request.getResultId();

        // 중복 저장 방지(원하면 예외로 바꿀 수도 있음)
        if (simulationRepository.existsByResultId(resultId)) {
            return simulationRepository.findByResultId(resultId)
                    .orElseThrow(() -> new CustomException(ExceptionCode.AI_SERVER_5XX))
                    .getId();
        }

        // 1) Redis에서 결과 조회(없으면 AI_RESULT_NOT_FOUND 터짐)
        AnalysisResultResponse cached = analysisService.getSimulationResult(resultId);

        // 2) 엔티티 생성
        Simulation simulation = Simulation.builder()
                .resultId(cached.getResultId())
                .user(user)
                .insuranceCompany(nullToEmpty(cached.getInsuranceCompany()))
                .productName(nullToEmpty(cached.getProductName()))
                .question(nullToEmpty(cached.getQuestion()))
                .result(nullToEmpty(cached.getResult()))
                .build();

        if (isBlank(simulation.getInsuranceCompany())
                || isBlank(simulation.getProductName())
                || isBlank(simulation.getQuestion())
                || isBlank(simulation.getResult())) {
            throw new CustomException(ExceptionCode.AI_PROCESSING_FAILED);
        }

        // 3) 특약 리스트 저장
        if (cached.getSpecialContracts() != null) {
            for (var sc : cached.getSpecialContracts()) {
                if (sc == null || isBlank(sc.getContractName())) continue;

                long page = (sc.getPageNumber() == null) ? 0L : sc.getPageNumber().longValue();

                SimulationContract contract = SimulationContract.builder()
                        .contractName(sc.getContractName())
                        .pageNumber(page)
                        .build();

                simulation.addContract(contract);
            }
        }

        return simulationRepository.save(simulation).getId();
    }

    /**
     * 시뮬레이션 리스트 조회
     */
    @Transactional(readOnly = true)
    public List<SimulationListResponse> getSimulationList(Long userId) {
        return simulationRepository.findByUser_UserIdOrderByCreatedAtAsc(userId).stream()
                .map(s -> SimulationListResponse.of(s.getId(), s.getCreatedAt()))
                .toList();
    }

    /**
     * 시뮬레이션 상세 리스트 조회
     */
    @Transactional(readOnly = true)
    public List<SimulationDetailListResponse> getSimulationDetailList(Long userId) {
        return simulationRepository.findByUser_UserIdOrderByCreatedAtAsc(userId).stream()
                .map(s -> SimulationDetailListResponse.of(s.getId(), s.getProductName(), s.getCreatedAt()))
                .toList();
    }

    /**
     * 시뮬레이션 상세보기
     */
    @Transactional(readOnly = true)
    public SimulationDetailResponse getSimulationDetail(Long userId, Long simulationId) {
        Simulation simulation = simulationRepository.findByIdAndUser_UserId(simulationId, userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.SIMULATION_NOT_FOUND));

        AnalysisResultResponse cached = analysisService.getSimulationResult(simulation.getResultId());
        return SimulationDetailResponse.from(simulation, cached);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * 시뮬레이션 삭제
     */
    @Transactional
    public void deleteSimulation(Long userId, Long simulationId) {
        boolean exists = simulationRepository.existsByIdAndUser_UserId(simulationId, userId);
        if (!exists) {
            throw new CustomException(ExceptionCode.SIMULATION_NOT_FOUND);
        }

        simulationRepository.deleteByIdAndUser_UserId(simulationId, userId);
    }
}
