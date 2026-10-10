package com.karel.webhookinbox.webhook.api;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import com.karel.webhookinbox.AbstractWebIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "webhook-inbox.rate-limit.enabled=true",
        "webhook-inbox.rate-limit.requests-per-minute=5"
})
class RateLimitIT extends AbstractWebIT {

    @Test
    void rejectsRequestsBeyondTheLimitForTheSameToken() {
        String token = createInboxToken();

        for (int i = 0; i < 5; i++) {
            ResponseEntity<JsonNode> response = post(token);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        for (int i = 0; i < 5; i++) {
            ResponseEntity<JsonNode> response = post(token);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("60");
        }
    }

    @Test
    void limitIsIndependentPerToken() {
        String token = createInboxToken();
        for (int i = 0; i < 5; i++) {
            assertThat(post(token).getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        assertThat(post(token).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        String otherToken = createInboxToken();
        assertThat(post(otherToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<JsonNode> post(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return restTemplate.postForEntity(
                "/in/" + token + "/hook",
                new HttpEntity<>("x", headers),
                JsonNode.class);
    }
}