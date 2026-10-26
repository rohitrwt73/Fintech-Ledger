package com.fintechledger.transfer;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.data.jpa.repository.Query;import org.springframework.data.repository.query.Param;
public interface TransferRepository extends JpaRepository<Transfer,UUID>{
 @Query("select t from Transfer t where t.initiatedBy.email=:email or t.destination.owner.email=:email order by t.createdAt desc") List<Transfer> findVisibleByEmail(@Param("email")String email);
 boolean existsByReversedTransferId(UUID id);long countByInitiatedByEmailAndCreatedAtAfter(String email,java.time.Instant since);
}
