package io.velarc.sdk.exception;

/**
 * Checked exception thrown when the Velarc proxy is unreachable,
 * a connection timeout occurs, or a 5xx response is received.
 * Retryable.
 */
public class VelarcServiceException extends Exception {

    public VelarcServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
