package io.velarc.sdk.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.velarc.sdk.exception.VelarcConfigException;
import io.velarc.sdk.exception.VelarcServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class VelarcSyncService {

    private static final Logger log = LoggerFactory.getLogger(VelarcSyncService.class);

    private final String endpoint;
    private final String apiKey;
    private final Path cachePath;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private volatile SdkSyncResponse cache;

    public VelarcSyncService(String endpoint, String apiKey, String cachePath, ObjectMapper objectMapper) {
        this(endpoint, apiKey, cachePath, objectMapper, HttpClient.newHttpClient());
    }

    VelarcSyncService(String endpoint, String apiKey, String cachePath,
                      ObjectMapper objectMapper, HttpClient httpClient) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.cachePath = Path.of(cachePath);
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public void sync() throws VelarcServiceException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "/v1/sdk/sync"))
                .header("X-API-Key", apiKey)
                .GET();

        if (cache != null) {
            requestBuilder.header("If-None-Match", String.valueOf(cache.version()));
        }

        HttpResponse<String> response;
        try {
            response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new VelarcServiceException("Failed to connect to Velarc server", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VelarcServiceException("Sync request interrupted", e);
        }

        int status = response.statusCode();

        if (status == 304) {
            return;
        }

        if (status == 401 || status == 403) {
            throw new VelarcConfigException("Invalid or expired API key (HTTP " + status + ")");
        }

        if (status != 200) {
            throw new VelarcServiceException("Unexpected response from Velarc server (HTTP " + status + ")",
                    new IOException("HTTP " + status));
        }

        try {
            cache = objectMapper.readValue(response.body(), SdkSyncResponse.class);
        } catch (IOException e) {
            throw new VelarcServiceException("Failed to deserialise sync response", e);
        }

        writeToDisk();
    }

    public boolean loadFromDisk() {
        if (!Files.exists(cachePath)) {
            return false;
        }
        try {
            cache = objectMapper.readValue(cachePath.toFile(), SdkSyncResponse.class);
            return true;
        } catch (IOException e) {
            log.warn("Failed to load cache from disk: {}", e.getMessage());
            return false;
        }
    }

    public Optional<SdkUseCaseDefinition> getUseCaseDefinition(String code) {
        if (cache == null) {
            return Optional.empty();
        }
        return cache.useCases().stream()
                .filter(uc -> uc.code().equals(code))
                .findFirst();
    }

    public Optional<SdkUserDefinition> getUserDefinition(String userKey) {
        if (cache == null) {
            return Optional.empty();
        }
        return cache.users().stream()
                .filter(u -> u.userKey().equals(userKey))
                .findFirst();
    }

    public Optional<SdkBusinessObjectTypeDefinition> getBusinessObjectTypeDefinition(String code) {
        if (cache == null) {
            return Optional.empty();
        }
        return cache.businessObjectTypes().stream()
                .filter(bot -> bot.code().equals(code))
                .findFirst();
    }

    public boolean isReady() {
        return cache != null;
    }

    public int getCurrentVersion() {
        return cache != null ? cache.version() : -1;
    }

    private void writeToDisk() {
        try {
            Path parent = cachePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writeValue(cachePath.toFile(), cache);
        } catch (IOException e) {
            log.warn("Failed to write cache to disk: {}", e.getMessage());
        }
    }
}
