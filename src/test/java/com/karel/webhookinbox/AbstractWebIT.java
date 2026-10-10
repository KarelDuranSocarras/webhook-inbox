package com.karel.webhookinbox;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@AutoConfigureTestRestTemplate
public abstract class AbstractWebIT extends AbstractIntegrationTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    protected String createInboxToken() {
        return createInbox("it-" + UUID.randomUUID(), null).getBody().get("token").asText();
    }

    protected ResponseEntity<JsonNode> createInbox(String name, String description) {
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        if (description != null) {
            body.put("description", description);
        }
        ResponseEntity<JsonNode> response = restTemplate.postForEntity("/api/inboxes", body, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response;
    }
}
