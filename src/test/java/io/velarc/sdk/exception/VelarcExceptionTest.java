package io.velarc.sdk.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VelarcExceptionTest {

    @Test
    void configExceptionSetsMessage() {
        var ex = new VelarcConfigException("missing API key");
        assertThat(ex.getMessage()).isEqualTo("missing API key");
    }

    @Test
    void configExceptionIsRuntimeException() {
        assertThat(new VelarcConfigException("x")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void governanceExceptionSetsFields() {
        var ex = new VelarcGovernanceException("unknown use case", "USE_CASE_NOT_FOUND", 422);
        assertThat(ex.getMessage()).isEqualTo("unknown use case");
        assertThat(ex.getErrorCode()).isEqualTo("USE_CASE_NOT_FOUND");
        assertThat(ex.getHttpStatus()).isEqualTo(422);
    }

    @Test
    void governanceExceptionIsRuntimeException() {
        assertThat(new VelarcGovernanceException("x", "E", 422))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void serviceExceptionSetsMessageAndCause() {
        var cause = new java.io.IOException("timeout");
        var ex = new VelarcServiceException("proxy unreachable", cause);
        assertThat(ex.getMessage()).isEqualTo("proxy unreachable");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void serviceExceptionIsNotRuntimeException() {
        assertThat(new VelarcServiceException("x", null))
                .isNotInstanceOf(RuntimeException.class);
    }

    @Test
    void providerExceptionSetsFields() {
        var ex = new VelarcProviderException("provider error", "openai", "rate_limit", "too many requests");
        assertThat(ex.getMessage()).isEqualTo("provider error");
        assertThat(ex.getProviderName()).isEqualTo("openai");
        assertThat(ex.getProviderErrorCode()).isEqualTo("rate_limit");
        assertThat(ex.getProviderMessage()).isEqualTo("too many requests");
    }

    @Test
    void providerExceptionIsNotRuntimeException() {
        assertThat(new VelarcProviderException("x", "p", "c", "m"))
                .isNotInstanceOf(RuntimeException.class);
    }
}
