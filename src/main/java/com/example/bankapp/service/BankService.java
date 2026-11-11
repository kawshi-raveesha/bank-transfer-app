package com.example.bankapp.service;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    
           @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(TransferRequest request, boolean simulateFailure) {
        if (request.getFromAccount().equals(request.getToAccount())) {
            throw new BankException("Sender and receiver must be different accounts");
        }

        // Validate all 3 accounts exist (Sender, Receiver, and Bank Fee Account)
        customerRepository.getByAccountNumber(request.getFromAccount())
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found"));
        customerRepository.getByAccountNumber(request.getToAccount())
                .orElseThrow(() -> new AccountNotFoundException("Receiver account not found"));
        customerRepository.getByAccountNumber("999999")
                .orElseThrow(() -> new AccountNotFoundException("Bank fee account 999999 not found. Please create it first."));

        // 1. Calculate 1% fee (rounded to 2 decimal places)
        BigDecimal fee = request.getAmount().multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDeduction = request.getAmount().add(fee);

        // 2. Debit sender (Amount + Fee)
        int rowsUpdated = customerRepository.debit(request.getFromAccount(), totalDeduction);
        if (rowsUpdated == 0) {
            throw new InsufficientBalanceException("Insufficient balance (including 1% fee) in account " + request.getFromAccount());
        }

        if (simulateFailure) {
            throw new RuntimeException("Simulated system failure after withdrawal");
        }

        // 3. Credit receiver (Amount only)
        customerRepository.credit(request.getToAccount(), request.getAmount());
        
        // 4. Credit Bank Fee Account (Fee only)
        customerRepository.credit("999999", fee);

        // 5. Save SUCCESS record
        BankTransaction transaction = new BankTransaction();
        transaction.setFromAccount(request.getFromAccount());
        transaction.setToAccount(request.getToAccount());
        transaction.setAmount(request.getAmount());
        transaction.setStatus(TransactionStatus.SUCCESS);
        // Add the fee to the description so you can see it in the table
        transaction.setDescription("Transfer completed (Fee: LKR " + fee + ")"); 
        transaction.setCreatedAt(LocalDateTime.now());
        
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        if (customerRepository.countAccounts(customer.getAccountNumber()) > 0) {
            throw new BankException("Account number already exists");
        }
        if (customerRepository.countEmails(customer.getEmail()) > 0) {
            throw new BankException("Email already exists");
        }
        return customerRepository.save(customer);
    }

        @Transactional(rollbackFor = Exception.class)
    public void deposit(String accountNumber, BigDecimal amount) {
        // 1. Check if the account exists
        customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        // 2. Add the money using the credit query
        customerRepository.credit(accountNumber, amount);

        // 3. Record the transaction in the database
        BankTransaction tx = new BankTransaction();
        tx.setToAccount(accountNumber); // Money goes TO this account
        tx.setAmount(amount);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setDescription("Deposit");
        tx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(tx);
    }

    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String accountNumber, BigDecimal amount) {
        // 1. Check if the account exists
        customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        // 2. Deduct the money using the debit query
        int rowsUpdated = customerRepository.debit(accountNumber, amount);
        if (rowsUpdated == 0) {
            throw new InsufficientBalanceException("Insufficient balance for withdrawal");
        }

        // 3. Record the transaction in the database
        BankTransaction tx = new BankTransaction();
        tx.setFromAccount(accountNumber); // Money goes FROM this account
        tx.setAmount(amount);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setDescription("Withdrawal");
        tx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(tx);
    }
    
    // ==========================================

    public List<Customer> searchByName(String keyword) {
        return customerRepository.searchByName(keyword);
    }

    public List<Customer> findAllOrdered() {
        return customerRepository.findAllOrdered();
    }

    public BigDecimal getTotalBalance() {
        return customerRepository.getTotalBalance();
    }

    public long countCustomersNative() {
        return customerRepository.countCustomersNative();
    }

    public List<BankTransaction> findAllLatest() {
        return transactionRepository.findAllLatest();
    }

    public long countByStatus(TransactionStatus status) {
        return transactionRepository.countByStatus(status);
    }

    public Optional<Customer> getByAccountNumber(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber);
    }

    public List<BankTransaction> findHistory(String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }

    public List<BankTransaction> findLargeTransfers(BigDecimal min) {
        return transactionRepository.findLargeTransfers(min);

    }

        public BigDecimal getTotalSentByAccount(String accountNumber) {
        BigDecimal total = transactionRepository.getTotalSentByAccount(accountNumber);
        return total != null ? total : BigDecimal.ZERO;
    }
}