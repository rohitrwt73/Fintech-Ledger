package com.fintechledger.account;
import com.fintechledger.common.ApiException; import com.fintechledger.user.UserRepository; import com.fintechledger.audit.AuditService; import java.util.*; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class AccountService{
 private final AccountRepository accounts; private final UserRepository users;private final AuditService audit; public AccountService(AccountRepository a,UserRepository u,AuditService audit){accounts=a;users=u;this.audit=audit;}
 @Transactional public Account create(String email){var owner=users.findByEmail(email).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"User not found"));String number="IN"+UUID.randomUUID().toString().replace("-","").substring(0,16).toUpperCase();var account=accounts.save(new Account(number,owner));audit.record(email,"ACCOUNT_CREATED","ACCOUNT",account.getId().toString(),Map.of("currency",account.getCurrency()));return account;}
 @Transactional(readOnly=true) public List<Account> list(String email){return accounts.findByOwnerEmailOrderByCreatedAtDesc(email);}
 @Transactional(readOnly=true) public Account get(UUID id,String email){return accounts.findByIdAndOwnerEmail(id,email).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Account not found"));}
}
