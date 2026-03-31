package io.velarc.sdk.sync;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.velarc.sdk.exception.VelarcConfigException;
import io.velarc.sdk.exception.VelarcServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class VelarcSyncServiceTest {

    private static final String SYNC_RESPONSE_JSON = """
            {
              "version": 5,
              "server_timestamp": "2026-03-31T12:00:00Z",
              "use_cases": [
                {
                  "code": "summarise",
                  "name": "Summarise Document",
                  "risk_level": "high",
                  "requires_approval": true,
                  "provider": "openai",
                  "model": "gpt-4o",
                  "system_prompt_text": "You are a summariser."
                }
              ],
              "users": [
                {
                  "user_key": "u-001",
                  "display_name": "Alice",
                  "role": "admin"
                }
              ],
              "business_object_types": [
                {
                  "code": "order",
                  "name": "Order",
                  "id_pattern": "^ORD-\\\\d+$",
                  "id_min_length": 5,
                  "id_max_length": 20,
                  "id_description": "Order identifier"
                }
              ]
            }
            """;

    @TempDir
    Path tempDir;

    private ObjectMapper objectMapper;
    private HttpClient httpClient;
    private VelarcSyncService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        httpClient = mock(HttpClient.class);
        String cachePath = tempDir.resolve("velarc-cache.json").toString();
        service = new VelarcSyncService("https://api.velarc.io", "test-key", cachePath,
                objectMapper, httpClient);
    }

    @Test
    void syncPopulatesCacheAndWritesToDisk() throws Exception {
        mockResponse(200, SYNC_RESPONSE_JSON);

        service.sync();

        assertThat(service.isReady()).isTrue();
        assertThat(service.getCurrentVersion()).isEqualTo(5);
        assertThat(Files.exists(tempDir.resolve("velarc-cache.json"))).isTrue();
    }

    @Test
    void sync304DoesNotChangeCache() throws Exception {
        mockResponse(200, SYNC_RESPONSE_JSON);
        service.sync();

        mockResponse(304, "");
        service.sync();

        assertThat(service.getCurrentVersion()).isEqualTo(5);
    }

    @Test
    void sync304DoesNotWriteToDisk() throws Exception {
        mockResponse(304, "");

        service.sync();

        assertThat(service.isReady()).isFalse();
        assertThat(Files.exists(tempDir.resolve("velarc-cache.json"))).isFalse();
    }

    @Test
    void sync401ThrowsVelarcConfigException() throws Exception {
        mockResponse(401, "");

        assertThatThrownBy(() -> service.sync())
                .isInstanceOf(VelarcConfigException.class);
    }

    @Test
    void sync403ThrowsVelarcConfigException() throws Exception {
        mockResponse(403, "");

        assertThatThrownBy(() -> service.sync())
                .isInstanceOf(VelarcConfigException.class);
    }

    @Test
    void sync500ThrowsVelarcServiceException() throws Exception {
        mockResponse(500, "Internal Server Error");

        assertThatThrownBy(() -> service.sync())
                .isInstanceOf(VelarcServiceException.class);
    }

    @Test
    void syncConnectionFailureThrowsVelarcServiceException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection refused"));

        assertThatThrownBy(() -> service.sync())
                .isInstanceOf(VelarcServiceException.class);
    }

    @Test
    void loadFromDiskPopulatesCache() throws Exception {
        Path cacheFile = tempDir.resolve("velarc-cache.json");
        Files.writeString(cacheFile, SYNC_RESPONSE_JSON);

        assertThat(service.loadFromDisk()).isTrue();
        assertThat(service.isReady()).isTrue();
        assertThat(service.getCurrentVersion()).isEqualTo(5);
    }

    @Test
    void loadFromDiskReturnsFalseWhenFileDoesNotExist() {
        assertThat(service.loadFromDisk()).isFalse();
        assertThat(service.isReady()).isFalse();
    }

    @Test
    void lookupReturnsEmptyForUnknownCode() throws Exception {
        mockResponse(200, SYNC_RESPONSE_JSON);
        service.sync();

        assertThat(service.getUseCaseDefinition("nonexistent")).isEmpty();
        assertThat(service.getUserDefinition("nonexistent")).isEmpty();
        assertThat(service.getBusinessObjectTypeDefinition("nonexistent")).isEmpty();
    }

    @Test
    void lookupReturnsCorrectDefinitions() throws Exception {
        mockResponse(200, SYNC_RESPONSE_JSON);
        service.sync();

        assertThat(service.getUseCaseDefinition("summarise"))
                .isPresent()
                .get().extracting(SdkUseCaseDefinition::code).isEqualTo("summarise");

        assertThat(service.getUserDefinition("u-001"))
                .isPresent()
                .get().extracting(SdkUserDefinition::userKey).isEqualTo("u-001");

        assertThat(service.getBusinessObjectTypeDefinition("order"))
                .isPresent()
                .get().extracting(SdkBusinessObjectTypeDefinition::code).isEqualTo("order");
    }

    @Test
    void lookupsReturnEmptyWhenNotReady() {
        assertThat(service.getUseCaseDefinition("summarise")).isEmpty();
        assertThat(service.getUserDefinition("u-001")).isEmpty();
        assertThat(service.getBusinessObjectTypeDefinition("order")).isEmpty();
    }

    @Test
    void getCurrentVersionReturnsNegativeOneBeforeSync() {
        assertThat(service.getCurrentVersion()).isEqualTo(-1);
    }

    private void mockResponse(int status, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);
    }
}
