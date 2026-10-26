package com.fintechledger.account;
import com.fintechledger.user.AppUser; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="accounts") public class Account {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(name="account_number",nullable=false,unique=true) private String accountNumber;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="owner_id") private AppUser owner;
 @Column(nullable=false) private String currency="INR"; @Column(nullable=false) private String status="ACTIVE";
 @Column(name="system_account",nullable=false) private boolean systemAccount; @Column(nullable=false,precision=19,scale=2) private BigDecimal balance=BigDecimal.ZERO;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now(); protected Account(){}
 public Account(String accountNumber,AppUser owner){this.accountNumber=accountNumber;this.owner=owner;}
 public UUID getId(){return id;} public String getAccountNumber(){return accountNumber;} public AppUser getOwner(){return owner;} public String getCurrency(){return currency;} public String getStatus(){return status;} public BigDecimal getBalance(){return balance;}
 public void setBalance(BigDecimal v){balance=v;} public boolean isSystemAccount(){return systemAccount;}
}
