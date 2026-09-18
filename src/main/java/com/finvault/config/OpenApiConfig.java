package com.finvault.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI finVaultOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinVault API")
                        .description("Digital wallet and payment platform - Spring Boot fintech stack")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("FinVault Team")
                                .email("dev@finvault.io"))
                        .license(new License().name("MIT")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token from /api/v1/auth/login")));
    }
}
