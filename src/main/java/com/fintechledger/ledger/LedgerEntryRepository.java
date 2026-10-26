package com.fintechledger.ledger;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry,UUID>{List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId);}
