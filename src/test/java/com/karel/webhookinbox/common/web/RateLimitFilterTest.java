package com.karel.webhookinbox.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.karel.webhookinbox.config.WebhookInboxProperties;
import com.karel.webhookinbox.config.WebhookInboxProperties.RateLimit;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    private static final WebhookInboxProperties PROPERTIES =
            new WebhookInboxProperties(1_048_576L, 24, 25, 7, new RateLimit(true, 2), null, true, "0 0 * * * *");

    private final FilterChain chain = mock(FilterChain.class);

    @Test
    void allowsRequestsUpToLimit() throws IOException, ServletException {
        RateLimitFilter filter = new RateLimitFilter(PROPERTIES);

        assertAllowed(filter);
        assertAllowed(filter);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/in/aaa"), response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        verify(chain, times(2)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void bucketsAreIndependentPerToken() throws IOException, ServletException {
        RateLimitFilter filter = new RateLimitFilter(PROPERTIES);

        assertAllowed(filter);
        assertAllowed(filter);
        assertRejected(filter);

        filter.doFilter(request("/in/bbb"), new MockHttpServletResponse(), chain);
        verify(chain, times(3)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void passesThroughWhenDisabled() throws IOException, ServletException {
        WebhookInboxProperties disabledProperties =
                new WebhookInboxProperties(1_048_576L, 24, 25, 7, new RateLimit(false, 2), null, true, "0 0 * * * *");
        RateLimitFilter filter = new RateLimitFilter(disabledProperties);

        assertAllowed(filter);
        assertAllowed(filter);
        assertAllowed(filter);
    }

    @Test
    void passesThroughWhenTokenCannotBeResolved() throws IOException, ServletException {
        RateLimitFilter filter = new RateLimitFilter(PROPERTIES);

        filter.doFilter(request("/in/"), new MockHttpServletResponse(), chain);

        verify(chain, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    private void assertAllowed(RateLimitFilter filter) throws IOException, ServletException {
        filter.doFilter(request("/in/aaa"), new MockHttpServletResponse(), chain);
    }

    private void assertRejected(RateLimitFilter filter) throws IOException, ServletException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/in/aaa"), response, chain);
        assertThat(response.getStatus()).isEqualTo(429);
    }
}