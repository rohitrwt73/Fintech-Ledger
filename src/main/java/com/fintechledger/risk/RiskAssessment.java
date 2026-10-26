package com.fintechledger.risk;
import jakarta.persistence.*;import org.hibernate.annotations.JdbcTypeCode;import org.hibernate.type.SqlTypes;import java.time.Instant;import java.util.*;
@Entity @Table(name="risk_assessments") public class RiskAssessment{
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;@Column(name="transaction_reference",nullable=false)private String transactionReference;
 @Column(name="risk_score",nullable=false)private int riskScore;@Column(name="risk_level",nullable=false)private String riskLevel;
 @JdbcTypeCode(SqlTypes.JSON) @Column(name="triggered_rules",columnDefinition="jsonb",nullable=false)private List<String> triggeredRules;
 @Column(nullable=false)private String explanation;@Column(name="review_status",nullable=false)private String reviewStatus="PENDING";
 @Column(name="created_at",nullable=false)private Instant createdAt=Instant.now();@Column(name="reviewed_by")private UUID reviewedBy;@Column(name="reviewed_at")private Instant reviewedAt;
 protected RiskAssessment(){} public RiskAssessment(String ref,int score,String level,List<String> rules,String explanation){transactionReference=ref;riskScore=score;riskLevel=level;triggeredRules=List.copyOf(rules);this.explanation=explanation;}
 public UUID getId(){return id;}public String getTransactionReference(){return transactionReference;}public int getRiskScore(){return riskScore;}public String getRiskLevel(){return riskLevel;}public List<String> getTriggeredRules(){return triggeredRules;}public String getExplanation(){return explanation;}public String getReviewStatus(){return reviewStatus;}public Instant getCreatedAt(){return createdAt;}
 public void review(String status,UUID actor){reviewStatus=status;reviewedBy=actor;reviewedAt=Instant.now();}
}
