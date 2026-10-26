package com.fintechledger.audit;
import com.fasterxml.jackson.databind.ObjectMapper;import com.fintechledger.user.UserRepository;import java.util.Map;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
@Service public class AuditService{
 private final JdbcTemplate jdbc;private final UserRepository users;private final ObjectMapper mapper;
 public AuditService(JdbcTemplate jdbc,UserRepository users,ObjectMapper mapper){this.jdbc=jdbc;this.users=users;this.mapper=mapper;}
 @Transactional public void record(String email,String action,String type,String id,Map<String,?> metadata){try{String actor=users.findByEmail(email).map(u->u.getId().toString()).orElse(null);jdbc.update("insert into audit_events(actor_id,action,resource_type,resource_id,metadata) values (?::uuid,?,?,?,?::jsonb)",actor,action,type,id,mapper.writeValueAsString(metadata));}catch(Exception e){throw new IllegalStateException("Unable to persist audit event",e);}}
}
