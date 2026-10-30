package domain;

public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(String transactionCurrency, String accountCurrency) {
        super(String.format("La devise de la transaction (%s) ne correspond pas à la devise du compte (%s).",
                transactionCurrency, accountCurrency));
    }
}
