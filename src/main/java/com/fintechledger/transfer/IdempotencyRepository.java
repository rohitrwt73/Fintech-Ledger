package com.fintechledger.transfer;
import java.util.*; import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord,UUID>{
 Optional<IdempotencyRecord> findByUserEmailAndKey(String email,String key);
 @Modifying @Query(value="insert into idempotency_records(user_id,idempotency_key,request_hash) select id,:key,:hash from app_users where email=:email on conflict(user_id,idempotency_key) do nothing",nativeQuery=true)
 int insertIfAbsent(@Param("email")String email,@Param("key")String key,@Param("hash")String hash);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from IdempotencyRecord i where i.user.email=:email and i.key=:key") Optional<IdempotencyRecord> lockByUserEmailAndKey(@Param("email")String email,@Param("key")String key);
}
