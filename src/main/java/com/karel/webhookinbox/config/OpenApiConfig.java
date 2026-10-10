package com.karel.webhookinbox.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI webhookInboxOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Webhook Inbox API")
                        .version("0.1.0-SNAPSHOT")
                        .description("""
                                RequestBin-style webhook inspector.
                                Create an inbox, point webhooks at its ingest URL, and inspect the captured requests.
                                """)
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(new Server()
                        .url("http://localhost:8080")
                        .description("Local development")));
    }
}
