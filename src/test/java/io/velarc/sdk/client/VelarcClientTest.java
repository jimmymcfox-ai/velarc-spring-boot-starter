package io.velarc.sdk.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class VelarcClientTest {

    private static final String CHAT_RESPONSE_JSON = """
            {
              "trace_id": "t-100",
              "response_text": "Hello there!",
              "provider": "openai",
              "model": "gpt-4o",
              "provider_response": {"id": "abc"},
              "tokens_in": 10,
              "tokens_out": 5
            }
            """;

    private ObjectMapper objectMapper;
    private HttpClient httpClient;
    private VelarcSyncService syncService;
    private VelarcProperties properties;
    private VelarcClient client;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        httpClient = mock(HttpClient.class);
        syncService = mock(VelarcSyncService.class);
        properties = new VelarcProperties();
        properties.setEndpoint("https://api.velarc.io");
        properties.setApiKey("velarc-key");
        properties.getProviderApiKeys().put("openai", "sk-openai");

        when(syncService.isReady()).thenReturn(true);
        when(syncService.getUseCaseDefinition("summarise")).thenReturn(Optional.of(
                new SdkUseCaseDefinition("summarise", "Summarise", "high",
                        true, "openai", "gpt-4o", "You are a summariser.")));

        client = new VelarcClient(syncService, properties, objectMapper, httpClient);
    }

    @Test
    void successfulChatWithSinglePrompt() throws Exception {
        mockResponse(200, CHAT_RESPONSE_JSON);

        VelarcResponse response = client.chat("summarise", "u-001", "Summarise this");

        assertThat(response.traceId()).isEqualTo("t-100");
        assertThat(response.responseText()).isEqualTo("Hello there!");
        assertThat(response.provider()).isEqualTo("openai");
        assertThat(response.model()).isEqualTo("gpt-4o");
        assertThat(response.tokensIn()).isEqualTo(10);
        assertThat(response.tokensOut()).isEqualTo(5);

        Map<String, Object> body = captureRequestBody();
        assertThat(body.get("use_case_code")).isEqualTo("summarise");
        assertThat(body.get("user_key")).isEqualTo("u-001");
        assertThat(body.get("provider")).isEqualTo("openai");
        assertThat(body.get("model")).isEqualTo("gpt-4o");
        assertThat(body.get("provider_api_key")).isEqualTo("sk-openai");
    }

    @Test
    void successfulChatWithBusinessObject() throws Exception {
        mockResponse(200, CHAT_RESPONSE_JSON);

        client.chat("summarise", "u-001", BusinessObject.of("order", "ORD-1"), "Summarise");

        Map<String, Object> body = captureRequestBody();
        assertThat(body.get("business_object_type")).isEqualTo("order");
        assertThat(body.get("business_object_id")).isEqualTo("ORD-1");
    }

    @Test
    void successfulChatWithMessageArray() throws Exception {
        mockResponse(200, CHAT_RESPONSE_JSON);

        List<Message> messages = List.of(Message.user("Hello"), Message.assistant("Hi"));
        client.chat("summarise", "u-001", messages);

        Map<String, Object> body = captureRequestBody();
        List<?> msgList = (List<?>) body.get("messages");
        // system prompt prepended + 2 original messages
        assertThat(msgList).hasSize(3);
    }

    @Test
    void providerAndModelResolvedFromUseCaseDefinition() throws Exception {
        mockResponse(200, CHAT_RESPONSE_JSON);

        VelarcRequest request = VelarcRequest.builder()
                .useCase("summarise")
                .user("u-001")
                .messages(List.of(Message.user("Hello")))
                .build();

        client.chat(request);

        Map<String, Object> body = captureRequestBody();
        assertThat(body.get("provider")).isEqualTo("openai");
        assertThat(body.get("model")).isEqualTo("gpt-4o");
    }

    @Test
    void providerApiKeyResolvedFromProperties() throws Exception {
        mockResponse(200, CHAT_RESPONSE_JSON);

        client.chat("summarise", "u-001", "Hello");

        Map<String, Object> body = captureRequestBody();
        assertThat(body.get("provider_api_key")).isEqualTo("sk-openai");
    }

    @Test
    void systemPromptPrependedFromUseCaseDefinition() throws Exception {
        mockResponse(200, CHAT_RESPONSE_JSON);

        client.chat("summarise", "u-001", "Hello");

        Map<String, Object> body = captureRequestBody();
        List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
        assertThat(messages.get(0).get("role")).isEqualTo("system");
        assertThat(messages.get(0).get("content")).isEqualTo("You are a summariser.");
    }

    @Test
    void systemPromptNotPrependedWhenAlreadyPresent() throws Exception {
        mockResponse(200, CHAT_RESPONSE_JSON);

        List<Message> messages = List.of(
                Message.system("Custom system prompt"),
                Message.user("Hello"));

        client.chat("summarise", "u-001", messages);

        Map<String, Object> body = captureRequestBody();
        List<Map<String, Object>> msgList = (List<Map<String, Object>>) body.get("messages");
        assertThat(msgList).hasSize(2);
        assertThat(msgList.get(0).get("content")).isEqualTo("Custom system prompt");
    }

    @Test
    void response422ThrowsGovernanceException() throws Exception {
        mockResponse(422, """
                {"error_code": "UNKNOWN_USE_CASE", "message": "Use case not found"}
                """);

        assertThatThrownBy(() -> client.chat("summarise", "u-001", "Hello"))
                .isInstanceOf(VelarcGovernanceException.class)
                .satisfies(ex -> {
                    VelarcGovernanceException ge = (VelarcGovernanceException) ex;
                    assertThat(ge.getErrorCode()).isEqualTo("UNKNOWN_USE_CASE");
                    assertThat(ge.getHttpStatus()).isEqualTo(422);
                });
    }

    @Test
    void response502ThrowsProviderException() throws Exception {
        mockResponse(502, """
                {
                  "provider_name": "openai",
                  "provider_error_code": "rate_limit",
                  "provider_message": "Rate limit exceeded"
                }
                """);

        assertThatThrownBy(() -> client.chat("summarise", "u-001", "Hello"))
                .isInstanceOf(VelarcProviderException.class)
                .satisfies(ex -> {
                    VelarcProviderException pe = (VelarcProviderException) ex;
                    assertThat(pe.getProviderName()).isEqualTo("openai");
                    assertThat(pe.getProviderErrorCode()).isEqualTo("rate_limit");
                });
    }

    @Test
    void response401ThrowsConfigException() throws Exception {
        mockResponse(401, "");

        assertThatThrownBy(() -> client.chat("summarise", "u-001", "Hello"))
                .isInstanceOf(VelarcConfigException.class);
    }

    @Test
    void response500ThrowsServiceException() throws Exception {
        mockResponse(500, "Internal Server Error");

        assertThatThrownBy(() -> client.chat("summarise", "u-001", "Hello"))
                .isInstanceOf(VelarcServiceException.class);
    }

    @Test
    void connectionFailureThrowsServiceException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection refused"));

        assertThatThrownBy(() -> client.chat("summarise", "u-001", "Hello"))
                .isInstanceOf(VelarcServiceException.class);
    }

    @Test
    void chatWhenNotReadyThrowsConfigException() {
        when(syncService.isReady()).thenReturn(false);

        assertThatThrownBy(() -> client.chat("summarise", "u-001", "Hello"))
                .isInstanceOf(VelarcConfigException.class)
                .hasMessageContaining("not initialised");
    }

    @Test
    void providerParamsIncludedInRequest() throws Exception {
        mockResponse(200, CHAT_RESPONSE_JSON);

        VelarcRequest request = VelarcRequest.builder()
                .useCase("summarise")
                .user("u-001")
                .messages(List.of(Message.user("Hello")))
                .providerParams(Map.of("temperature", 0.5))
                .build();

        client.chat(request);

        Map<String, Object> body = captureRequestBody();
        assertThat((Map<String, Object>) body.get("provider_params"))
                .containsEntry("temperature", 0.5);
    }

    @Test
    void traceIdIncludedInRequest() throws Exception {
        mockResponse(200, CHAT_RESPONSE_JSON);

        VelarcRequest request = VelarcRequest.builder()
                .useCase("summarise")
                .user("u-001")
                .messages(List.of(Message.user("Hello")))
                .traceId("trace-42")
                .build();

        client.chat(request);

        Map<String, Object> body = captureRequestBody();
        assertThat(body.get("trace_id")).isEqualTo("trace-42");
    }

    private void mockResponse(int status, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);
    }

    private Map<String, Object> captureRequestBody() throws Exception {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        org.mockito.Mockito.verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));

        HttpRequest captured = captor.getValue();
        var bodyPublisher = captured.bodyPublisher().orElseThrow();
        var future = new java.util.concurrent.CompletableFuture<byte[]>();
        bodyPublisher.subscribe(new java.util.concurrent.Flow.Subscriber<>() {
            private final java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

            @Override
            public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(java.nio.ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                baos.write(bytes, 0, bytes.length);
            }

            @Override
            public void onError(Throwable throwable) {
                future.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                future.complete(baos.toByteArray());
            }
        });
        String bodyString = new String(future.get(), java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readValue(bodyString, Map.class);
    }
}
