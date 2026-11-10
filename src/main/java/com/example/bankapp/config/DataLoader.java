package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Inserts three sample customers the first time the application starts (empty database). */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    // Inject the repository so we can save data to MySQL
    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) throws Exception {
        // Only insert data if the table is currently empty
        if (customerRepository.count() == 0) {
            
            Customer c1 = new Customer();
            c1.setAccountNumber("100001");
            c1.setFullName("Kamal Perera");
            c1.setEmail("kamal@example.com");
            c1.setBalance(new BigDecimal("50000.00")); // LKR 50,000

            Customer c2 = new Customer();
            c2.setAccountNumber("100002");
            c2.setFullName("Nimali Silva");
            c2.setEmail("nimali@example.com");
            c2.setBalance(new BigDecimal("25000.00")); // LKR 25,000

            Customer c3 = new Customer();
            c3.setAccountNumber("100003");
            c3.setFullName("Saman Fernando");
            c3.setEmail("saman@example.com");
            c3.setBalance(new BigDecimal("10000.00")); // LKR 10,000

            // Save all three to the database
            customerRepository.save(c1);
            customerRepository.save(c2);
            customerRepository.save(c3);
            
            System.out.println("✅ Sample data loaded successfully. Total money: LKR 85,000.00");
        } else {
            System.out.println("ℹ️ Database already contains data. Skipping sample data loading.");
        }
    }
}