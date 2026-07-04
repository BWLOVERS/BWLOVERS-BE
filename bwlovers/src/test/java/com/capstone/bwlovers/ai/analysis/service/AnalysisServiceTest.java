package com.capstone.bwlovers.ai.analysis.service;

import com.capstone.bwlovers.ai.analysis.dto.response.AnalysisResultResponse;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.capstone.bwlovers.insurance.domain.InsuranceProduct;
import com.capstone.bwlovers.insurance.repository.InsuranceProductRepository;
import com.capstone.bwlovers.insurance.repository.SpecialContractRepository;
import com.capstone.bwlovers.simulation.domain.Simulation;
import com.capstone.bwlovers.simulation.domain.SimulationContract;
import com.capstone.bwlovers.simulation.repository.SimulationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AnalysisCacheService analysisCacheService;
    @Mock
    private InsuranceProductRepository insuranceProductRepository;
    @Mock
    private SpecialContractRepository specialContractRepository;
    @Mock
    private SimulationRepository simulationRepository;

    private AnalysisService analysisService;

    @BeforeEach
    void setUp() {
        analysisService = new AnalysisService(
                userRepository,
                WebClient.builder().build(),
                analysisCacheService,
                insuranceProductRepository,
                specialContractRepository,
                simulationRepository,
                new ObjectMapper()
        );
    }

    @Test
    void getSimulationResult_returnsDatabaseSourceWhenRedisReadFails() {
        String resultId = "sim-result-1";
        InsuranceProduct sourceInsurance = sourceInsurance();

        when(analysisCacheService.getResult(resultId))
                .thenThrow(new CustomException(ExceptionCode.REDIS_CONNECTION_FAILED));
        when(analysisCacheService.findSourceInsuranceIdSafely(resultId)).thenReturn(99L);
        when(simulationRepository.findWithContractsByResultId(resultId))
                .thenReturn(Optional.of(savedSimulation(resultId)));
        when(insuranceProductRepository.findById(99L))
                .thenReturn(Optional.of(sourceInsurance));

        AnalysisResultResponse response = analysisService.getSimulationResult(resultId);

        assertEquals(resultId, response.getResultId());
        assertEquals("ACME", response.getInsuranceCompany());
        assertEquals("Safe Plan", response.getProductName());
        assertEquals("보장 내용을 알려줘", response.getQuestion());
        assertEquals("충분한 보장입니다.", response.getResult());
        assertEquals(1, response.getSpecialContracts().size());
        assertEquals("입원 특약", response.getSpecialContracts().get(0).getContractName());
        assertTrue(response.getLongTerm());
        assertEquals("1000", response.getSumInsured());
        assertEquals("10", response.getMonthlyCost());
        assertEquals("saved memo", response.getMemo());
        verify(analysisCacheService).saveResultSafely(eq(resultId), any(AnalysisResultResponse.class), eq(2592000L));
    }

    @Test
    void getSimulationResult_rethrowsRedisFailureWhenNoFallbackSourceExists() {
        String resultId = "sim-result-2";

        when(analysisCacheService.getResult(resultId))
                .thenThrow(new CustomException(ExceptionCode.REDIS_CONNECTION_FAILED));
        when(simulationRepository.findWithContractsByResultId(resultId))
                .thenReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> analysisService.getSimulationResult(resultId)
        );

        assertEquals(ExceptionCode.REDIS_CONNECTION_FAILED, exception.getExceptionCode());
    }

    private Simulation savedSimulation(String resultId) {
        return Simulation.builder()
                .resultId(resultId)
                .insuranceCompany("ACME")
                .productName("Safe Plan")
                .question("보장 내용을 알려줘")
                .result("충분한 보장입니다.")
                .contracts(List.of(
                        SimulationContract.builder()
                                .contractName("입원 특약")
                                .pageNumber(3L)
                                .build()
                ))
                .build();
    }

    private InsuranceProduct sourceInsurance() {
        InsuranceProduct insurance = mock(InsuranceProduct.class);
        when(insurance.isLongTerm()).thenReturn(true);
        when(insurance.getSumInsured()).thenReturn("1000");
        when(insurance.getMonthlyCost()).thenReturn("10");
        when(insurance.getMemo()).thenReturn("saved memo");
        return insurance;
    }
}
