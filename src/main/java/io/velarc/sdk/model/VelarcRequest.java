package io.velarc.sdk.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An immutable request to the Velarc proxy chat endpoint.
 *
 * <p>Construct instances via the {@link Builder}:
 * <pre>{@code
 * VelarcRequest request = VelarcRequest.builder()
 *         .useCase("summarise")
 *         .user("u-001")
 *         .messages(List.of(Message.user("Hello")))
 *         .build();
 * }</pre>
 */
public final class VelarcRequest {

    private final String useCase;
    private final String user;
    private final List<Message> messages;
    private final BusinessObject businessObject;
    private final String provider;
    private final String model;
    private final String providerApiKey;
    private final Map<String, Object> providerParams;
    private final String traceId;

    private VelarcRequest(Builder builder) {
        this.useCase = builder.useCase;
        this.user = builder.user;
        this.messages = Collections.unmodifiableList(builder.messages);
        this.businessObject = builder.businessObject;
        this.provider = builder.provider;
        this.model = builder.model;
        this.providerApiKey = builder.providerApiKey;
        this.providerParams = builder.providerParams != null
                ? Collections.unmodifiableMap(builder.providerParams) : null;
        this.traceId = builder.traceId;
    }

    /**
     * Creates a new request builder.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Returns the use-case code. */
    public String useCase() {
        return useCase;
    }

    /** Returns the user key. */
    public String user() {
        return user;
    }

    /** Returns the conversation messages. */
    public List<Message> messages() {
        return messages;
    }

    /** Returns the business object, or {@code null} if not set. */
    public BusinessObject businessObject() {
        return businessObject;
    }

    /** Returns the provider override, or {@code null} if not set. */
    public String provider() {
        return provider;
    }

    /** Returns the model override, or {@code null} if not set. */
    public String model() {
        return model;
    }

    /** Returns the provider API key override, or {@code null} if not set. */
    public String providerApiKey() {
        return providerApiKey;
    }

    /** Returns the provider-specific parameters, or {@code null} if not set. */
    public Map<String, Object> providerParams() {
        return providerParams;
    }

    /** Returns the trace ID for multi-turn conversations, or {@code null} if not set. */
    public String traceId() {
        return traceId;
    }

    /**
     * Builder for {@link VelarcRequest}.
     */
    public static final class Builder {

        private String useCase;
        private String user;
        private List<Message> messages;
        private BusinessObject businessObject;
        private String provider;
        private String model;
        private String providerApiKey;
        private Map<String, Object> providerParams;
        private String traceId;

        private Builder() {
        }

        /** Sets the use-case code (required). */
        public Builder useCase(String useCase) {
            this.useCase = useCase;
            return this;
        }

        /** Sets the user key (required). */
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        /** Sets the conversation messages (required, must not be empty). */
        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        /** Sets the business object context. */
        public Builder businessObject(BusinessObject businessObject) {
            this.businessObject = businessObject;
            return this;
        }

        /**
         * Sets the business object context by type and ID.
         *
         * @param type the business object type
         * @param id   the business object identifier
         * @return this builder
         */
        public Builder businessObject(String type, String id) {
            this.businessObject = BusinessObject.of(type, id);
            return this;
        }

        /** Sets the provider override. */
        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        /** Sets the model override. */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /** Sets the provider API key override. */
        public Builder providerApiKey(String providerApiKey) {
            this.providerApiKey = providerApiKey;
            return this;
        }

        /** Sets provider-specific parameters. */
        public Builder providerParams(Map<String, Object> providerParams) {
            this.providerParams = providerParams;
            return this;
        }

        /** Sets the trace ID for multi-turn conversations. */
        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        /**
         * Builds the request, validating that all required fields are set.
         *
         * @return an immutable {@link VelarcRequest}
         * @throws IllegalArgumentException if useCase, user, or messages is null or empty
         */
        public VelarcRequest build() {
            if (useCase == null || useCase.isEmpty()) {
                throw new IllegalArgumentException("useCase must not be null or empty");
            }
            if (user == null || user.isEmpty()) {
                throw new IllegalArgumentException("user must not be null or empty");
            }
            if (messages == null || messages.isEmpty()) {
                throw new IllegalArgumentException("messages must not be null or empty");
            }
            return new VelarcRequest(this);
        }
    }
}
