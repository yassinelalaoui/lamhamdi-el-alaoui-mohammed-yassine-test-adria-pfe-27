package service;

import domain.TransactionCreatedEvent;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link TransactionCreatedEvent} records to the {@code bank.transactions.created} Kafka
 * topic using the account ID as the message key (as required by Part F).
 */
@Component
public class TransactionEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(TransactionEventPublisher.class);
    public static final String TOPIC = "bank.transactions.created";

    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    public TransactionEventPublisher(KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TransactionCreatedEvent event) {
        String key = event.accountId().toString();
        CompletableFuture<SendResult<String, TransactionCreatedEvent>> future =
                kafkaTemplate.send(TOPIC, key, event);
        future.whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(
                        "Failed to publish TransactionCreatedEvent [transactionId={}, accountId={}]",
                        event.transactionId(),
                        event.accountId(),
                        exception);
            } else {
                log.info(
                        "Published TransactionCreatedEvent [transactionId={}, accountId={}, offset={}]",
                        event.transactionId(),
                        event.accountId(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
