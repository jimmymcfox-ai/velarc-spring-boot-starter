package io.velarc.sdk.exception;

/**
 * Thrown for configuration errors such as missing provider API key,
 * use case not in cache, SDK not initialised, or invalid/expired Velarc API key.
 * Not retryable.
 */
public class VelarcConfigException extends VelarcException {

    public VelarcConfigException(String message) {
        super(message);
    }
}
