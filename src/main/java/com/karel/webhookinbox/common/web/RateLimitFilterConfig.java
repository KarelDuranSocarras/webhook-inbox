package com.karel.webhookinbox.common.web;

import com.karel.webhookinbox.config.WebhookInboxProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class RateLimitFilterConfig {

    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilter(WebhookInboxProperties properties) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(properties));
        registration.setName("rateLimitFilter");
        registration.addUrlPatterns("/in/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}