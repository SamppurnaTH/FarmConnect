package com.agrichain.subsidy.repository;

import com.agrichain.subsidy.entity.SubsidyProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface SubsidyProgramRepository extends JpaRepository<SubsidyProgram, UUID> {

    /** SUM aggregate — does NOT load all rows into memory. */
    @Query("SELECT COALESCE(SUM(p.totalDisbursed), 0) FROM SubsidyProgram p")
    BigDecimal sumTotalDisbursed();
}
