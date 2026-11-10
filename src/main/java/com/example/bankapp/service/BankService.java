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

        // Validate existence (removed the variable assignment to fix the warning)
        customerRepository.getByAccountNumber(request.getFromAccount())
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found"));
        customerRepository.getByAccountNumber(request.getToAccount())
                .orElseThrow(() -> new AccountNotFoundException("Receiver account not found"));

        int rowsUpdated = customerRepository.debit(request.getFromAccount(), request.getAmount());
        if (rowsUpdated == 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + request.getFromAccount());
        }

        if (simulateFailure) {
            throw new RuntimeException("Simulated system failure after withdrawal");
        }

        customerRepository.credit(request.getToAccount(), request.getAmount());

        BankTransaction transaction = new BankTransaction();
        transaction.setFromAccount(request.getFromAccount());
        transaction.setToAccount(request.getToAccount());
        transaction.setAmount(request.getAmount());
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Transfer completed");
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

    // ==========================================
    // THESE ARE THE METHODS YOUR CONTROLLER NEEDS
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
}