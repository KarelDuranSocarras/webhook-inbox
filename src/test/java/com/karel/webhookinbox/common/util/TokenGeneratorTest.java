package com.karel.webhookinbox.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class TokenGeneratorTest {

    private final TokenGenerator generator = new TokenGenerator();

    @Test
    void generatesRequestedLength() {
        assertThat(generator.generate(24)).hasSize(24);
        assertThat(generator.generate(1)).hasSize(1);
        assertThat(generator.generate(40)).hasSize(40);
    }

    @Test
    void usesBase62CharsetOnly() {
        Pattern base62 = Pattern.compile("[A-Za-z0-9]+");
        assertThat(base62.matcher(generator.generate(512)).matches()).isTrue();
    }

    @Test
    void generatesUniqueTokens() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            tokens.add(generator.generate(24));
        }
        assertThat(tokens).hasSize(10_000);
    }
}
