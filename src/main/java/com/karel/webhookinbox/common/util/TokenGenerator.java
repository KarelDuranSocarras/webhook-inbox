package com.karel.webhookinbox.common.util;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class TokenGenerator {

    private static final char[] CHARSET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Token length must be positive");
        }
        char[] token = new char[length];
        for (int i = 0; i < length; i++) {
            token[i] = CHARSET[RANDOM.nextInt(CHARSET.length)];
        }
        return new String(token);
    }
}
