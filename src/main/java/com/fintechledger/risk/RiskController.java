package com.fintechledger.risk;
import java.time.Instant;import java.util.*;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/risk-assessments")public class RiskController{
 record Review(String status){}record View(UUID id,String transactionReference,int riskScore,String riskLevel,List<String> triggeredRules,String explanation,String reviewStatus,Instant createdAt){}
 private final RiskService service;public RiskController(RiskService s){service=s;}
 @GetMapping List<View> list(){return service.list().stream().map(this::view).toList();}@GetMapping("/{id}")View get(@PathVariable UUID id){return view(service.get(id));}
 @PostMapping("/{id}/review")View review(@PathVariable UUID id,@RequestBody Review request,Authentication auth){return view(service.review(id,request.status(),auth.getName()));}
 private View view(RiskAssessment r){return new View(r.getId(),r.getTransactionReference(),r.getRiskScore(),r.getRiskLevel(),r.getTriggeredRules(),r.getExplanation(),r.getReviewStatus(),r.getCreatedAt());}
}
