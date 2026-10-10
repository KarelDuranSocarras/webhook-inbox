package com.karel.webhookinbox.webhook.api;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import com.karel.webhookinbox.AbstractWebIT;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

class IngestApiIT extends AbstractWebIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void capturesWebhookAndExposesItThroughApi() {
        String token = createInboxToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Signature", "abc");
        headers.add("X-Signature", "def");

        ResponseEntity<JsonNode> ingest = restTemplate.postForEntity(
                "/in/" + token + "/orders/detail?status=new",
                new HttpEntity<>("{\"orderId\":123}", headers),
                JsonNode.class);

        assertThat(ingest.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ingest.getBody().get("receivedAt").asText()).isNotBlank();
        long id = ingest.getBody().get("id").asLong();

        JsonNode detail = getDetail(token, id);
        assertThat(detail.get("method").asText()).isEqualTo("POST");
        assertThat(detail.get("path").asText()).isEqualTo("/orders/detail");
        assertThat(detail.get("queryString").asText()).isEqualTo("status=new");
        assertThat(detail.get("contentType").asText()).startsWith("application/json");
        assertThat(detail.get("body").asText()).isEqualTo("{\"orderId\":123}");
        assertThat(detail.get("sourceIp").asText()).isNotBlank();
        assertThat(detail.get("headers").get("X-Signature")).hasSize(2);
    }

    @Test
    void capturesRootPath() {
        String token = createInboxToken();

        ResponseEntity<JsonNode> ingest = restTemplate.postForEntity(
                "/in/" + token, new HttpEntity<>("ping", headers(MediaType.TEXT_PLAIN)), JsonNode.class);

        JsonNode detail = getDetail(token, ingest.getBody().get("id").asLong());
        assertThat(detail.get("path").asText()).isEqualTo("/");
    }

    @Test
    void capturesSubPath() {
        String token = createInboxToken();

        ResponseEntity<JsonNode> ingest = restTemplate.postForEntity(
                "/in/" + token + "/a/b/c", new HttpEntity<>("x", headers(MediaType.TEXT_PLAIN)), JsonNode.class);

        JsonNode detail = getDetail(token, ingest.getBody().get("id").asLong());
        assertThat(detail.get("path").asText()).isEqualTo("/a/b/c");
    }

    @Test
    void preservesUrlEncodingInPath() {
        String token = createInboxToken();

        ResponseEntity<JsonNode> ingest = restTemplate.postForEntity(
                URI.create("/in/" + token + "/orders%20detail"),
                new HttpEntity<>("x", headers(MediaType.TEXT_PLAIN)),
                JsonNode.class);

        assertThat(ingest.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode detail = getDetail(token, ingest.getBody().get("id").asLong());
        assertThat(detail.get("path").asText()).isEqualTo("/orders%20detail");
    }

    @Test
    void capturesEmptyBody() {
        String token = createInboxToken();

        ResponseEntity<JsonNode> ingest = restTemplate.postForEntity(
                "/in/" + token + "/empty",
                new HttpEntity<>(null, headers(MediaType.TEXT_PLAIN)),
                JsonNode.class);

        JsonNode detail = getDetail(token, ingest.getBody().get("id").asLong());
        assertThat(detail.get("bodySize").asInt()).isZero();
        assertThat(detail.get("body").asText()).isEmpty();
    }

    @Test
    void rejectsUnknownToken() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/in/unknown-token",
                new HttpEntity<>("x", headers(MediaType.TEXT_PLAIN)),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsInactiveToken() {
        String token = createInboxToken();
        jdbcTemplate.update("update inbox set active = false where token = ?", token);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/in/" + token,
                new HttpEntity<>("x", headers(MediaType.TEXT_PLAIN)),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsBodyLargerThanLimit() {
        String token = createInboxToken();
        byte[] payload = new byte[1_048_577];
        Arrays.fill(payload, (byte) 'a');

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/in/" + token + "/big",
                new HttpEntity<>(new String(payload, StandardCharsets.UTF_8), headers(MediaType.TEXT_PLAIN)),
                JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(413);
    }

    private JsonNode getDetail(String token, long id) {
        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/api/inboxes/" + token + "/requests/" + id, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private HttpHeaders headers(MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        if (contentType != null) {
            headers.setContentType(contentType);
        }
        return headers;
    }
}
