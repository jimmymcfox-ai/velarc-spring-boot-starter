package io.velarc.sdk.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class VelarcPropertiesTest {

    @Test
    void defaultRequireSyncIsTrue() {
        var props = new VelarcProperties();
        assertThat(props.getStartup().isRequireSync()).isTrue();
    }

    @Test
    void defaultSyncTimeoutIs30Seconds() {
        var props = new VelarcProperties();
        assertThat(props.getStartup().getSyncTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void defaultSyncRetryIntervalIs5Seconds() {
        var props = new VelarcProperties();
        assertThat(props.getStartup().getSyncRetryInterval()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void defaultCachePathIsSet() {
        var props = new VelarcProperties();
        assertThat(props.getCache().getPath()).isEqualTo("./velarc-cache.json");
    }

    @Test
    void defaultSyncIntervalIs60Seconds() {
        var props = new VelarcProperties();
        assertThat(props.getSync().getInterval()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void defaultProviderApiKeysIsEmpty() {
        var props = new VelarcProperties();
        assertThat(props.getProviderApiKeys()).isEmpty();
    }

    @Test
    void endpointAndApiKeyAreNullByDefault() {
        var props = new VelarcProperties();
        assertThat(props.getEndpoint()).isNull();
        assertThat(props.getApiKey()).isNull();
    }

    @Test
    void settersUpdateValues() {
        var props = new VelarcProperties();
        props.setEndpoint("https://api.velarc.io");
        props.setApiKey("key-123");
        props.getStartup().setRequireSync(false);
        props.getStartup().setSyncTimeout(Duration.ofSeconds(10));
        props.getCache().setPath("/tmp/cache.json");
        props.getSync().setInterval(Duration.ofSeconds(120));

        assertThat(props.getEndpoint()).isEqualTo("https://api.velarc.io");
        assertThat(props.getApiKey()).isEqualTo("key-123");
        assertThat(props.getStartup().isRequireSync()).isFalse();
        assertThat(props.getStartup().getSyncTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(props.getCache().getPath()).isEqualTo("/tmp/cache.json");
        assertThat(props.getSync().getInterval()).isEqualTo(Duration.ofSeconds(120));
    }
}
