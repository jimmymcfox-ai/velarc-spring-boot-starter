package io.velarc.sdk.exception;

/**
 * Checked exception thrown when an AI provider error is surfaced
 * via Velarc HTTP 502 response.
 */
public class VelarcProviderException extends Exception {

    private final String providerName;
    private final String providerErrorCode;
    private final String providerMessage;

    public VelarcProviderException(String message, String providerName,
                                   String providerErrorCode, String providerMessage) {
        super(message);
        this.providerName = providerName;
        this.providerErrorCode = providerErrorCode;
        this.providerMessage = providerMessage;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getProviderErrorCode() {
        return providerErrorCode;
    }

    public String getProviderMessage() {
        return providerMessage;
    }
}
