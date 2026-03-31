package io.velarc.sdk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for the Velarc SDK, bound from the {@code velarc.*} namespace.
 */
@ConfigurationProperties(prefix = "velarc")
public class VelarcProperties {

    private String endpoint;
    private String apiKey;
    private Map<String, String> providerApiKeys = new HashMap<>();
    private final Startup startup = new Startup();
    private final Cache cache = new Cache();
    private final Sync sync = new Sync();

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Map<String, String> getProviderApiKeys() {
        return providerApiKeys;
    }

    public void setProviderApiKeys(Map<String, String> providerApiKeys) {
        this.providerApiKeys = providerApiKeys;
    }

    public Startup getStartup() {
        return startup;
    }

    public Cache getCache() {
        return cache;
    }

    public Sync getSync() {
        return sync;
    }

    public static class Startup {

        private boolean requireSync = true;
        private Duration syncTimeout = Duration.ofSeconds(30);
        private Duration syncRetryInterval = Duration.ofSeconds(5);

        public boolean isRequireSync() {
            return requireSync;
        }

        public void setRequireSync(boolean requireSync) {
            this.requireSync = requireSync;
        }

        public Duration getSyncTimeout() {
            return syncTimeout;
        }

        public void setSyncTimeout(Duration syncTimeout) {
            this.syncTimeout = syncTimeout;
        }

        public Duration getSyncRetryInterval() {
            return syncRetryInterval;
        }

        public void setSyncRetryInterval(Duration syncRetryInterval) {
            this.syncRetryInterval = syncRetryInterval;
        }
    }

    public static class Cache {

        private String path = "./velarc-cache.json";

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class Sync {

        private Duration interval = Duration.ofSeconds(60);

        public Duration getInterval() {
            return interval;
        }

        public void setInterval(Duration interval) {
            this.interval = interval;
        }
    }
}
