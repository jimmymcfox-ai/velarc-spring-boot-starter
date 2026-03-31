package io.velarc.sdk.exception;

/**
 * Abstract base exception for all unchecked Velarc SDK errors.
 */
public abstract class VelarcException extends RuntimeException {

    protected VelarcException(String message) {
        super(message);
    }

    protected VelarcException(String message, Throwable cause) {
        super(message, cause);
    }
}
