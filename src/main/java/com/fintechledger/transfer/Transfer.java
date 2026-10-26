package com.fintechledger.transfer;
import com.fintechledger.account.Account; import com.fintechledger.ledger.LedgerTransaction; import com.fintechledger.user.AppUser; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="transfers") public class Transfer {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @Column(nullable=false,unique=true) private String reference;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="source_account_id",nullable=false) private Account source;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="destination_account_id",nullable=false) private Account destination;
 @Column(nullable=false,precision=19,scale=2) private BigDecimal amount; @Column(nullable=false) private String currency;
 private String description; @Column(nullable=false) private String status="COMPLETED";
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="ledger_transaction_id",nullable=false) private LedgerTransaction ledgerTransaction;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="initiated_by",nullable=false) private AppUser initiatedBy;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reversed_transfer_id") private Transfer reversedTransfer;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now(); protected Transfer(){}
 public Transfer(String ref,Account src,Account dst,BigDecimal amount,String currency,String description,LedgerTransaction tx,AppUser user){this.reference=ref;source=src;destination=dst;this.amount=amount;this.currency=currency;this.description=description;ledgerTransaction=tx;initiatedBy=user;}
 public Transfer(String ref,Account src,Account dst,BigDecimal amount,String currency,String description,LedgerTransaction tx,AppUser user,Transfer reversed){this(ref,src,dst,amount,currency,description,tx,user);reversedTransfer=reversed;}
 public UUID getId(){return id;} public String getReference(){return reference;} public Account getSource(){return source;} public Account getDestination(){return destination;} public BigDecimal getAmount(){return amount;} public String getCurrency(){return currency;} public String getDescription(){return description;} public String getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public LedgerTransaction getLedgerTransaction(){return ledgerTransaction;} public AppUser getInitiatedBy(){return initiatedBy;}
 public void markReversed(){status="REVERSED";}
}
