package io.routalyx.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI routalyxOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Routalyx API")
                        .description(
                                "REST API for fleet management and analytics."
                        )
                        .version("v1"));
    }
}
