package com.biblioteca.prestamosbiblioteca.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bibliotecaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Préstamos de Biblioteca")
                        .version("v1")
                        .description("Préstamos, atrasos, bloqueos, lista de espera, "
                                + "integración con Open Library y notificaciones por correo."))
                .components(new Components().addSecuritySchemes("cookieAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("access_token")))
                .addSecurityItem(new SecurityRequirement().addList("cookieAuth"));
    }
}
