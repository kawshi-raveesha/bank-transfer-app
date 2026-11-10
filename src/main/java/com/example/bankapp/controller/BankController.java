package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService transactionAuditService;

    
    // 1. GET / (Dashboard)
    
        // ==========================================
    // 1. GET / (Dashboard)
    // ==========================================
       @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        List<Customer> customers;
        if (keyword != null && !keyword.trim().isEmpty()) {
            customers = bankService.searchByName(keyword);
        } else {
            customers = bankService.findAllOrdered();
        }

        // These are for the dropdowns and the customers table
        model.addAttribute("customers", customers);
        model.addAttribute("allCustomers", customers); 
        
        // Forms
        model.addAttribute("customer", new Customer()); 
        model.addAttribute("transferRequest", new TransferRequest()); 

        // Dashboard Stats
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("totalCustomers", bankService.countCustomersNative());
        model.addAttribute("successCount", bankService.countByStatus(TransactionStatus.SUCCESS));
        model.addAttribute("failedCount", bankService.countByStatus(TransactionStatus.FAILED));

        // 🚨 THIS IS THE FIX FOR THE TABLE 🚨
        List<BankTransaction> recent = bankService.findAllLatest();
        model.addAttribute("transactions", recent);          // <-- HTML uses this
        model.addAttribute("latestTransactions", recent);    // <-- Backup just in case

        return "bank";
    }

      
    
    // 2. POST /transfer (Transfer Money)
    
    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transferRequest") TransferRequest request, 
                           BindingResult bindingResult, 
                           RedirectAttributes redirectAttributes) {
        
        // If form validation fails (e.g., amount is empty or negative), go back to dashboard
                // If form validation fails (e.g., amount is empty, or missing in Postman)
                if (bindingResult.hasErrors()) {
            // WE NEED TO SEE THE ERROR IN THE TERMINAL
            System.out.println("VALIDATION FAILED: " + bindingResult.getAllErrors());
            
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid input data.");
            return "redirect:/";
        }
        try {
            // Call the service to perform the transaction
            BankTransaction transaction = bankService.transfer(request, request.isSimulateFailure());
            redirectAttributes.addFlashAttribute("successMessage", "Transfer successful! Transaction ID: " + transaction.getId());
            
        } catch (Exception e) {
            // IF ANYTHING FAILS, Spring has rolled back the transfer.
            // Now, we catch the exception and log it in a NEW transaction (Audit Service).
            transactionAuditService.recordFailure(
                    request.getFromAccount(), 
                    request.getToAccount(), 
                    request.getAmount(), 
                    e.getMessage()
            );
            
            // Show the error message on the UI
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        // Redirect back to the dashboard to prevent duplicate form submissions on refresh
        return "redirect:/";
    }

   
    // 3. POST /customers (Add Customer)
    // ==========================================
    @PostMapping("/customers")
    public String addCustomer(@ModelAttribute Customer customer, RedirectAttributes redirectAttributes) {
        try {
            bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("successMessage", "Customer added successfully!");
        } catch (BankException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/";
    }

    // ==========================================
    // 4. GET /accounts/{accountNumber}/history
    // ==========================================
    @GetMapping("/accounts/{accountNumber}/history")
    public String history(@PathVariable String accountNumber, Model model) {
        Customer customer = bankService.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        
        List<BankTransaction> transactions = bankService.findHistory(accountNumber);
        
        model.addAttribute("customer", customer);
        model.addAttribute("transactions", transactions);

                // For the stats
        model.addAttribute("latestTransactions", bankService.findAllLatest());
        
        // Add these aliases just in case the instructor's HTML uses a different name:
        model.addAttribute("transactions", bankService.findAllLatest());
        model.addAttribute("allTransactions", bankService.findAllLatest());
        model.addAttribute("recentTransactions", bankService.findAllLatest());
        
        return "history"; // This refers to history.html
    }
}