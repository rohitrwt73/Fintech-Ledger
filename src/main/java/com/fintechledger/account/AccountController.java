package com.fintechledger.account;
import com.fintechledger.ledger.LedgerEntryRepository; import jakarta.validation.Valid; import jakarta.validation.constraints.NotBlank; import java.math.BigDecimal; import java.util.*; import org.springframework.http.HttpStatus; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/accounts") public class AccountController{
 record AccountView(UUID id,String accountNumber,String currency,String status,BigDecimal balance){} record CreateAccount(@NotBlank String currency){} record LedgerView(UUID id,String transactionReference,UUID accountId,String direction,BigDecimal amount,java.time.Instant createdAt){}
 private final AccountService service; private final LedgerEntryRepository entries; public AccountController(AccountService s,LedgerEntryRepository e){service=s;entries=e;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) AccountView create(@Valid @RequestBody(required=false) CreateAccount ignored,Authentication auth){return view(service.create(auth.getName()));}
 @GetMapping List<AccountView> list(Authentication auth){return service.list(auth.getName()).stream().map(this::view).toList();}
 @GetMapping("/{id}") AccountView get(@PathVariable UUID id,Authentication auth){return view(service.get(id,auth.getName()));}
 @GetMapping("/{id}/balance") Map<String,Object> balance(@PathVariable UUID id,Authentication auth){var a=service.get(id,auth.getName());return Map.of("accountId",id,"currency",a.getCurrency(),"balance",a.getBalance());}
 @GetMapping("/{id}/ledger") List<LedgerView> ledger(@PathVariable UUID id,Authentication auth){service.get(id,auth.getName());return entries.findByAccountIdOrderByCreatedAtDesc(id).stream().map(e->new LedgerView(e.getId(),e.getTransaction().getReference(),e.getAccount().getId(),e.getDirection(),e.getAmount(),e.getCreatedAt())).toList();}
 private AccountView view(Account a){return new AccountView(a.getId(),a.getAccountNumber(),a.getCurrency(),a.getStatus(),a.getBalance());}
}
