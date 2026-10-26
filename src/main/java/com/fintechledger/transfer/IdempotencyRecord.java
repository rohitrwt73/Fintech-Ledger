package com.fintechledger.transfer;
import com.fintechledger.user.AppUser; import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="idempotency_records",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","idempotency_key"})) public class IdempotencyRecord{
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private AppUser user;
 @Column(name="idempotency_key",nullable=false) private String key; @Column(name="request_hash",nullable=false) private String requestHash;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="transfer_id") private Transfer transfer; @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now(); protected IdempotencyRecord(){}
 public IdempotencyRecord(AppUser u,String k,String h){user=u;key=k;requestHash=h;} public String getRequestHash(){return requestHash;} public Transfer getTransfer(){return transfer;} public void setTransfer(Transfer t){transfer=t;}
}
