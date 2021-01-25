package dev.JustRed23.Exceptions;

public class InvalidDescriptionException extends Exception {

    public InvalidDescriptionException(final Throwable cause, final String message) {
        super(message, cause);
    }

    public InvalidDescriptionException(final Throwable cause) {
        super("Invalid app.yml", cause);
    }

    public InvalidDescriptionException(final String message) {
        super(message);
    }

    public InvalidDescriptionException() {
        super("Invalid app.yml");
    }
}
