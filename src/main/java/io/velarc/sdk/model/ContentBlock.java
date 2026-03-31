package io.velarc.sdk.model;

import java.util.Base64;
import java.util.Map;

/**
 * Represents a content block within a multi-part message.
 *
 * @param type             the block type (e.g. "text", "image")
 * @param additionalFields additional key-value pairs for this block
 */
public record ContentBlock(String type, Map<String, Object> additionalFields) {

    public static ContentBlock text(String text) {
        return new ContentBlock("text", Map.of("text", text));
    }

    public static ContentBlock image(String mediaType, byte[] data) {
        String encoded = Base64.getEncoder().encodeToString(data);
        return new ContentBlock("image", Map.of(
                "media_type", mediaType,
                "data", encoded
        ));
    }
}
