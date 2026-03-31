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

}
