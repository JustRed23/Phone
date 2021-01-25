package dev.JustRed23.Exceptions;

public class InvalidAppException extends Exception {

    public InvalidAppException(final Throwable cause) {
        super(cause);
    }

    public InvalidAppException() {}

    public InvalidAppException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public InvalidAppException(final String message) {
        super(message);
    }
}
