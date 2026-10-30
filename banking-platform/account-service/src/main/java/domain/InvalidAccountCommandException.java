package domain;

public class InvalidAccountCommandException extends RuntimeException {
    public InvalidAccountCommandException(String message) {
        super(message);
    }

    public InvalidAccountCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}