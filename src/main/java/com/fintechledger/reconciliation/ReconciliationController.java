package com.fintechledger.reconciliation;
import java.util.*;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/reconciliation") public class ReconciliationController{
 private final ReconciliationService service;public ReconciliationController(ReconciliationService service){this.service=service;}
 @PostMapping("/run") Map<String,Object> run(){return service.run();}
 @GetMapping List<Map<String,Object>> list(){return service.runs();}
 @GetMapping("/{id}") Map<String,Object> detail(@PathVariable UUID id){return service.detail(id);}
}
