package application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"application", "consumer"})
public class TransactionConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransactionConsumerApplication.class, args);
    }
}
