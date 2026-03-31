package io.velarc.sdk.model;

import java.util.List;

/**
 * Represents a conversation message with a role and content.
 *
 * @param role    the message role (e.g. "system", "user", "assistant")
 * @param content the message content — either a {@link String} or a {@link List} of {@link ContentBlock}
 */
public record Message(String role, Object content) {

    public static Message system(String text) {
        return new Message("system", text);
    }

    public static Message user(String text) {
        return new Message("user", text);
    }

    public static Message assistant(String text) {
        return new Message("assistant", text);
    }

    public static Message user(ContentBlock... blocks) {
        return new Message("user", List.of(blocks));
    }
}
