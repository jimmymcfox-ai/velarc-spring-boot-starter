package io.velarc.sdk.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

/**
 * The response from the Velarc proxy chat endpoint.
 *
 * @param traceId          the trace identifier for multi-turn conversations
 * @param responseText     the first text content from the provider response
 * @param provider         the provider that handled the request
 * @param model            the model that generated the response
 * @param providerResponse the raw provider JSON response as a map
 * @param tokensIn         input token count, or {@code null} if unavailable
 * @param tokensOut        output token count, or {@code null} if unavailable
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record VelarcResponse(
        String traceId,
        String responseText,
        String provider,
        String model,
        Map<String, Object> providerResponse,
        Integer tokensIn,
        Integer tokensOut
) {
}
