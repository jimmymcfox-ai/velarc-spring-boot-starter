package io.velarc.sdk.exception;

/**
 * Thrown for governance violations returned as HTTP 422 responses:
 * unknown use case, unknown user, or invalid business object ID.
 * Not retryable.
 */
public class VelarcGovernanceException extends VelarcException {

    private final String errorCode;
    private final int httpStatus;

    public VelarcGovernanceException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
