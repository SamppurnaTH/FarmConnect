package com.agrichain.subsidy;

import com.agrichain.common.enums.SubsidyProgramStatus;
import com.agrichain.subsidy.dto.DisbursementRequest;
import com.agrichain.subsidy.entity.SubsidyProgram;
import com.agrichain.subsidy.repository.DisbursementRepository;
import com.agrichain.subsidy.repository.SubsidyProgramRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.Positive;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SubsidyPropertyTest {

    private final SubsidyProgramRepository programRepository = Mockito.mock(SubsidyProgramRepository.class);
    private final DisbursementRepository disbursementRepository = Mockito.mock(DisbursementRepository.class);
    private final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    private final SubsidyService subsidyService;

    public SubsidyPropertyTest() {
        RestTemplateBuilder builder = Mockito.mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(restTemplate);
        this.subsidyService = new SubsidyService(programRepository, disbursementRepository, builder);
    }

    /**
     * Property 26: Subsidy program status transitions are monotonic.
     */
    @Property(tries = 50)
    void property_26_monotonic_transitions(
            @ForAll SubsidyProgramStatus from,
            @ForAll SubsidyProgramStatus to) {

        Mockito.reset(programRepository);

        SubsidyProgram program = new SubsidyProgram();
        program.setStatus(from);
        when(programRepository.findById(any())).thenReturn(Optional.of(program));
        when(programRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean shouldFail = false;
        if (from == SubsidyProgramStatus.Closed) shouldFail = true;
        if (to == SubsidyProgramStatus.Active && from != SubsidyProgramStatus.Draft) shouldFail = true;
        if (to == SubsidyProgramStatus.Draft) shouldFail = true;

        if (shouldFail) {
            assertThatThrownBy(() -> subsidyService.updateProgramStatus(UUID.randomUUID(), to))
                    .isInstanceOf(RuntimeException.class);
        } else {
            assertThatCode(() -> subsidyService.updateProgramStatus(UUID.randomUUID(), to))
                    .doesNotThrowAnyException();
        }
    }

    /**
     * Property 27: Total disbursed never exceeds program budget.
     */
    @Property(tries = 50)
    void property_27_budget_enforcement(
            @ForAll @Positive int budget,
            @ForAll @Positive int disbursed,
            @ForAll @Positive int requestAmount) {

        Mockito.reset(programRepository, disbursementRepository, restTemplate);

        BigDecimal b = BigDecimal.valueOf(budget);
        BigDecimal d = BigDecimal.valueOf(disbursed);
        BigDecimal r = BigDecimal.valueOf(requestAmount);

        SubsidyProgram program = new SubsidyProgram();
        program.setStatus(SubsidyProgramStatus.Active);
        program.setBudgetAmount(b);
        program.setTotalDisbursed(d);
        when(programRepository.findById(any())).thenReturn(Optional.of(program));
        when(programRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Mock farmer status check
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("Active");

        DisbursementRequest req = new DisbursementRequest();
        req.setProgramId(UUID.randomUUID());
        req.setFarmerId(UUID.randomUUID());
        req.setAmount(r);
        req.setProgramCycle("2024-Q1");

        if (r.compareTo(b.subtract(d)) > 0) {
            assertThatThrownBy(() -> subsidyService.applyForDisbursement(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("budget");
        } else {
            when(disbursementRepository.existsByFarmerIdAndProgramIdAndProgramCycle(any(), any(), any()))
                    .thenReturn(false);
            when(disbursementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> subsidyService.applyForDisbursement(req))
                    .doesNotThrowAnyException();
        }
    }

    /**
     * Property 28: Duplicate disbursement for same farmer/program/cycle is rejected.
     */
    @Property(tries = 50)
    void property_28_duplicate_rejection(
            @ForAll("uuids") UUID farmerId,
            @ForAll("uuids") UUID programId,
            @ForAll("cycles") String cycle) {

        Mockito.reset(programRepository, disbursementRepository, restTemplate);

        SubsidyProgram program = new SubsidyProgram();
        program.setStatus(SubsidyProgramStatus.Active);
        program.setBudgetAmount(BigDecimal.valueOf(1000000));
        program.setTotalDisbursed(BigDecimal.ZERO);
        when(programRepository.findById(any())).thenReturn(Optional.of(program));

        // Mock farmer check
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("Active");

        // Mock existing record
        when(disbursementRepository.existsByFarmerIdAndProgramIdAndProgramCycle(eq(farmerId), eq(programId), eq(cycle)))
                .thenReturn(true);

        DisbursementRequest req = new DisbursementRequest();
        req.setProgramId(programId);
        req.setFarmerId(farmerId);
        req.setAmount(BigDecimal.valueOf(100));
        req.setProgramCycle(cycle);

        assertThatThrownBy(() -> subsidyService.applyForDisbursement(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate");
    }

    @Provide
    Arbitrary<UUID> uuids() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<String> cycles() {
        return Arbitraries.of("2024-Q1", "2024-Season1", "2024-Fertilizer");
    }
}
