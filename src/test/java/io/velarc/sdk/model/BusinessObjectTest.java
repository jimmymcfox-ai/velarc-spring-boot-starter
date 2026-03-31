package io.velarc.sdk.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessObjectTest {

    @Test
    void ofFactoryMethod() {
        var bo = BusinessObject.of("order", "123");
        assertThat(bo.type()).isEqualTo("order");
        assertThat(bo.id()).isEqualTo("123");
    }
}
