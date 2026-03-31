package io.velarc.sdk.config;

import io.velarc.sdk.client.VelarcClient;
import io.velarc.sdk.health.VelarcHealthIndicator;
import io.velarc.sdk.sync.VelarcSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class VelarcAutoConfigurationIntegrationTest {

    @SpringBootApplication
    static class TestApp {
    }

    @Autowired
    private VelarcClient velarcClient;

    @Autowired
    private VelarcSyncService velarcSyncService;

    @Autowired
    private VelarcHealthIndicator velarcHealthIndicator;

    @Autowired
    private VelarcProperties velarcProperties;

    @Test
    void velarcClientBeanIsCreated() {
        assertThat(velarcClient).isNotNull();
    }

    @Test
    void velarcSyncServiceBeanIsCreated() {
        assertThat(velarcSyncService).isNotNull();
    }

    @Test
    void velarcHealthIndicatorBeanIsCreated() {
        assertThat(velarcHealthIndicator).isNotNull();
    }

    @Test
    void propertiesAreBoundCorrectly() {
        assertThat(velarcProperties.getEndpoint()).isEqualTo("https://test.velarc.io");
        assertThat(velarcProperties.getApiKey()).isEqualTo("test-api-key");
        assertThat(velarcProperties.getStartup().isRequireSync()).isFalse();
    }
}
