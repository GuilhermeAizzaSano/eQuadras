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

    @Bean
    public OpenApiCustomizer separateApiAndFrontendTagsCustomizer() {
        return openApi -> {
            if (openApi.getComponents() != null) {
                // Registra explicitamente o schema de QuadraResumoResponseDTO para exibição no Swagger
                Map<String, Schema> schemas = ModelConverters.getInstance().read(QuadraResumoResponseDTO.class);
                schemas.forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
            }

            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((path, pathItem) -> {
                    boolean isApi = path.startsWith("/api/") || path.equals("/api");

                    pathItem.readOperations().forEach(operation -> {
                        if (operation.getTags() != null) {
                            List<String> newTags = operation.getTags().stream().map(tag -> {
                                String base = getBaseTagName(tag);
                                return isApi ? getApiTagName(base) : base;
                            }).distinct().toList();
                            operation.setTags(newTags);
                        }
                    });

                    // Customiza resposta do GET /quadras vs GET /api/quadras
                    if ("/quadras".equals(path) && pathItem.getGet() != null && pathItem.getGet().getResponses() != null) {
                        ApiResponse resp200 = pathItem.getGet().getResponses().get("200");
                        if (resp200 != null) {
                            MediaType mediaType = new MediaType().schema(
                                    new ArraySchema().items(new Schema<>().$ref("#/components/schemas/QuadraResponseDTO"))
                            );
                            resp200.setDescription("Lista completa de quadras com fotos e disponibilidades (Formato Frontend)");
                            resp200.setContent(new Content().addMediaType("application/json", mediaType));
                        }
                    }

                    if ("/api/quadras".equals(path) && pathItem.getGet() != null && pathItem.getGet().getResponses() != null) {
                        ApiResponse resp200 = pathItem.getGet().getResponses().get("200");
                        if (resp200 != null) {
                            MediaType mediaType = new MediaType().schema(
                                    new ArraySchema().items(new Schema<>().$ref("#/components/schemas/QuadraResumoResponseDTO"))
                            );
                            resp200.setDescription("Lista resumida de quadras otimizada para bots e integrações (Formato API / TCC)");
                            resp200.setContent(new Content().addMediaType("application/json", mediaType));
                        }
                    }
                });
            }

            // Define e ordena as tags exibidas no Swagger UI
            List<Tag> organizedTags = new ArrayList<>();
            organizedTags.add(new Tag().name("Quadras").description("Endpoints de quadras para consumo do Frontend e catálogo geral."));
            organizedTags.add(new Tag().name("Quadras - API - TCC").description("Endpoints da API (/api/quadras) otimizados para bots e integrações externas (retorno resumido)."));
            organizedTags.add(new Tag().name("Agendamentos").description("Endpoints de agendamentos e reservas para o Frontend."));
            organizedTags.add(new Tag().name("Agendamentos - API - TCC").description("Endpoints da API (/api/agendamentos) para agendamentos via Bot e integrações."));
            organizedTags.add(new Tag().name("Usuários e Autenticação").description("Endpoints de usuários e autenticação para o Frontend."));
            organizedTags.add(new Tag().name("Usuários - API - TCC").description("Endpoints da API (/api/usuarios) para integrações externas e login."));
            organizedTags.add(new Tag().name("Pagamentos").description("Endpoints de pagamentos Pix e simulações para o Frontend."));
            organizedTags.add(new Tag().name("Pagamentos - API - TCC").description("Endpoints da API (/api/pagamentos) e webhooks para parceiros."));
            organizedTags.add(new Tag().name("Notificações").description("Endpoints de notificações para o painel administrativo do Frontend."));
            organizedTags.add(new Tag().name("Notificações - API - TCC").description("Endpoints da API (/api/notificacoes) para streaming SSE e integrações."));
            organizedTags.add(new Tag().name("Bloqueios de Quadra").description("Endpoints de bloqueios de horários para o Frontend."));
            organizedTags.add(new Tag().name("Bloqueios - API - TCC").description("Endpoints da API (/api/quadras/bloqueios) para bloqueios externos."));

            openApi.setTags(organizedTags);
        };
    }

    private String getBaseTagName(String tag) {
        if (tag == null) return "Geral";
        if (tag.contains("Quadra") && !tag.contains("Bloqueio")) return "Quadras";
        if (tag.contains("Bloqueio")) return "Bloqueios de Quadra";
        if (tag.contains("Agendamento") || tag.contains("Reserva")) return "Agendamentos";
        if (tag.contains("Usuário") || tag.contains("Autenticação")) return "Usuários e Autenticação";
        if (tag.contains("Pagamento")) return "Pagamentos";
        if (tag.contains("Notificação")) return "Notificações";
        return tag;
    }

    private String getApiTagName(String base) {
        if ("Usuários e Autenticação".equals(base)) return "Usuários - API - TCC";
        if ("Bloqueios de Quadra".equals(base)) return "Bloqueios - API - TCC";
        return base + " - API - TCC";
    }
}
