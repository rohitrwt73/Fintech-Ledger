package com.fintechledger.ledger;
import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction,UUID>{}
