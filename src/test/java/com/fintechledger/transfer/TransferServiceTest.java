package com.fintechledger.transfer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintechledger.account.*;
import com.fintechledger.audit.AuditService;
import com.fintechledger.common.ApiException;
import com.fintechledger.ledger.*;
import com.fintechledger.risk.RiskService;
import com.fintechledger.user.*;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class TransferServiceTest {
    AccountRepository accounts; UserRepository users; TransferRepository transfers;
    LedgerEntryRepository entries; LedgerTransactionRepository transactions;
    IdempotencyRepository idempotency; ObjectMapper mapper; TransferService service;
    RiskService risk; AuditService audit; AppUser user; UUID sourceId,destId;

    @BeforeEach void setup() {
        accounts=mock(AccountRepository.class);users=mock(UserRepository.class);transfers=mock(TransferRepository.class);
        entries=mock(LedgerEntryRepository.class);transactions=mock(LedgerTransactionRepository.class);idempotency=mock(IdempotencyRepository.class);
        mapper=new ObjectMapper();risk=mock(RiskService.class);audit=mock(AuditService.class);
        service=new TransferService(accounts,users,transfers,entries,transactions,idempotency,mapper,risk,audit);
        user=mock(AppUser.class);when(user.getEmail()).thenReturn("user@example.com");when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        sourceId=UUID.fromString("00000000-0000-0000-0000-000000000010");destId=UUID.fromString("00000000-0000-0000-0000-000000000020");
    }
    @Test void rejectsSameAccountTransfersBeforeAccountLocking() {
        var request=new TransferService.Request(sourceId,"IN0000000000000001",new BigDecimal("10.00"),"test");prepareKey(request,"key");Account same=mock(Account.class);when(same.getId()).thenReturn(sourceId);when(accounts.findFirstByAccountNumber(request.destinationAccountNumber())).thenReturn(Optional.of(same));
        ApiException error=assertThrows(ApiException.class,()->service.create("user@example.com","key",request));
        assertEquals(HttpStatus.BAD_REQUEST,error.status());verify(accounts,never()).lockById(any());
    }
    @Test void rejectsInsufficientFundsWithoutPostingLedgerRows() {
        var request=new TransferService.Request(sourceId,"IN0000000000000002",new BigDecimal("10.00"),"test");prepareKey(request,"key");
        Account source=mock(Account.class),dest=mock(Account.class);when(source.getId()).thenReturn(sourceId);when(dest.getId()).thenReturn(destId);
        when(accounts.findFirstByAccountNumber(request.destinationAccountNumber())).thenReturn(Optional.of(dest));
        when(source.getOwner()).thenReturn(user);when(source.getStatus()).thenReturn("ACTIVE");when(dest.getStatus()).thenReturn("ACTIVE");
        when(source.getCurrency()).thenReturn("INR");when(dest.getCurrency()).thenReturn("INR");when(source.getBalance()).thenReturn(new BigDecimal("9.99"));
        when(accounts.lockById(sourceId)).thenReturn(Optional.of(source));when(accounts.lockById(destId)).thenReturn(Optional.of(dest));
        ApiException error=assertThrows(ApiException.class,()->service.create("user@example.com","key",request));
        assertEquals(HttpStatus.CONFLICT,error.status());verify(entries,never()).saveAll(any());verify(transactions,never()).save(any());
    }
    @Test void rejectsReusingAnIdempotencyKeyForAnotherPayload() {
        var original=new TransferService.Request(sourceId,"IN0000000000000002",new BigDecimal("10.00"),"one");prepareKey(original,"key");
        var changed=new TransferService.Request(sourceId,"IN0000000000000002",new BigDecimal("11.00"),"two");
        ApiException error=assertThrows(ApiException.class,()->service.create("user@example.com","key",changed));
        assertEquals(HttpStatus.CONFLICT,error.status());verify(accounts,never()).lockById(any());
    }
    private void prepareKey(TransferService.Request request,String key) {
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(request));
            String hash=HexFormat.of().formatHex(digest);IdempotencyRecord record=mock(IdempotencyRecord.class);
            when(record.getRequestHash()).thenReturn(hash);when(record.getTransfer()).thenReturn(null);
            when(idempotency.lockByUserEmailAndKey("user@example.com",key)).thenReturn(Optional.of(record));
        } catch(Exception e){throw new IllegalStateException(e);}
    }
}
