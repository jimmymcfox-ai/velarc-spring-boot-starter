package io.velarc.sdk.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTest {

    @Test
    void systemMessage() {
        var msg = Message.system("You are helpful.");
        assertThat(msg.role()).isEqualTo("system");
        assertThat(msg.content()).isEqualTo("You are helpful.");
    }

    @Test
    void userMessage() {
        var msg = Message.user("Hello");
        assertThat(msg.role()).isEqualTo("user");
        assertThat(msg.content()).isEqualTo("Hello");
    }

    @Test
    void assistantMessage() {
        var msg = Message.assistant("Hi there");
        assertThat(msg.role()).isEqualTo("assistant");
        assertThat(msg.content()).isEqualTo("Hi there");
    }

    @Test
    void userMessageWithContentBlocks() {
        var block = ContentBlock.text("hello");
        var msg = Message.user(block);
        assertThat(msg.role()).isEqualTo("user");
        assertThat(msg.content()).isEqualTo(List.of(block));
    }
}
