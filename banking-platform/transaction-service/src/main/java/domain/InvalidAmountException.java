package domain;

import java.math.BigDecimal;

public class InvalidAmountException extends RuntimeException {
    public InvalidAmountException(BigDecimal amount) {
        super(String.format("Le montant doit être strictement positif (valeur fournie : %s).", amount));
    }

    public InvalidAmountException(String message) {
        super(message);
    }
}
