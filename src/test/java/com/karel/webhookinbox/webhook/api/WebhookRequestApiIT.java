package com.karel.webhookinbox.webhook.api;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import com.karel.webhookinbox.AbstractWebIT;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class WebhookRequestApiIT extends AbstractWebIT {

    @Test
    void listsRequestsInDescendingOrder() {
        String token = createInboxToken();
        ingest(token, "/orders", "POST");
        ingest(token, "/orders/1", "GET");
        long newest = ingest(token, "/hooks", "POST");

        JsonNode page = list(token, "");

        assertThat(page.get("totalElements").asLong()).isEqualTo(3);
        assertThat(page.get("content")).hasSize(3);
        assertThat(page.get("content").get(0).get("id").asLong()).isEqualTo(newest);
        assertThat(receivedAt(page, 0)).isAfterOrEqualTo(receivedAt(page, 1));
        assertThat(receivedAt(page, 1)).isAfterOrEqualTo(receivedAt(page, 2));
    }

    @Test
    void filtersByMethod() {
        String token = createInboxToken();
        ingest(token, "/orders", "POST");
        ingest(token, "/orders/1", "GET");
        ingest(token, "/hooks", "POST");

        JsonNode page = list(token, "?method=POST");

        assertThat(page.get("totalElements").asLong()).isEqualTo(2);
        page.get("content").forEach(node -> assertThat(node.get("method").asText()).isEqualTo("POST"));
    }

    @Test
    void filtersByPathSubstring() {
        String token = createInboxToken();
        ingest(token, "/orders", "POST");
        ingest(token, "/orders/1", "GET");
        ingest(token, "/hooks", "POST");

        JsonNode page = list(token, "?q=/orders");

        assertThat(page.get("totalElements").asLong()).isEqualTo(2);
        page.get("content").forEach(node -> assertThat(node.get("path").asText()).startsWith("/orders"));
    }

    @Test
    void paginatesResults() {
        String token = createInboxToken();
        ingest(token, "/one", "POST");
        ingest(token, "/two", "POST");
        ingest(token, "/three", "POST");

        JsonNode page = list(token, "?page=0&size=2");

        assertThat(page.get("size").asInt()).isEqualTo(2);
        assertThat(page.get("totalElements").asLong()).isEqualTo(3);
        assertThat(page.get("totalPages").asInt()).isEqualTo(2);
        assertThat(page.get("first").asBoolean()).isTrue();
        assertThat(page.get("last").asBoolean()).isFalse();
        assertThat(page.get("content")).hasSize(2);
    }

    @Test
    void capsRequestedPageSize() {
        String token = createInboxToken();
        ingest(token, "/one", "POST");

        JsonNode page = list(token, "?size=1000");

        assertThat(page.get("size").asInt()).isEqualTo(100);
    }

    @Test
    void deletesSingleRequest() {
        String token = createInboxToken();
        long first = ingest(token, "/first", "POST");
        ingest(token, "/second", "POST");

        ResponseEntity<Void> delete = restTemplate.exchange(
                "/api/inboxes/" + token + "/requests/" + first, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);

        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(list(token, "").get("totalElements").asLong()).isEqualTo(1);
        ResponseEntity<JsonNode> afterDelete = restTemplate.getForEntity(
                "/api/inboxes/" + token + "/requests/" + first, JsonNode.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void clearsAllRequests() {
        String token = createInboxToken();
        ingest(token, "/one", "POST");
        ingest(token, "/two", "POST");

        ResponseEntity<Void> clear = restTemplate.exchange(
                "/api/inboxes/" + token + "/requests", HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);

        assertThat(clear.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(list(token, "").get("totalElements").asLong()).isZero();
    }

    @Test
    void returnsNotFoundForUnknownInbox() {
        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/api/inboxes/does-not-exist/requests", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returnsNotFoundForUnknownRequest() {
        String token = createInboxToken();

        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/api/inboxes/" + token + "/requests/999999", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private JsonNode list(String token, String query) {
        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/api/inboxes/" + token + "/requests" + query, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private long ingest(String token, String path, String method) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = HttpMethod.GET == HttpMethod.valueOf(method)
                ? new HttpEntity<>(headers)
                : new HttpEntity<>("{\"path\":\"" + path + "\"}", headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/in/" + token + path, HttpMethod.valueOf(method), entity, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().get("id").asLong();
    }

    private Instant receivedAt(JsonNode page, int index) {
        return Instant.parse(page.get("content").get(index).get("receivedAt").asText());
    }
}
