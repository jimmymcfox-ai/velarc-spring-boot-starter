package io.velarc.sdk.config;

import io.velarc.sdk.exception.VelarcConfigException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VelarcAutoConfigurationValidationTest {

    @Test
    void missingEndpointThrowsConfigException() {
        VelarcProperties props = new VelarcProperties();
        props.setApiKey("some-key");

        VelarcAutoConfiguration config = new VelarcAutoConfiguration(props);

        assertThatThrownBy(config::validate)
                .isInstanceOf(VelarcConfigException.class)
                .hasMessageContaining("velarc.endpoint");
    }

    @Test
    void missingApiKeyThrowsConfigException() {
        VelarcProperties props = new VelarcProperties();
        props.setEndpoint("https://api.velarc.io");

        VelarcAutoConfiguration config = new VelarcAutoConfiguration(props);

        assertThatThrownBy(config::validate)
                .isInstanceOf(VelarcConfigException.class)
                .hasMessageContaining("velarc.api-key");
    }
}
