package io.velarc.sdk.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VelarcRequestTest {

    @Test
    void builderCreatesRequestWithAllFields() {
        BusinessObject bo = BusinessObject.of("order", "ORD-123");
        Map<String, Object> params = Map.of("temperature", 0.7);

        VelarcRequest request = VelarcRequest.builder()
                .useCase("summarise")
                .user("u-001")
                .messages(List.of(Message.user("Hello")))
                .businessObject(bo)
                .provider("openai")
                .model("gpt-4o")
                .providerApiKey("sk-test")
                .providerParams(params)
                .traceId("trace-1")
                .build();

        assertThat(request.useCase()).isEqualTo("summarise");
        assertThat(request.user()).isEqualTo("u-001");
        assertThat(request.messages()).hasSize(1);
        assertThat(request.businessObject()).isEqualTo(bo);
        assertThat(request.provider()).isEqualTo("openai");
        assertThat(request.model()).isEqualTo("gpt-4o");
        assertThat(request.providerApiKey()).isEqualTo("sk-test");
        assertThat(request.providerParams()).containsEntry("temperature", 0.7);
        assertThat(request.traceId()).isEqualTo("trace-1");
    }

    @Test
    void builderValidatesNullUseCase() {
        assertThatThrownBy(() -> VelarcRequest.builder()
                .user("u-001")
                .messages(List.of(Message.user("Hello")))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("useCase");
    }

    @Test
    void builderValidatesNullUser() {
        assertThatThrownBy(() -> VelarcRequest.builder()
                .useCase("summarise")
                .messages(List.of(Message.user("Hello")))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user");
    }

    @Test
    void builderValidatesEmptyMessages() {
        assertThatThrownBy(() -> VelarcRequest.builder()
                .useCase("summarise")
                .user("u-001")
                .messages(List.of())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messages");
    }

    @Test
    void businessObjectConvenienceMethod() {
        VelarcRequest request = VelarcRequest.builder()
                .useCase("summarise")
                .user("u-001")
                .messages(List.of(Message.user("Hello")))
                .businessObject("order", "ORD-123")
                .build();

        assertThat(request.businessObject().type()).isEqualTo("order");
        assertThat(request.businessObject().id()).isEqualTo("ORD-123");
    }
}
