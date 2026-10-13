package edu.unibague.users.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI userOpenApi() {
        String scheme = "bearerAuth";
        return new OpenAPI()
            .info(new Info()
                .title("Taller 02 v2.0 - User Service")
                .version("2.0.0")
                .description("CRUD de perfiles protegido con JWT."))
            .addSecurityItem(new SecurityRequirement().addList(scheme))
            .components(new Components().addSecuritySchemes(scheme,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
