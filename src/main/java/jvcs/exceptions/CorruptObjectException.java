package jvcs.exceptions;

public class CorruptObjectException extends JvcsException {
    public CorruptObjectException(String message) {
        super(message);
    }

    public CorruptObjectException(String message, Throwable cause) {
        super(message, cause);
    }
}
