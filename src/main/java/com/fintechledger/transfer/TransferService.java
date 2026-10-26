package com.fintechledger.transfer;
import com.fasterxml.jackson.databind.ObjectMapper; import com.fintechledger.account.*; import com.fintechledger.common.ApiException; import com.fintechledger.ledger.*; import com.fintechledger.user.*;
import com.fintechledger.risk.RiskService;
import com.fintechledger.audit.AuditService;
import java.math.BigDecimal; import java.nio.charset.StandardCharsets; import java.security.MessageDigest; import java.util.*; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class TransferService{
 public record Request(UUID sourceAccountId,String destinationAccountNumber,BigDecimal amount,String description){}
 private final AccountRepository accounts;private final UserRepository users;private final TransferRepository transfers;private final LedgerEntryRepository entries;private final LedgerTransactionRepository txs;private final IdempotencyRepository idem;private final ObjectMapper mapper;private final RiskService risk;private final AuditService audit;
 public TransferService(AccountRepository a,UserRepository u,TransferRepository t,LedgerEntryRepository e,LedgerTransactionRepository x,IdempotencyRepository i,ObjectMapper m,RiskService risk,AuditService audit){accounts=a;users=u;transfers=t;entries=e;txs=x;idem=i;mapper=m;this.risk=risk;this.audit=audit;}
 @Transactional public Transfer create(String email,String key,Request request){
  if(key==null||key.isBlank()||key.length()>128)throw new ApiException(HttpStatus.BAD_REQUEST,"A valid Idempotency-Key header is required");
  if(request.amount()==null||request.amount().signum()<=0||request.amount().scale()>2)throw new ApiException(HttpStatus.BAD_REQUEST,"Amount must be positive with at most two decimal places");
  var user=users.findByEmail(email).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"User not found"));String hash=fingerprint(request);
  idem.insertIfAbsent(email,key,hash);var record=idem.lockByUserEmailAndKey(email,key).orElseThrow(()->new IllegalStateException("Idempotency record was not created"));
  if(!record.getRequestHash().equals(hash))throw new ApiException(HttpStatus.CONFLICT,"Idempotency key was used for a different request");if(record.getTransfer()!=null)return record.getTransfer();
  Account destinationLookup=accounts.findFirstByAccountNumber(request.destinationAccountNumber().trim().toUpperCase(Locale.ROOT)).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Destination account not found"));
  if(request.sourceAccountId().equals(destinationLookup.getId()))throw new ApiException(HttpStatus.BAD_REQUEST,"Source and destination accounts must differ");
  UUID destinationId=destinationLookup.getId();UUID first=request.sourceAccountId().compareTo(destinationId)<0?request.sourceAccountId():destinationId;UUID second=first.equals(request.sourceAccountId())?destinationId:request.sourceAccountId();
  Account a=accounts.lockById(first).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Account not found"));Account b=accounts.lockById(second).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Account not found"));
  Account source=a.getId().equals(request.sourceAccountId())?a:b;Account dest=a.getId().equals(destinationId)?a:b;
  if(source.isSystemAccount()||dest.isSystemAccount())throw new ApiException(HttpStatus.BAD_REQUEST,"System accounts cannot be used in wallet transfers");
  if(source.getOwner()==null||!source.getOwner().getEmail().equals(email))throw new ApiException(HttpStatus.FORBIDDEN,"Source account is not accessible");
  if(source.getStatus().equals("FROZEN")||source.getStatus().equals("CLOSED")||dest.getStatus().equals("FROZEN")||dest.getStatus().equals("CLOSED"))throw new ApiException(HttpStatus.CONFLICT,"Both accounts must be active");
  if(!source.getCurrency().equals(dest.getCurrency()))throw new ApiException(HttpStatus.BAD_REQUEST,"Account currencies must match");
  if(source.getBalance().compareTo(request.amount())<0)throw new ApiException(HttpStatus.CONFLICT,"Insufficient funds");
  String ref="TR-"+UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase();var tx=txs.save(new LedgerTransaction(ref,"TRANSFER",request.description()));
  entries.saveAll(List.of(new LedgerEntry(tx,source,"DEBIT",request.amount()),new LedgerEntry(tx,dest,"CREDIT",request.amount())));
  source.setBalance(source.getBalance().subtract(request.amount()));dest.setBalance(dest.getBalance().add(request.amount()));accounts.saveAll(List.of(source,dest));
  var transfer=transfers.save(new Transfer(ref,source,dest,request.amount(),source.getCurrency(),request.description(),tx,user));risk.assess(transfer);record.setTransfer(transfer);idem.save(record);audit.record(email,"TRANSFER_COMPLETED","TRANSFER",transfer.getId().toString(),Map.of("reference",ref,"amount",request.amount().toPlainString(),"currency",source.getCurrency()));return transfer;
 }
 @Transactional(readOnly=true) public List<Transfer> list(String email){return transfers.findVisibleByEmail(email);}
 @Transactional public Map<String,Object> fund(String email,UUID accountId,BigDecimal amount){
  if(amount==null||amount.signum()<=0||amount.scale()>2||amount.compareTo(new BigDecimal("100000.00"))>0)throw new ApiException(HttpStatus.BAD_REQUEST,"Funding amount must be between ₹0.01 and ₹100,000.00");
  var user=users.findByEmail(email).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"User not found"));
  UUID clearingId=UUID.fromString("00000000-0000-0000-0000-000000000002");UUID first=accountId.compareTo(clearingId)<0?accountId:clearingId;UUID second=first.equals(clearingId)?accountId:clearingId;
  Account a=accounts.lockById(first).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Account not found"));Account b=accounts.lockById(second).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Account not found"));Account dest=a.getId().equals(accountId)?a:b;Account clearing=a.getId().equals(clearingId)?a:b;
  if(!dest.getOwner().getEmail().equals(email))throw new ApiException(HttpStatus.FORBIDDEN,"Account is not accessible");if(!dest.getStatus().equals("ACTIVE"))throw new ApiException(HttpStatus.CONFLICT,"Account must be active");if(!dest.getCurrency().equals("INR"))throw new ApiException(HttpStatus.BAD_REQUEST,"Funding is only available in INR");if(clearing.getBalance().compareTo(amount)<0)throw new ApiException(HttpStatus.CONFLICT,"Simulated funding pool is depleted");
  String ref="FUND-"+UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase();var tx=txs.save(new LedgerTransaction(ref,"SIMULATED_FUNDING","Development-only simulated funding"));entries.saveAll(List.of(new LedgerEntry(tx,clearing,"DEBIT",amount),new LedgerEntry(tx,dest,"CREDIT",amount)));clearing.setBalance(clearing.getBalance().subtract(amount));dest.setBalance(dest.getBalance().add(amount));accounts.saveAll(List.of(clearing,dest));audit.record(email,"SIMULATED_FUNDING","ACCOUNT",accountId.toString(),Map.of("reference",ref,"amount",amount.toPlainString()));return Map.of("reference",ref,"accountId",accountId,"amount",amount,"currency","INR","simulated",true);
 }
 @Transactional(readOnly=true) public Transfer get(UUID id,String email){return transfers.findById(id).filter(t->t.getInitiatedBy().getEmail().equals(email)||t.getDestination().getOwner().getEmail().equals(email)).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Transfer not found"));}
 @Transactional public Transfer reverse(UUID id,String email){
  Transfer original=transfers.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Transfer not found"));
  if(!original.getInitiatedBy().getEmail().equals(email))throw new ApiException(HttpStatus.NOT_FOUND,"Transfer not found");
  if(!original.getStatus().equals("COMPLETED")||transfers.existsByReversedTransferId(id))throw new ApiException(HttpStatus.CONFLICT,"Transfer is not eligible for reversal");
  UUID x=original.getSource().getId(),y=original.getDestination().getId();UUID first=x.compareTo(y)<0?x:y,second=first.equals(x)?y:x;Account a=accounts.lockById(first).orElseThrow(),b=accounts.lockById(second).orElseThrow();Account source=a.getId().equals(y)?a:b;Account dest=a.getId().equals(x)?a:b;
  if(source.getBalance().compareTo(original.getAmount())<0)throw new ApiException(HttpStatus.CONFLICT,"Destination account has insufficient available balance for reversal");
  String ref="RV-"+UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase();var tx=txs.save(new LedgerTransaction(ref,"REVERSAL","Compensating reversal for "+original.getReference()));
  entries.saveAll(List.of(new LedgerEntry(tx,source,"DEBIT",original.getAmount()),new LedgerEntry(tx,dest,"CREDIT",original.getAmount())));source.setBalance(source.getBalance().subtract(original.getAmount()));dest.setBalance(dest.getBalance().add(original.getAmount()));accounts.saveAll(List.of(source,dest));original.markReversed();transfers.save(original);
  var reversal=transfers.save(new Transfer(ref,source,dest,original.getAmount(),original.getCurrency(),"Reversal of "+original.getReference(),tx,original.getInitiatedBy(),original));audit.record(email,"TRANSFER_REVERSED","TRANSFER",original.getId().toString(),Map.of("reversalReference",ref));return reversal;
 }
 private String fingerprint(Request r){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(r));return HexFormat.of().formatHex(b);}catch(Exception e){throw new IllegalStateException(e);}}
}
