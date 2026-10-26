package com.fintechledger.transfer;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.Instant; import java.util.*; import org.springframework.http.HttpStatus; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/transfers") public class TransferController{
 record TransferRequest(@NotNull UUID sourceAccountId,@NotBlank @Pattern(regexp="IN[A-Z0-9]{16}",message="Enter the recipient account number, for example IN…") String destinationAccountNumber,@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,@Size(max=500) String description){}
 record View(UUID id,String reference,UUID sourceAccountId,UUID destinationAccountId,BigDecimal amount,String currency,String description,String status,Instant createdAt){}
 private final TransferService service;public TransferController(TransferService s){service=s;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) View create(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody TransferRequest body,Authentication auth){return view(service.create(auth.getName(),key,new TransferService.Request(body.sourceAccountId(),body.destinationAccountNumber(),body.amount(),body.description())));}
 @GetMapping List<View> list(Authentication auth){return service.list(auth.getName()).stream().map(this::view).toList();}
 @GetMapping("/{id}") View get(@PathVariable UUID id,Authentication auth){return view(service.get(id,auth.getName()));}
 @GetMapping("/{id}/receipt") View receipt(@PathVariable UUID id,Authentication auth){return get(id,auth);}
 @PostMapping("/simulated-funding") Map<String,Object> simulatedFunding(@RequestBody Map<String,String> body,Authentication auth){return service.fund(auth.getName(),UUID.fromString(body.get("accountId")),new BigDecimal(body.get("amount")));}
 @PostMapping("/{id}/reverse") View reverse(@PathVariable UUID id,Authentication auth){return view(service.reverse(id,auth.getName()));}
 private View view(Transfer t){return new View(t.getId(),t.getReference(),t.getSource().getId(),t.getDestination().getId(),t.getAmount(),t.getCurrency(),t.getDescription(),t.getStatus(),t.getCreatedAt());}
}
