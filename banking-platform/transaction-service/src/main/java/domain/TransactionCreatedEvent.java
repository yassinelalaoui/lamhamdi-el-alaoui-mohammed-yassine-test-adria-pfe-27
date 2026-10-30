package domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kafka event published after a transaction is successfully persisted.
 *
 * <p>The event is serialized to JSON and published to the {@code bank.transactions.created} topic
 * using the account ID as the message key.
 */
public record TransactionCreatedEvent(
        UUID eventId,
        String eventType,
        UUID transactionId,
        UUID accountId,
        BigDecimal amount,
        String currency,
        java.time.Instant occurredAt) {

    public static final String EVENT_TYPE = "TransactionCreated";

    public static TransactionCreatedEvent from(Transaction transaction) {
        return new TransactionCreatedEvent(
                UUID.randomUUID(),
                EVENT_TYPE,
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getCreatedAt());
    }
}
