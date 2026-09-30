package com.agendamentos.equadras.config;

import com.agendamentos.equadras.config.ExemplosDaApi.ErroExemplo;
import com.agendamentos.equadras.config.ExemplosDaApi.ExemploOperacao;
import com.agendamentos.equadras.config.ExemplosDaApi.Formato;
import com.agendamentos.equadras.config.ExemplosDaApi.RespostaExemplo;
import com.agendamentos.equadras.dto.response.ApiKeyCriadaDTO;
import com.agendamentos.equadras.dto.response.QuadraFotosResponseDTO;
import com.agendamentos.equadras.dto.response.QuadraResumoResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";
    private static final String API_KEY_SCHEME_NAME = "ApiKeyAuth";
    private static final ObjectMapper LEITOR = new ObjectMapper();

    @Bean
    public OpenAPI customOpenAPI() {
        Schema<String> timeSchema = new io.swagger.v3.oas.models.media.StringSchema()
                .example("14:00:00")
                .description("Horário no formato HH:mm:ss");

        return new OpenAPI()
                .servers(List.of(
                        new io.swagger.v3.oas.models.servers.Server()
                                .url("https://equadras.app")
                                .description("Servidor de Produção (HTTPS)"),
                        new io.swagger.v3.oas.models.servers.Server()
                                .url("/")
                                .description("Servidor Relativo (Mesma Origem)"),
                        new io.swagger.v3.oas.models.servers.Server()
                                .url("http://localhost:8080")
                                .description("Ambiente Local (Desenvolvimento)")
                ))
                .info(new Info()
                        .title("eQuadras API - Gestão e Agendamento Esportivo")
                        .description("Documentação oficial das APIs REST da plataforma eQuadras para integrações de sistemas parceiros, bots e clientes de API. Todos os endpoints incluem exemplos reais de requisição e resposta.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Suporte Técnico eQuadras")
                                .email("contato@equadras.app")
                                .url("https://github.com/GuilhermeAizzaSano/eQuadras"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME_NAME))
                .components(new Components()
                        .addSchemas("LocalTime", timeSchema)
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                         .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("API-Key")
                                        .description("Informe apenas a API-Key gerada no painel da conta (formato `eq_...`), sem o prefixo `Bearer` — ele é adicionado automaticamente, resultando em `Authorization: Bearer eq_...`. Alternativa equivalente ao `X-API-KEY`: use apenas um dos dois. Tokens JWT não são aceitos neste cabeçalho (a sessão web usa cookie)."))
                        .addSecuritySchemes(API_KEY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-API-KEY")
                                        .description("Chave de API pessoal gerada no painel do usuário (formato `eq_...`). Válida em todas as rotas da API respeitando os papéis (ROLE_CLIENT e ROLE_ADMIN)."))
                        );
    }

    // Ordena as propriedades dos schemas para que o documento gerado seja determinístico entre execuções
    @Bean
    public OpenApiCustomizer ordenarPropriedadesDosSchemasCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            openApi.getComponents().getSchemas().values().forEach(schema -> {
                if (schema.getProperties() != null) {
                    schema.setProperties(new java.util.TreeMap<>(schema.getProperties()));
                }
            });
        };
    }

    @Bean
    public OpenApiCustomizer filterExternalApiRoutesCustomizer() {
        return openApi -> {
            openApi.setServers(List.of(
                    new io.swagger.v3.oas.models.servers.Server()
                            .url("https://equadras.app")
                            .description("Servidor de Produção (HTTPS)"),
                    new io.swagger.v3.oas.models.servers.Server()
                            .url("/")
                            .description("Servidor Relativo (Mesma Origem)"),
                    new io.swagger.v3.oas.models.servers.Server()
                            .url("http://localhost:8080")
                            .description("Ambiente Local (Desenvolvimento)")
            ));

            if (openApi.getComponents() != null) {
                // QuadraResumoResponseDTO, QuadraFotosResponseDTO e ApiKeyCriadaDTO não são retornados por tipo em nenhum método (resposta é ResponseEntity<?>)
                for (Class<?> tipo : List.of(QuadraResumoResponseDTO.class, QuadraFotosResponseDTO.class, ApiKeyCriadaDTO.class)) {
                    Map<String, Schema> schemas = ModelConverters.getInstance().read(tipo);
                    schemas.forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
                }
                registrarSchemasAuxiliares(openApi);
            }

            if (openApi.getPaths() != null) {
                // Remove todas as rotas internas consultadas pelo frontend, mantendo apenas as rotas da API externa (/api/**)
                openApi.getPaths().entrySet().removeIf(entry -> !entry.getKey().startsWith("/api"));

                openApi.getPaths().forEach((path, pathItem) ->
                        pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                            if (operation.getTags() != null) {
                                List<String> newTags = operation.getTags().stream().map(this::getApiTagName).distinct().toList();
                                operation.setTags(newTags);
                            }
                            descreverParametrosDePaginacao(operation);
                            aplicarExemplos(path, httpMethod.name(), operation);
                        }));
            }

            // Define e ordena exclusivamente as tags da API externa no Swagger UI
            List<Tag> organizedTags = new ArrayList<>();
            organizedTags.add(new Tag().name("Usuários - API").description("Autenticação por cookie de sessão, perfil, chave de API pessoal e administração cadastral (Master Admin)."));
            organizedTags.add(new Tag().name("Quadras - API").description("Consulta de quadras (resumida ou completa), fotos, status operacional e gestão pelo administrador dono."));
            organizedTags.add(new Tag().name("Agendamentos - API").description("Criação de reservas com lock pessimista, agenda, contadores, horários disponíveis e integração com Bot."));
            organizedTags.add(new Tag().name("Bloqueios - API").description("Bloqueios administrativos de horários para torneios e manutenção."));
            organizedTags.add(new Tag().name("Pagamentos - API").description("Status de pagamento Pix, simulação de aprovação e webhook do Mercado Pago."));
            organizedTags.add(new Tag().name("Notificações - API").description("Histórico de notificações do administrador e stream SSE em tempo real."));
            organizedTags.add(new Tag().name("Auditoria & Logs - API").description("Trilha de auditoria do sistema (exclusivo do Master Admin)."));

            openApi.setTags(organizedTags);
        };
    }

    private void registrarSchemasAuxiliares(OpenAPI openApi) {
        Components componentes = openApi.getComponents();
        componentes.addSchemas("ProblemaErro", new ObjectSchema()
                .description("Corpo de erro (RFC 9457). Os filtros de segurança devolvem só status, title e detail.")
                .addProperty("type", new StringSchema())
                .addProperty("title", new StringSchema())
                .addProperty("status", new IntegerSchema())
                .addProperty("detail", new StringSchema())
                .addProperty("instance", new StringSchema())
                .addProperty("code", new StringSchema())
                .addProperty("timestamp", new StringSchema())
                .addProperty("camposIncorretos", new ArraySchema().items(new ObjectSchema()
                        .addProperty("campo", new StringSchema())
                        .addProperty("mensagem", new StringSchema()))));
        componentes.addSchemas("PageQuadraResponseDTO", new ObjectSchema()
                .description("Página (Spring Data) de quadras completas. Aparece em GET /api/quadras com formato completo e parâmetro page.")
                .addProperty("content", new ArraySchema().items(new Schema<>().$ref("#/components/schemas/QuadraResponseDTO")))
                .addProperty("pageable", new Schema<>().$ref("#/components/schemas/PageableObject"))
                .addProperty("sort", new Schema<>().$ref("#/components/schemas/SortObject"))
                .addProperty("totalElements", new IntegerSchema().format("int64"))
                .addProperty("totalPages", new IntegerSchema().format("int32"))
                .addProperty("size", new IntegerSchema().format("int32"))
                .addProperty("number", new IntegerSchema().format("int32"))
                .addProperty("numberOfElements", new IntegerSchema().format("int32"))
                .addProperty("first", new BooleanSchema())
                .addProperty("last", new BooleanSchema())
                .addProperty("empty", new BooleanSchema()));
        componentes.addSchemas("FotosSemResultado", new ObjectSchema()
                .description("Resposta de GET /api/quadras/fotos quando há filtro e nenhuma quadra corresponde.")
                .addProperty("fotos", new ArraySchema().items(new StringSchema())));
    }

    // Parâmetros gerados a partir de Pageable (page, size, sort) chegam sem descrição
    private void descreverParametrosDePaginacao(Operation operation) {
        if (operation.getParameters() == null) {
            return;
        }
        operation.getParameters().forEach(parametro -> {
            if (parametro.getDescription() != null) {
                return;
            }
            switch (parametro.getName()) {
                case "page" -> parametro.setDescription("Índice da página, começando em 0 (`spring.data.web.pageable.one-indexed-parameters=false`).");
                case "size" -> parametro.setDescription("Itens por página. Padrão 10; máximo 50 (valores maiores são limitados a 50).");
                case "sort" -> parametro.setDescription("Ordenação `campo,asc|desc`. Campos aceitos: veja a descrição da operação. Campo fora da lista devolve 400 (`Ordenação Inválida`).");
                default -> { }
            }
        });
    }

    private void aplicarExemplos(String path, String metodo, Operation operation) {
        normalizarMediaTypes(operation);
        ExemploOperacao exemplo = ExemplosDaApi.buscar(metodo, path);
        if (exemplo == null) {
            return;
        }
        String contexto = ExemplosDaApi.chave(metodo, path);
        if (exemplo.requestJson() != null) {
            definirExemploDeRequest(operation, lerJsonDeExemplo(exemplo.requestJson(), contexto + " [request]"));
        }
        Map<String, List<RespostaExemplo>> sucessos = new LinkedHashMap<>();
        exemplo.respostas().forEach(r -> sucessos.computeIfAbsent(r.status(), k -> new ArrayList<>()).add(r));
        sucessos.forEach((status, lista) -> definirSucesso(operation, contexto, status, lista));
        removerRespostasAutomaticasEspurias(operation, sucessos.keySet());

        Map<Integer, List<ErroExemplo>> erros = new LinkedHashMap<>();
        exemplo.erros().forEach(e -> erros.computeIfAbsent(e.status(), k -> new ArrayList<>()).add(e));
        erros.forEach((status, lista) -> definirErros(operation, path, status, lista));
    }

    // Normaliza todos os media types das respostas existentes de */* para application/json
    private void normalizarMediaTypes(Operation operation) {
        if (operation.getResponses() == null) {
            return;
        }
        operation.getResponses().forEach((code, response) -> {
            if (response.getContent() != null && response.getContent().containsKey("*/*")) {
                MediaType padrao = response.getContent().remove("*/*");
                response.getContent().addMediaType(ExemplosDaApi.JSON, padrao);
            }
        });
    }

    static JsonNode lerJsonDeExemplo(String json, String contexto) {
        try {
            return LEITOR.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Exemplo OpenAPI com JSON inválido em " + contexto + ": " + e.getOriginalMessage(), e);
        }
    }

    private void definirExemploDeRequest(Operation operation, JsonNode exemplo) {
        RequestBody body = operation.getRequestBody();
        if (body == null) {
            body = new RequestBody().required(true);
            operation.setRequestBody(body);
        }
        if (body.getContent() == null) {
            body.setContent(new Content());
        }
        MediaType mediaType = body.getContent().get(ExemplosDaApi.JSON);
        if (mediaType == null) {
            mediaType = new MediaType();
            body.getContent().addMediaType(ExemplosDaApi.JSON, mediaType);
        }
        mediaType.setExample(exemplo);
    }

    private ApiResponse respostaDe(Operation operation, String status) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        ApiResponse response = responses.get(status);
        if (response == null) {
            response = new ApiResponse();
            responses.addApiResponse(status, response);
        }
        return response;
    }

    private void definirSucesso(Operation operation, String contexto, String status, List<RespostaExemplo> lista) {
        ApiResponse response = respostaDe(operation, status);
        RespostaExemplo primeira = lista.get(0);
        response.setDescription(primeira.descricao());
        primeira.cabecalhos().forEach((nome, descricao) ->
                response.addHeaderObject(nome, new Header().description(descricao).schema(new StringSchema())));
        if (primeira.conteudo() == null) {
            response.setContent(null);
            return;
        }
        if (response.getContent() == null) {
            response.setContent(new Content());
        }
        MediaType mediaType = response.getContent().get(primeira.mediaType());
        if (mediaType == null) {
            mediaType = new MediaType();
            response.getContent().addMediaType(primeira.mediaType(), mediaType);
        }
        if (!ExemplosDaApi.JSON.equals(primeira.mediaType())) {
            mediaType.setExample(primeira.conteudo());
            return;
        }
        boolean nomeado = lista.size() > 1 || primeira.nome() != null;
        List<Schema> alternativas = new ArrayList<>();
        for (RespostaExemplo r : lista) {
            JsonNode json = lerJsonDeExemplo(r.conteudo(), contexto + " [" + status + (r.nome() != null ? ":" + r.nome() : "") + "]");
            if (nomeado) {
                mediaType.addExamples(r.nome(), new Example().summary(r.descricao()).value(json));
            } else {
                mediaType.setExample(json);
            }
            if (r.schema() != null) {
                alternativas.add(schemaDe(r.schema()));
            }
        }
        if (alternativas.size() == 1) {
            mediaType.setSchema(alternativas.get(0));
        } else if (alternativas.size() > 1) {
            mediaType.setSchema(new ComposedSchema().oneOf(alternativas));
        }
    }

    private Schema<?> schemaDe(String notacao) {
        if (notacao.startsWith("array:")) {
            return new ArraySchema().items(new Schema<>().$ref("#/components/schemas/" + notacao.substring(6)));
        }
        return new Schema<>().$ref("#/components/schemas/" + notacao);
    }

    // O springdoc cria um "200" sem exemplo ao lado do 201 (POST) e do 204 (DELETE)
    private void removerRespostasAutomaticasEspurias(Operation operation, Set<String> documentados) {
        if (operation.getResponses() != null && !documentados.contains("200")
                && documentados.stream().anyMatch(s -> s.startsWith("2"))) {
            operation.getResponses().remove("200");
        }
    }

    private void definirErros(Operation operation, String path, int status, List<ErroExemplo> lista) {
        ApiResponse response = new ApiResponse().description(lista.get(0).titulo());
        MediaType mediaType = new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemaErro"));
        Set<String> nomes = new HashSet<>();
        for (ErroExemplo erro : lista) {
            String nome = erro.code() != null ? erro.code() : "erro" + status;
            while (!nomes.add(nome)) {
                nome = nome + "_2";
            }
            JsonNode corpo = corpoDeErro(erro, path);
            if (lista.size() == 1) {
                mediaType.setExample(corpo);
            } else {
                mediaType.addExamples(nome, new Example().summary(erro.detalhe()).value(corpo));
            }
            erro.cabecalhos().forEach((cabecalho, descricao) ->
                    response.addHeaderObject(cabecalho, new Header().description(descricao).schema(new StringSchema())));
        }
        // Map devolvido pelo próprio controller sai como application/json, não problem+json
        String tipoDeMidia = lista.stream().allMatch(e -> e.formato() == Formato.MAPA)
                ? ExemplosDaApi.JSON : ExemplosDaApi.PROBLEM_JSON;
        response.setContent(new Content().addMediaType(tipoDeMidia, mediaType));
        operation.getResponses().addApiResponse(String.valueOf(status), response);
    }

    private JsonNode corpoDeErro(ErroExemplo erro, String path) {
        ObjectNode corpo = LEITOR.createObjectNode();
        switch (erro.formato()) {
            case FILTRO, MAPA -> {
                corpo.put("status", erro.status());
                corpo.put("title", erro.titulo());
                corpo.put("detail", erro.detalhe());
            }
            case SIMPLES -> {
                corpo.put("type", "about:blank");
                corpo.put("title", erro.titulo());
                corpo.put("status", erro.status());
                corpo.put("detail", erro.detalhe());
            }
            case HANDLER, VALIDACAO -> {
                corpo.put("type", "https://api.equadras.com/erros/" + erro.slug());
                corpo.put("title", erro.titulo());
                corpo.put("status", erro.status());
                corpo.put("detail", erro.detalhe());
                corpo.put("instance", path);
                corpo.put("code", erro.code());
                corpo.put("timestamp", "2026-09-29T14:30:00.123456Z");
                if (erro.formato() == Formato.VALIDACAO) {
                    ArrayNode campos = corpo.putArray("camposIncorretos");
                    campos.addObject().put("campo", "Campo").put("mensagem", "Mensagem de validação do campo");
                }
            }
        }
        return corpo;
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
