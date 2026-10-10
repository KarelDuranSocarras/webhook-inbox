package com.karel.webhookinbox.ui;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import com.karel.webhookinbox.AbstractWebIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class UiSmokeIT extends AbstractWebIT {

    @Test
    void rendersInboxAndEscapesRequestBodies() {
        String token = createInboxToken();
        String malicious = "<script>alert(1)</script>";
        long maliciousId = ingest(token, "/evil", malicious, MediaType.TEXT_PLAIN);
        ingest(token, "/plain", "{\"x\":\"y\"}", MediaType.APPLICATION_JSON);

        String inboxHtml = restTemplate.getForObject("/inboxes/" + token, String.class);
        assertThat(inboxHtml).contains("/evil").contains("/plain").contains("POST");

        String detailHtml = restTemplate.getForObject("/inboxes/" + token + "/requests/" + maliciousId, String.class);
        assertThat(detailHtml).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
        assertThat(detailHtml).doesNotContain(malicious);
    }

    private long ingest(String token, String path, String body, MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/in/" + token + path, new HttpEntity<>(body, headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().get("id").asLong();
    }
}
