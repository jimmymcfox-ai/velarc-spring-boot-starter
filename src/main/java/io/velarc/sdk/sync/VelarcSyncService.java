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

/**
 * Synchronises SDK configuration from the Velarc server and maintains
 * an in-memory and on-disk cache of the latest policy snapshot.
 *
 * <p>Call {@link #sync()} periodically to fetch the latest configuration.
 * Between syncs (or when the server is unreachable), lookups are served
 * from the in-memory cache. On startup, {@link #loadFromDisk()} can
 * restore the cache from a previous sync so the SDK is usable before
 * the first network call completes.
 *
 * <p>This class is safe for use by multiple threads.
 */
public class VelarcSyncService {

    private static final Logger log = LoggerFactory.getLogger(VelarcSyncService.class);

    private final String endpoint;
    private final String apiKey;
    private final Path cachePath;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private volatile SdkSyncResponse cache;

    /**
     * Creates a sync service that builds its own {@link HttpClient}.
     *
     * @param endpoint     base URL of the Velarc server (e.g. {@code https://api.velarc.io})
     * @param apiKey       Velarc API key sent as the {@code X-API-Key} header
     * @param cachePath    file path for the on-disk JSON cache
     * @param objectMapper Jackson mapper used for JSON serialisation
     */
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

    /**
     * Fetches the latest SDK configuration from the Velarc server.
     *
     * <p>Sends a {@code GET} request to {@code {endpoint}/v1/sdk/sync} with
     * the configured API key. If a previous configuration version is known,
     * an {@code If-None-Match} header is included so the server can return
     * {@code 304 Not Modified} when nothing has changed.
     *
     * <p>On a successful {@code 200} response the in-memory cache is updated
     * and the response is written to the disk cache file. A {@code 304}
     * response is a no-op.
     *
     * @throws VelarcConfigException  if the server returns {@code 401} or {@code 403}
     *                                (invalid or expired API key)
     * @throws VelarcServiceException if the server returns an unexpected status code
     *                                or the request fails due to a network error
     */
    public void sync() throws VelarcServiceException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "/v1/sdk/sync"))
                .header("X-API-Key", apiKey)
                .GET();

        SdkSyncResponse snapshot = this.cache;
        if (snapshot != null) {
            requestBuilder.header("If-None-Match", String.valueOf(snapshot.version()));
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

    /**
     * Loads the SDK configuration from the on-disk cache file.
     *
     * <p>If the file exists and can be deserialised, the in-memory cache is
     * populated and the service becomes {@linkplain #isReady() ready}.
     * I/O errors are logged but never propagated — the caller should fall
     * back to {@link #sync()} if this method returns {@code false}.
     *
     * @return {@code true} if the cache was successfully loaded, {@code false}
     *         if the file does not exist or could not be read
     */
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

    /**
     * Looks up a use-case definition by its code.
     *
     * @param code the use-case code (e.g. {@code "summarise"})
     * @return the matching definition, or empty if not found or the cache is not ready
     */
    public Optional<SdkUseCaseDefinition> getUseCaseDefinition(String code) {
        SdkSyncResponse snapshot = this.cache;
        if (snapshot == null) {
            return Optional.empty();
        }
        return snapshot.useCases().stream()
                .filter(uc -> uc.code().equals(code))
                .findFirst();
    }

    /**
     * Looks up a user definition by its key.
     *
     * @param userKey the user key (e.g. {@code "u-001"})
     * @return the matching definition, or empty if not found or the cache is not ready
     */
    public Optional<SdkUserDefinition> getUserDefinition(String userKey) {
        SdkSyncResponse snapshot = this.cache;
        if (snapshot == null) {
            return Optional.empty();
        }
        return snapshot.users().stream()
                .filter(u -> u.userKey().equals(userKey))
                .findFirst();
    }

    /**
     * Looks up a business-object type definition by its code.
     *
     * @param code the business-object type code (e.g. {@code "order"})
     * @return the matching definition, or empty if not found or the cache is not ready
     */
    public Optional<SdkBusinessObjectTypeDefinition> getBusinessObjectTypeDefinition(String code) {
        SdkSyncResponse snapshot = this.cache;
        if (snapshot == null) {
            return Optional.empty();
        }
        return snapshot.businessObjectTypes().stream()
                .filter(bot -> bot.code().equals(code))
                .findFirst();
    }

    /**
     * Returns {@code true} if at least one successful {@link #sync()} or
     * {@link #loadFromDisk()} call has populated the in-memory cache.
     *
     * @return whether the service is ready to serve lookups
     */
    public boolean isReady() {
        return this.cache != null;
    }

    /**
     * Returns the configuration version from the most recent sync, or
     * {@code -1} if no sync has completed.
     *
     * @return the current config version, or {@code -1}
     */
    public int getCurrentVersion() {
        SdkSyncResponse snapshot = this.cache;
        return snapshot != null ? snapshot.version() : -1;
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
