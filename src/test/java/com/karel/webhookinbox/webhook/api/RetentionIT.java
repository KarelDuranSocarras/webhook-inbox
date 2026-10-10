package com.karel.webhookinbox.webhook.api;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import com.karel.webhookinbox.AbstractWebIT;
import com.karel.webhookinbox.webhook.application.RetentionJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

class RetentionIT extends AbstractWebIT {

    @Autowired
    private RetentionJob retentionJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deletesRequestsOlderThanRetentionWindowWhenJobRuns() {
        String token = createInboxToken();
        long oldId = ingest(token);
        long recentId = ingest(token);

        jdbcTemplate.update("update webhook_request set received_at = now() - interval '10 days' where id = ?", oldId);

        retentionJob.run();

        assertThat(getStatus(token, oldId)).isEqualTo(404);
        assertThat(getStatus(token, recentId)).isEqualTo(200);
    }

    private long ingest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        ResponseEntity<JsonNode> response =
                restTemplate.postForEntity("/in/" + token + "/captured", new HttpEntity<>("x", headers), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().get("id").asLong();
    }

    private int getStatus(String token, long id) {
        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/api/inboxes/" + token + "/requests/" + id, JsonNode.class);
        return response.getStatusCode().value();
    }
}