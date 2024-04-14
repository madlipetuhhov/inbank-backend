package ee.taltech.inbankbackend.infrastructure.exceptions;

/**
 * Throw when the age does not fall within the allowed range (18-80) based on the given personal ID code.
 */
public class InvalidAgeException extends Throwable {

    private final String message;
    private final Throwable cause;

    public InvalidAgeException(String message) {
        this(message, null);
    }

    public InvalidAgeException(String message, Throwable cause) {
        this.message = message;
        this.cause = cause;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
