package com.example.bankapp.service;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime; 

/**
 * Saves a FAILED record after a transfer was rolled back.
 * REQUIRES_NEW = always start a brand-new, independent transaction,
 * so this record is committed even though the transfer itself was rolled back.
 */
@Service
@RequiredArgsConstructor
public class TransactionAuditService {

    // Inject the repository using Lombok's @RequiredArgsConstructor
    private final BankTransactionRepository transactionRepository;

    // The annotation that makes the magic happen!
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String fromAccount, String toAccount, BigDecimal amount, String errorMessage) {
        
        BankTransaction failedTx = new BankTransaction();
        failedTx.setFromAccount(fromAccount);
        failedTx.setToAccount(toAccount);
        failedTx.setAmount(amount);
        failedTx.setStatus(TransactionStatus.FAILED);
        failedTx.setDescription(errorMessage);
        failedTx.setCreatedAt(LocalDateTime.now());
        
        transactionRepository.save(failedTx);
    }
}