package ayd2.p2b.conference_service_api.core.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConferenceOpenApiConfig {

    @Bean
    public OpenAPI conferenceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Conference Service API")
                        .description("Conference, institutions, activities, registrations, attendance and certificates API for the AyD2 P2B microservices ecosystem.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(
                                "bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }
}
