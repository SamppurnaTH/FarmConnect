package com.agrichain.subsidy.repository;

import com.agrichain.subsidy.entity.SubsidyProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SubsidyProgramRepository extends JpaRepository<SubsidyProgram, UUID> {
}
