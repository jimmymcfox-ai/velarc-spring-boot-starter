package io.velarc.sdk.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * The response from the Velarc proxy chat endpoint.
 *
 * @param traceId             the trace identifier for multi-turn conversations
 * @param responseText        the first text content from the provider response
 * @param provider            the provider that handled the request
 * @param model               the model that generated the response
 * @param rawProviderResponse the raw JSON response body from the provider
 * @param tokensIn            input token count, or {@code null} if unavailable
 * @param tokensOut           output token count, or {@code null} if unavailable
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record VelarcResponse(
        String traceId,
        String responseText,
        String provider,
        String model,
        String rawProviderResponse,
        Integer tokensIn,
        Integer tokensOut
) {
}
