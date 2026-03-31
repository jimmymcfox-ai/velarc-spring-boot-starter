package io.velarc.sdk.model;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ContentBlockTest {

    @Test
    void textBlock() {
        var block = ContentBlock.text("hello");
        assertThat(block.type()).isEqualTo("text");
        assertThat(block.additionalFields()).containsEntry("text", "hello");
    }

    @Test
    void imageBlock() {
        byte[] data = {1, 2, 3};
        var block = ContentBlock.image("image/png", data);
        assertThat(block.type()).isEqualTo("image");
        assertThat(block.additionalFields()).containsEntry("media_type", "image/png");
        assertThat(block.additionalFields().get("data"))
                .isEqualTo(Base64.getEncoder().encodeToString(data));
    }
}
