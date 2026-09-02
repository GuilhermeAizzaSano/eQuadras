package com.agendamentos.equadras.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        io.swagger.v3.oas.models.media.Schema<String> timeSchema = new io.swagger.v3.oas.models.media.StringSchema()
                .example("14:00:00")
                .description("Horário no formato HH:mm:ss");

        return new OpenAPI()
                .info(new Info()
                        .title("eQuadras API - Gestão e Agendamento Esportivo")
                        .description("Documentação oficial das APIs REST da plataforma eQuadras para integração de sistemas externos, parceiros e aplicativos clientes.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Suporte Técnico eQuadras")
                                .email("contato@equadras.com.br")
                                .url("https://github.com/GuilhermeAizzaSano/eQuadras"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSchemas("LocalTime", timeSchema)
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Informe o token JWT no formato: `Bearer <seu_token>` gerado no endpoint de login.")
                        ));
    }
}
