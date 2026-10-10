package com.karel.webhookinbox.inbox.api;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import com.karel.webhookinbox.AbstractWebIT;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class InboxApiIT extends AbstractWebIT {

    @Test
    void createsInboxAndReturnsLocationHeader() {
        String name = "My inbox " + UUID.randomUUID();

        ResponseEntity<JsonNode> response = createInbox(name, "desc");

        JsonNode body = response.getBody();
        assertThat(body.get("token").asText()).isNotBlank();
        assertThat(body.get("name").asText()).isEqualTo(name);
        assertThat(body.get("description").asText()).isEqualTo("desc");
        assertThat(body.get("active").asBoolean()).isTrue();
        assertThat(body.get("createdAt").asText()).isNotBlank();
        assertThat(response.getHeaders().getLocation())
                .hasToString("/api/inboxes/" + body.get("token").asText());
    }

    @Test
    void getsInboxByToken() {
        String token = createInboxToken();

        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/inboxes/" + token, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("token").asText()).isEqualTo(token);
    }

    @Test
    void listContainsCreatedInbox() {
        String token = createInboxToken();

        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/inboxes", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isArray()).isTrue();
        boolean found = false;
        for (JsonNode inbox : response.getBody()) {
            found |= token.equals(inbox.get("token").asText());
        }
        assertThat(found).as("created inbox present in list").isTrue();
    }

    @Test
    void rejectsBlankName() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/inboxes", java.util.Map.of("name", "   "), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("errors").get("name")).isNotNull();
    }

    @Test
    void deletesInbox() {
        String token = createInboxToken();

        restTemplate.delete("/api/inboxes/" + token);

        ResponseEntity<JsonNode> afterDelete = restTemplate.getForEntity("/api/inboxes/" + token, JsonNode.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returnsNotFoundForUnknownToken() {
        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/api/inboxes/does-not-exist", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("detail").asText()).contains("does-not-exist");
    }
}
