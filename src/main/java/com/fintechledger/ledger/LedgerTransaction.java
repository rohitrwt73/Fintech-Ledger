package com.fintechledger.ledger;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="ledger_transactions") public class LedgerTransaction {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @Column(nullable=false,unique=true) private String reference;
 @Column(nullable=false) private String type; @Column(nullable=false) private String status="POSTED"; private String description;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now(); protected LedgerTransaction(){}
 public LedgerTransaction(String reference,String type,String description){this.reference=reference;this.type=type;this.description=description;}
 public UUID getId(){return id;} public String getReference(){return reference;} public String getStatus(){return status;} public Instant getCreatedAt(){return createdAt;}
}
