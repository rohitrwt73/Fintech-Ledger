package com.fintechledger.account;
import java.util.*; import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface AccountRepository extends JpaRepository<Account,UUID>{
 List<Account> findByOwnerEmailOrderByCreatedAtDesc(String email);
 Optional<Account> findByIdAndOwnerEmail(UUID id,String email);
 Optional<Account> findFirstByAccountNumber(String accountNumber);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from Account a where a.id = :id") Optional<Account> lockById(@Param("id") UUID id);
}
