package io.velarc.sdk.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.velarc.sdk.config.VelarcProperties;
import io.velarc.sdk.exception.VelarcConfigException;
import io.velarc.sdk.exception.VelarcGovernanceException;
import io.velarc.sdk.exception.VelarcProviderException;
import io.velarc.sdk.exception.VelarcServiceException;
import io.velarc.sdk.model.BusinessObject;
import io.velarc.sdk.model.Message;
import io.velarc.sdk.model.VelarcRequest;
import io.velarc.sdk.model.VelarcResponse;
import io.velarc.sdk.sync.SdkUseCaseDefinition;
import io.velarc.sdk.sync.VelarcSyncService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The main client for interacting with the Velarc AI governance proxy.
 *
 * <p>Provides convenience methods for common chat patterns and a full
 * {@link VelarcRequest}-based method for advanced use cases. All
 * convenience methods delegate to {@link #chat(VelarcRequest)}.
 *
 * <p>The client resolves provider, model, and API key from the request
 * first, then falls back to the cached use-case definition and
 * {@link VelarcProperties}.
 */
public class VelarcClient {

    private final VelarcSyncService syncService;
    private final VelarcProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * Creates a client that builds its own {@link HttpClient}.
     *
     * @param syncService  the sync service for use-case resolution
     * @param properties   Velarc configuration properties
     * @param objectMapper Jackson mapper used for JSON serialisation
     */
    public VelarcClient(VelarcSyncService syncService, VelarcProperties properties,
                        ObjectMapper objectMapper) {
        this(syncService, properties, objectMapper, HttpClient.newHttpClient());
    }

    VelarcClient(VelarcSyncService syncService, VelarcProperties properties,
                 ObjectMapper objectMapper, HttpClient httpClient) {
        this.syncService = syncService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * Sends a single text prompt to the proxy.
     *
     * @param useCase the use-case code
     * @param user    the user key
     * @param prompt  the user prompt text
     * @return the proxy response
     * @throws VelarcServiceException  on network or server errors
     * @throws VelarcProviderException on AI provider errors
     */
    public VelarcResponse chat(String useCase, String user, String prompt)
            throws VelarcServiceException, VelarcProviderException {
        return chat(VelarcRequest.builder()
                .useCase(useCase)
                .user(user)
                .messages(List.of(Message.user(prompt)))
                .build());
    }

    /**
     * Sends a single text prompt with business object context.
     *
     * @param useCase        the use-case code
     * @param user           the user key
     * @param businessObject the business object reference
     * @param prompt         the user prompt text
     * @return the proxy response
     * @throws VelarcServiceException  on network or server errors
     * @throws VelarcProviderException on AI provider errors
     */
    public VelarcResponse chat(String useCase, String user, BusinessObject businessObject,
                               String prompt)
            throws VelarcServiceException, VelarcProviderException {
        return chat(VelarcRequest.builder()
                .useCase(useCase)
                .user(user)
                .businessObject(businessObject)
                .messages(List.of(Message.user(prompt)))
                .build());
    }

    /**
     * Sends a multi-turn conversation to the proxy.
     *
     * @param useCase  the use-case code
     * @param user     the user key
     * @param messages the conversation messages
     * @return the proxy response
     * @throws VelarcServiceException  on network or server errors
     * @throws VelarcProviderException on AI provider errors
     */
    public VelarcResponse chat(String useCase, String user, List<Message> messages)
            throws VelarcServiceException, VelarcProviderException {
        return chat(VelarcRequest.builder()
                .useCase(useCase)
                .user(user)
                .messages(messages)
                .build());
    }

    /**
     * Sends a fully configured request to the proxy.
     *
     * <p>Resolves provider, model, API key, and system prompt from the
     * request overrides, cached use-case definition, and properties —
     * in that order of precedence.
     *
     * @param request the chat request
     * @return the proxy response
     * @throws VelarcConfigException   if the SDK is not ready, or provider/model/API key
     *                                 cannot be resolved
     * @throws VelarcGovernanceException if the proxy returns a 422 governance violation
     * @throws VelarcServiceException  on network or server errors
     * @throws VelarcProviderException on AI provider errors
     */
    public VelarcResponse chat(VelarcRequest request)
            throws VelarcServiceException, VelarcProviderException {
        if (!syncService.isReady()) {
            throw new VelarcConfigException("Velarc SDK is not initialised — sync has not completed");
        }

        SdkUseCaseDefinition useCaseDef = syncService.getUseCaseDefinition(request.useCase())
                .orElse(null);

        String provider = resolve("provider", request.provider(),
                useCaseDef != null ? useCaseDef.provider() : null);
        String model = resolve("model", request.model(),
                useCaseDef != null ? useCaseDef.model() : null);
        String providerApiKey = resolveApiKey(request.providerApiKey(), provider);

        List<Message> messages = prependSystemPrompt(request.messages(), useCaseDef);

        Map<String, Object> body = buildRequestBody(request, provider, model,
                providerApiKey, messages);

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new VelarcServiceException("Failed to serialise request", e);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(properties.getEndpoint() + "/v1/proxy/chat"))
                .header("X-API-Key", properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> httpResponse;
        try {
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new VelarcServiceException("Failed to connect to Velarc server", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VelarcServiceException("Chat request interrupted", e);
        }

        return handleResponse(httpResponse);
    }

    private String resolve(String fieldName, String requestOverride, String useCaseDefault) {
        if (requestOverride != null) {
            return requestOverride;
        }
        if (useCaseDefault != null) {
            return useCaseDefault;
        }
        throw new VelarcConfigException(
                "Cannot resolve " + fieldName + " — not set on request or use case definition");
    }

    private String resolveApiKey(String requestOverride, String provider) {
        if (requestOverride != null) {
            return requestOverride;
        }
        String key = properties.getProviderApiKeys().get(provider);
        if (key != null) {
            return key;
        }
        throw new VelarcConfigException(
                "No API key configured for provider '" + provider + "'");
    }

    private List<Message> prependSystemPrompt(List<Message> messages,
                                               SdkUseCaseDefinition useCaseDef) {
        if (useCaseDef == null || useCaseDef.systemPromptText() == null) {
            return messages;
        }
        boolean hasSystem = messages.stream()
                .anyMatch(m -> "system".equals(m.role()));
        if (hasSystem) {
            return messages;
        }
        List<Message> withSystem = new ArrayList<>(messages.size() + 1);
        withSystem.add(Message.system(useCaseDef.systemPromptText()));
        withSystem.addAll(messages);
        return withSystem;
    }

    private Map<String, Object> buildRequestBody(VelarcRequest request, String provider,
                                                  String model, String providerApiKey,
                                                  List<Message> messages) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("use_case", request.useCase());
        body.put("user_key", request.user());
        body.put("messages", messages);
        body.put("provider", provider);
        body.put("model", model);
        body.put("provider_api_key", providerApiKey);

        BusinessObject bo = request.businessObject();
        if (bo != null) {
            body.put("business_object_type", bo.type());
            body.put("business_object_id", bo.id());
        }
        if (request.traceId() != null) {
            body.put("trace_id", request.traceId());
        }
        if (request.providerParams() != null) {
            body.put("provider_params", request.providerParams());
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private VelarcResponse handleResponse(HttpResponse<String> httpResponse)
            throws VelarcServiceException, VelarcProviderException {
        int status = httpResponse.statusCode();
        String responseBody = httpResponse.body();

        if (status == 200) {
            try {
                return objectMapper.readValue(responseBody, VelarcResponse.class);
            } catch (IOException e) {
                throw new VelarcServiceException("Failed to deserialise chat response", e);
            }
        }

        if (status == 401 || status == 403) {
            throw new VelarcConfigException("Invalid or expired API key (HTTP " + status + ")");
        }

        if (status == 422) {
            try {
                Map<String, Object> error = objectMapper.readValue(responseBody, Map.class);
                String errorCode = (String) error.get("error_code");
                String message = (String) error.get("message");
                throw new VelarcGovernanceException(message, errorCode, status);
            } catch (VelarcGovernanceException e) {
                throw e;
            } catch (Exception e) {
                throw new VelarcServiceException("Failed to parse governance error response", e);
            }
        }

        if (status == 502) {
            try {
                Map<String, Object> error = objectMapper.readValue(responseBody, Map.class);
                String providerName = (String) error.get("provider_name");
                String providerErrorCode = (String) error.get("provider_error_code");
                String providerMessage = (String) error.get("provider_message");
                throw new VelarcProviderException(
                        "Provider error from " + providerName, providerName,
                        providerErrorCode, providerMessage);
            } catch (VelarcProviderException e) {
                throw e;
            } catch (Exception e) {
                throw new VelarcServiceException("Failed to parse provider error response", e);
            }
        }

        throw new VelarcServiceException(
                "Unexpected response from Velarc server (HTTP " + status + ")",
                new IOException("HTTP " + status));
    }
}
