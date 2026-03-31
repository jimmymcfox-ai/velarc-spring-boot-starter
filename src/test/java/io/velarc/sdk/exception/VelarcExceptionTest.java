package io.velarc.sdk.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VelarcExceptionTest {

    @Test
    void configExceptionIsRuntimeException() {
        assertThat(new VelarcConfigException("x")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void governanceExceptionIsRuntimeException() {
        assertThat(new VelarcGovernanceException("x", "E", 422))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void serviceExceptionIsNotRuntimeException() {
        assertThat(new VelarcServiceException("x", null))
                .isNotInstanceOf(RuntimeException.class);
    }

    @Test
    void providerExceptionIsNotRuntimeException() {
        assertThat(new VelarcProviderException("x", "p", "c", "m"))
                .isNotInstanceOf(RuntimeException.class);
    }
}
