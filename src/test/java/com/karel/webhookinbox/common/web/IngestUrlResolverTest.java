package com.karel.webhookinbox.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.karel.webhookinbox.config.WebhookInboxProperties;
import com.karel.webhookinbox.config.WebhookInboxProperties.RateLimit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class IngestUrlResolverTest {

    private static WebhookInboxProperties properties(String baseUrl) {
        return new WebhookInboxProperties(1_048_576L, 24, 25, 7, new RateLimit(true, 100), baseUrl);
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void usesConfiguredBaseUrlWhenPresent() {
        IngestUrlResolver resolver = new IngestUrlResolver(properties("https://hooks.example.com"));

        assertThat(resolver.resolve("abc123")).isEqualTo("https://hooks.example.com/in/abc123");
    }

    @Test
    void stripsTrailingSlashFromConfiguredBaseUrl() {
        IngestUrlResolver resolver = new IngestUrlResolver(properties("https://hooks.example.com/"));

        assertThat(resolver.resolve("abc123")).isEqualTo("https://hooks.example.com/in/abc123");
    }

    @Test
    void derivesBaseUrlFromCurrentRequestWhenNotConfigured() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setContextPath("");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        IngestUrlResolver resolver = new IngestUrlResolver(properties(null));

        assertThat(resolver.resolve("abc123")).isEqualTo("http://localhost:8080/in/abc123");
    }
}
