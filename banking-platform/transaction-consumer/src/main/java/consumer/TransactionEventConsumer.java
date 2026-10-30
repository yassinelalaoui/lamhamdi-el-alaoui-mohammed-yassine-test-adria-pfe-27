package consumer;

import domain.TransactionCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Consumes events from the {@code bank.transactions.created} Kafka topic.
 *
 * <p>Requirement: logs the received event without modifying any banking data.
 */
@Service
public class TransactionEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    @KafkaListener(
            topics = "${bank.kafka.topic.transactions-created:bank.transactions.created}",
            groupId = "${spring.kafka.consumer.group-id:transaction-audit-consumer}")
    public void consume(
            @Payload TransactionCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String messageKey,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info(
                "Audit Log [Kafka Event Received] | topic=bank.transactions.created | partition={} | offset={} | key={} | eventId={} | eventType={} | transactionId={} | accountId={} | amount={} {} | occurredAt={}",
                partition,
                offset,
                messageKey,
                event.eventId(),
                event.eventType(),
                event.transactionId(),
                event.accountId(),
                event.amount(),
                event.currency(),
                event.occurredAt());
    }
}
