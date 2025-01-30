package it.gov.pagopa.self.expense;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
public class SelfExpenseBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SelfExpenseBackendApplication.class, args);
    }
}
