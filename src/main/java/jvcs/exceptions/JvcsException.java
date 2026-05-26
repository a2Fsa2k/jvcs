package jvcs.exceptions;

public class JvcsException extends RuntimeException {
    public JvcsException(String message) {
        super(message);
    }

    public JvcsException(String message, Throwable cause) {
        super(message, cause);
    }
}
