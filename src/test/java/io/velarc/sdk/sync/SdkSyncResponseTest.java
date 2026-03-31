package io.velarc.sdk.sync;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SdkSyncResponseTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void deserialiseFullResponse() throws Exception {
        String json = """
                {
                  "version": 3,
                  "server_timestamp": "2026-03-31T12:00:00Z",
                  "use_cases": [
                    {
                      "code": "summarise",
                      "name": "Summarise Document",
                      "risk_level": "high",
                      "requires_approval": true,
                      "provider": "openai",
                      "model": "gpt-4o",
                      "system_prompt_text": "You are a summariser."
                    }
                  ],
                  "users": [
                    {
                      "user_key": "u-001",
                      "display_name": "Alice",
                      "role": "admin"
                    }
                  ],
                  "business_object_types": [
                    {
                      "code": "order",
                      "name": "Order",
                      "id_pattern": "^ORD-\\\\d+$",
                      "id_min_length": 5,
                      "id_max_length": 20,
                      "id_description": "Order identifier"
                    }
                  ]
                }
                """;

        SdkSyncResponse response = mapper.readValue(json, SdkSyncResponse.class);

        assertThat(response.version()).isEqualTo(3);
        assertThat(response.serverTimestamp()).isEqualTo("2026-03-31T12:00:00Z");

        assertThat(response.useCases()).hasSize(1);
        SdkUseCaseDefinition uc = response.useCases().get(0);
        assertThat(uc.code()).isEqualTo("summarise");
        assertThat(uc.name()).isEqualTo("Summarise Document");
        assertThat(uc.riskLevel()).isEqualTo("high");
        assertThat(uc.requiresApproval()).isTrue();
        assertThat(uc.provider()).isEqualTo("openai");
        assertThat(uc.model()).isEqualTo("gpt-4o");
        assertThat(uc.systemPromptText()).isEqualTo("You are a summariser.");

        assertThat(response.users()).hasSize(1);
        SdkUserDefinition user = response.users().get(0);
        assertThat(user.userKey()).isEqualTo("u-001");
        assertThat(user.displayName()).isEqualTo("Alice");
        assertThat(user.role()).isEqualTo("admin");

        assertThat(response.businessObjectTypes()).hasSize(1);
        SdkBusinessObjectTypeDefinition bot = response.businessObjectTypes().get(0);
        assertThat(bot.code()).isEqualTo("order");
        assertThat(bot.name()).isEqualTo("Order");
        assertThat(bot.idPattern()).isEqualTo("^ORD-\\d+$");
        assertThat(bot.idMinLength()).isEqualTo(5);
        assertThat(bot.idMaxLength()).isEqualTo(20);
        assertThat(bot.idDescription()).isEqualTo("Order identifier");
    }

    @Test
    void deserialiseWithNullableFieldsAbsent() throws Exception {
        String json = """
                {
                  "version": 1,
                  "server_timestamp": "2026-01-01T00:00:00Z",
                  "use_cases": [
                    {
                      "code": "chat",
                      "name": "Chat",
                      "risk_level": "low",
                      "requires_approval": false
                    }
                  ],
                  "users": [],
                  "business_object_types": [
                    {
                      "code": "ticket",
                      "name": "Ticket"
                    }
                  ]
                }
                """;

        SdkSyncResponse response = mapper.readValue(json, SdkSyncResponse.class);

        SdkUseCaseDefinition uc = response.useCases().get(0);
        assertThat(uc.provider()).isNull();
        assertThat(uc.model()).isNull();
        assertThat(uc.systemPromptText()).isNull();

        SdkBusinessObjectTypeDefinition bot = response.businessObjectTypes().get(0);
        assertThat(bot.idPattern()).isNull();
        assertThat(bot.idMinLength()).isNull();
        assertThat(bot.idMaxLength()).isNull();
        assertThat(bot.idDescription()).isNull();
    }
}
