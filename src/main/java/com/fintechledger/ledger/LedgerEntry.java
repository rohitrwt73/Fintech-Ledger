package com.fintechledger.ledger;
import com.fintechledger.account.Account; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="ledger_entries") public class LedgerEntry {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="transaction_id",nullable=false) private LedgerTransaction transaction;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="account_id",nullable=false) private Account account;
 @Column(nullable=false) private String direction; @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now(); protected LedgerEntry(){}
 public LedgerEntry(LedgerTransaction tx,Account account,String direction,BigDecimal amount){this.transaction=tx;this.account=account;this.direction=direction;this.amount=amount;}
 public UUID getId(){return id;} public LedgerTransaction getTransaction(){return transaction;} public Account getAccount(){return account;} public String getDirection(){return direction;} public BigDecimal getAmount(){return amount;} public Instant getCreatedAt(){return createdAt;}
}
