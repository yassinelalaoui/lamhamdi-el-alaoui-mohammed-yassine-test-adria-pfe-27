package consumer;

import domain.TransactionCreatedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TransactionEventConsumerTest {

    @Test
    void consumesAndLogsEventWithoutException() {
        TransactionEventConsumer consumer = new TransactionEventConsumer();
        UUID accountId = UUID.randomUUID();
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(),
                "TransactionCreated",
                UUID.randomUUID(),
                accountId,
                new BigDecimal("250.00"),
                "EUR",
                Instant.now());

        assertDoesNotThrow(() -> consumer.consume(event, accountId.toString(), 0, 101L));
    }
}
