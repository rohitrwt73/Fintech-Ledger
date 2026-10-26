package com.fintechledger.risk;
import java.time.Instant;import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment,UUID>{List<RiskAssessment> findAllByOrderByCreatedAtDesc();long countByCreatedAtAfterAndRiskLevel(Instant since,String riskLevel);}
