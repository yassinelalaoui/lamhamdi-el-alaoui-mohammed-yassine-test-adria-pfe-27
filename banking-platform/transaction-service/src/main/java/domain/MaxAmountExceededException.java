package domain;

import java.math.BigDecimal;

public class MaxAmountExceededException extends RuntimeException {
    public MaxAmountExceededException(BigDecimal amount, BigDecimal maxAmount) {
        super(String.format("Le montant %s dépasse le plafond configuré de %s.", amount, maxAmount));
    }
}
