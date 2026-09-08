package com.agendamentos.equadras.config;

import com.agendamentos.equadras.dto.response.QuadraResumoResponseDTO;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                        .description("Documentação oficial das APIs REST externas da plataforma eQuadras para integrações de sistemas parceiros, bots e clientes de API.")
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

    @Bean
    public OpenApiCustomizer filterExternalApiRoutesCustomizer() {
        return openApi -> {
            if (openApi.getComponents() != null) {
                // Registra explicitamente o schema de QuadraResumoResponseDTO para exibição no Swagger
                Map<String, Schema> schemas = ModelConverters.getInstance().read(QuadraResumoResponseDTO.class);
                schemas.forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
            }

            if (openApi.getPaths() != null) {
                // Remove todas as rotas internas consultadas pelo frontend, mantendo apenas as rotas da API externa (/api/**)
                openApi.getPaths().entrySet().removeIf(entry -> !entry.getKey().startsWith("/api"));

                openApi.getPaths().forEach((path, pathItem) -> {
                    pathItem.readOperations().forEach(operation -> {
                        if (operation.getTags() != null) {
                            List<String> newTags = operation.getTags().stream().map(this::getApiTagName).distinct().toList();
                            operation.setTags(newTags);
                        }
                    });

                    // Customiza resposta do GET /api/quadras
                    if ("/api/quadras".equals(path) && pathItem.getGet() != null && pathItem.getGet().getResponses() != null) {
                        ApiResponse resp200 = pathItem.getGet().getResponses().get("200");
                        if (resp200 != null) {
                            MediaType mediaType = new MediaType().schema(
                                    new ArraySchema().items(new Schema<>().$ref("#/components/schemas/QuadraResumoResponseDTO"))
                            );
                            resp200.setDescription("Lista resumida de quadras otimizada para bots e integrações externas");
                            resp200.setContent(new Content().addMediaType("application/json", mediaType));
                        }
                    }
                });
            }

            // Define e ordena exclusivamente as tags da API externa no Swagger UI
            List<Tag> organizedTags = new ArrayList<>();
            organizedTags.add(new Tag().name("Quadras - API").description("Endpoints da API externa (/api/quadras) para consulta e gestão de quadras esportivas."));
            organizedTags.add(new Tag().name("Agendamentos - API").description("Endpoints da API externa (/api/agendamentos) para reservas, integração com Bot e horários disponíveis."));
            organizedTags.add(new Tag().name("Usuários - API").description("Endpoints da API externa (/api/usuarios) para autenticação JWT e administração de usuários."));
            organizedTags.add(new Tag().name("Pagamentos - API").description("Endpoints da API externa (/api/pagamentos) para pagamentos Pix e webhooks."));
            organizedTags.add(new Tag().name("Notificações - API").description("Endpoints da API externa (/api/notificacoes) para streaming SSE e histórico de alertas."));
            organizedTags.add(new Tag().name("Bloqueios - API").description("Endpoints da API externa (/api/quadras/bloqueios) para gestão de bloqueios de horários."));

            openApi.setTags(organizedTags);
        };
    }

    private String getApiTagName(String tag) {
        if (tag == null) return "Geral - API";
        if (tag.contains("Quadra") && !tag.contains("Bloqueio")) return "Quadras - API";
        if (tag.contains("Bloqueio")) return "Bloqueios - API";
        if (tag.contains("Agendamento") || tag.contains("Reserva")) return "Agendamentos - API";
        if (tag.contains("Usuário") || tag.contains("Autenticação")) return "Usuários - API";
        if (tag.contains("Pagamento")) return "Pagamentos - API";
        if (tag.contains("Notificação")) return "Notificações - API";
        return tag.endsWith("- API") ? tag : tag + " - API";
    }
}
