package com.fintechledger.reconciliation;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class ReconciliationService {
    private final JdbcTemplate jdbc;
    public ReconciliationService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Transactional public Map<String,Object> run() {
        int accounts = jdbc.queryForObject("select count(*) from accounts", Integer.class);
        var balances = jdbc.queryForList("""
            select a.id::text as id, a.balance::text as projection,
              coalesce(sum(case when e.direction='CREDIT' then e.amount else -e.amount end),0)::numeric(19,2)::text as calculated
            from accounts a left join ledger_entries e on e.account_id=a.id
            group by a.id,a.balance having a.balance <> coalesce(sum(case when e.direction='CREDIT' then e.amount else -e.amount end),0)
            """);
        var unbalanced = jdbc.queryForList("""
            select t.id::text as id,t.reference,
              coalesce(sum(case when e.direction='DEBIT' then e.amount else 0 end),0)::text as debits,
              coalesce(sum(case when e.direction='CREDIT' then e.amount else 0 end),0)::text as credits
            from ledger_transactions t left join ledger_entries e on e.transaction_id=t.id
            where t.status='POSTED' group by t.id,t.reference
            having coalesce(sum(case when e.direction='DEBIT' then e.amount else 0 end),0) <> coalesce(sum(case when e.direction='CREDIT' then e.amount else 0 end),0)
            """);
        var settlements = jdbc.queryForList("""
            select s.external_reference,s.transfer_reference,s.amount::text as settlement_amount,t.amount::text as transfer_amount
            from simulated_settlements s left join transfers t on t.reference=s.transfer_reference
            where t.id is null or s.amount <> t.amount or s.currency <> t.currency
            """);
        String runId=UUID.randomUUID().toString(); int total=balances.size()+unbalanced.size()+settlements.size();
        jdbc.update("insert into reconciliation_runs(id,status,accounts_checked,unbalanced_transactions,balance_discrepancies) values (?::uuid,?,?,?,?)",runId,total==0?"CLEAN":"DISCREPANCIES",accounts,unbalanced.size(),balances.size()+settlements.size());
        for(var row:balances)jdbc.update("insert into reconciliation_discrepancies(run_id,type,resource_id,expected_value,actual_value,description) values (?::uuid,'BALANCE_MISMATCH',?,?,?,'Ledger-derived balance differs from cached projection')",runId,row.get("id"),row.get("calculated"),row.get("projection"));
        for(var row:unbalanced)jdbc.update("insert into reconciliation_discrepancies(run_id,type,resource_id,expected_value,actual_value,description) values (?::uuid,'UNBALANCED_TRANSACTION',?,?,?,'Posted debits do not equal posted credits')",runId,row.get("id"),row.get("debits"),row.get("credits"));
        for(var row:settlements)jdbc.update("insert into reconciliation_discrepancies(run_id,type,resource_id,expected_value,actual_value,description) values (?::uuid,'SETTLEMENT_MISMATCH',?,?,?,'Settlement is missing its matching transfer or has a different amount/currency')",runId,row.get("external_reference"),row.get("transfer_amount"),row.get("settlement_amount"));
        return Map.of("id",runId,"status",total==0?"CLEAN":"DISCREPANCIES","accountsChecked",accounts,"unbalancedTransactions",unbalanced.size(),"balanceDiscrepancies",balances.size(),"settlementDiscrepancies",settlements.size(),"discrepancies",total);
    }
    @Transactional(readOnly=true) public List<Map<String,Object>> runs(){return jdbc.queryForList("select id,status,accounts_checked as \"accountsChecked\",unbalanced_transactions as \"unbalancedTransactions\",balance_discrepancies as \"balanceDiscrepancies\",created_at as \"createdAt\" from reconciliation_runs order by created_at desc limit 100");}
    @Transactional(readOnly=true) public Map<String,Object> detail(UUID id){var run=jdbc.queryForMap("select id,status,accounts_checked as \"accountsChecked\",unbalanced_transactions as \"unbalancedTransactions\",balance_discrepancies as \"balanceDiscrepancies\",created_at as \"createdAt\" from reconciliation_runs where id=?",id);run.put("discrepancies",jdbc.queryForList("select type,resource_id as \"resourceId\",expected_value as \"expectedValue\",actual_value as \"actualValue\",description from reconciliation_discrepancies where run_id=?",id));return run;}
}
