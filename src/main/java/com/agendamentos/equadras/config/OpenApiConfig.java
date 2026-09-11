package com.agendamentos.equadras.config;

import com.agendamentos.equadras.dto.response.QuadraResumoResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
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
import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                // Registra explicitamente o schema de QuadraResumoResponseDTO para exibição no Swagger
                Map<String, Schema> schemas = ModelConverters.getInstance().read(QuadraResumoResponseDTO.class);
                schemas.forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
            }

            if (openApi.getPaths() != null) {
                // Remove todas as rotas internas consultadas pelo frontend, mantendo apenas as rotas da API externa (/api/**)
                openApi.getPaths().entrySet().removeIf(entry -> !entry.getKey().startsWith("/api"));

                openApi.getPaths().forEach((path, pathItem) -> {
                    // Customiza resposta padrão do GET /api/quadras para formato resumido
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

                    // Itera operações HTTP (GET, POST, PUT, PATCH, DELETE) aplicando tags e exemplos ricos de request/response
                    pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                        if (operation.getTags() != null) {
                            List<String> newTags = operation.getTags().stream().map(this::getApiTagName).distinct().toList();
                            operation.setTags(newTags);
                        }
                        enrichOperationExamples(path, httpMethod.name(), operation);
                    });
                });
            }

            // Define e ordena exclusivamente as tags da API externa no Swagger UI
            List<Tag> organizedTags = new ArrayList<>();
            organizedTags.add(new Tag().name("Usuários - API").description("Autenticação JWT, perfil de usuário e administração cadastral."));
            organizedTags.add(new Tag().name("Quadras - API").description("Consulta de quadras, esportes suportados, fotos e status operacional."));
            organizedTags.add(new Tag().name("Agendamentos - API").description("Criação de reservas com Lock pessimista, consulta de horários disponíveis e integração com Bot."));
            organizedTags.add(new Tag().name("Bloqueios - API").description("Bloqueios administrativos de horários para torneios e manutenção."));
            organizedTags.add(new Tag().name("Pagamentos - API").description("Cobranças Pix automáticas, status de pagamento e webhooks de conciliação."));
            organizedTags.add(new Tag().name("Notificações - API").description("Histórico de notificações e stream SSE em tempo real."));

            openApi.setTags(organizedTags);
        };
    }

    private void enrichOperationExamples(String path, String method, Operation operation) {
        // 1. Normalizar todos os media types das respostas existentes de */* para application/json
        if (operation.getResponses() != null) {
            operation.getResponses().forEach((code, response) -> {
                if (response.getContent() != null && response.getContent().containsKey("*/*")) {
                    MediaType defaultMedia = response.getContent().remove("*/*");
                    response.getContent().addMediaType("application/json", defaultMedia);
                }
            });
        }

        // 2. Aplicar exemplos específicos de acordo com a rota e o método HTTP
        switch (path) {
            case "/api/usuarios/login" -> {
                setRequestExample(operation, """
                    {
                      "email_usuario": "arthur.prado@email.com",
                      "senha_usuario": "SenhaSegura@123"
                    }
                    """);
                setResponseExample(operation, "200", """
                    {
                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMCIsInJvbGUiOiJDTElFTlQiLCJpYXQiOjE3MjU4NTAwMDB9...",
                      "usuario": {
                        "id_usuario": 10,
                        "nome_usuario": "Arthur Prado",
                        "email_usuario": "arthur.prado@email.com",
                        "phone_usuario": "(11) 99999-8888",
                        "role": "CLIENT",
                        "criadoEm": "2026-09-04T10:00:00"
                      }
                    }
                    """, "Login autenticado com sucesso e token JWT emitido");
                addErrorResponse(operation, "400", "Requisição Inválida", "Credenciais de e-mail ou senha inválidas.", path);
            }

            case "/api/usuarios" -> {
                if ("POST".equalsIgnoreCase(method)) {
                    setRequestExample(operation, """
                        {
                          "nome_usuario": "Carlos Eduardo",
                          "email_usuario": "carlos.eduardo@email.com",
                          "phone_usuario": "(11) 98888-7777",
                          "senha_usuario": "SenhaForte@123"
                        }
                        """);
                    setResponseExample(operation, "201", """
                        {
                          "id_usuario": 12,
                          "nome_usuario": "Carlos Eduardo",
                          "email_usuario": "carlos.eduardo@email.com",
                          "phone_usuario": "(11) 98888-7777",
                          "role": "CLIENT",
                          "criadoEm": "2026-09-09T00:30:00"
                        }
                        """, "Usuário cadastrado com sucesso");
                    addErrorResponse(operation, "400", "Requisição Inválida", "E-mail já cadastrado na plataforma.", path);
                } else if ("GET".equalsIgnoreCase(method)) {
                    setResponseExample(operation, "200", """
                        [
                          {
                            "id_usuario": 10,
                            "nome_usuario": "Arthur Prado",
                            "email_usuario": "arthur.prado@email.com",
                            "phone_usuario": "(11) 99999-8888",
                            "role": "CLIENT",
                            "criadoEm": "2026-09-04T10:00:00"
                          },
                          {
                            "id_usuario": 1,
                            "nome_usuario": "Administrador Arena",
                            "email_usuario": "admin@equadras.app",
                            "phone_usuario": "(11) 97777-1111",
                            "role": "ADMIN",
                            "criadoEm": "2026-09-01T08:00:00"
                          }
                        ]
                        """, "Lista de usuários cadastrados");
                }
            }

            case "/api/usuarios/{id}" -> {
                if ("GET".equalsIgnoreCase(method)) {
                    setResponseExample(operation, "200", """
                        {
                          "id_usuario": 10,
                          "nome_usuario": "Arthur Prado",
                          "email_usuario": "arthur.prado@email.com",
                          "phone_usuario": "(11) 99999-8888",
                          "role": "CLIENT",
                          "criadoEm": "2026-09-04T10:00:00"
                        }
                        """, "Dados do usuário localizado");
                    addErrorResponse(operation, "404", "Usuário Não Encontrado", "Usuário não localizado para o ID informado.", path);
                } else if ("PUT".equalsIgnoreCase(method)) {
                    setRequestExample(operation, """
                        {
                          "nome_usuario": "Arthur Prado Silva",
                          "email_usuario": "arthur.silva@email.com",
                          "phone_usuario": "(11) 99999-9999"
                        }
                        """);
                    setResponseExample(operation, "200", """
                        {
                          "id_usuario": 10,
                          "nome_usuario": "Arthur Prado Silva",
                          "email_usuario": "arthur.silva@email.com",
                          "phone_usuario": "(11) 99999-9999",
                          "role": "CLIENT",
                          "criadoEm": "2026-09-04T10:00:00"
                        }
                        """, "Dados do usuário atualizados");
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    setEmptySuccessResponse(operation, "204", "Usuário removido com sucesso");
                }
            }

            case "/api/usuarios/minha-senha" -> {
                setRequestExample(operation, """
                    {
                      "senhaAtual": "SenhaVelha@123",
                      "novaSenha": "SenhaSuperSegura@456"
                    }
                    """);
                setResponseExample(operation, "200", """
                    {
                      "mensagem": "Senha alterada com sucesso."
                    }
                    """, "Confirmação de alteração de senha");
            }

            case "/api/usuarios/logout" -> {
                setResponseExample(operation, "200", """
                    {
                      "mensagem": "Logout realizado com sucesso."
                    }
                    """, "Sessão encerrada com sucesso");
            }

            case "/api/quadras" -> {
                if ("GET".equalsIgnoreCase(method)) {
                    setResponseExample(operation, "200", """
                        [
                          {
                            "id_quadra": 1,
                            "nome": "Arena Gol Society",
                            "tipo": "FUTEBOL_SOCIETY",
                            "valorHora": 140.00,
                            "coberta": true,
                            "endereco": "Av. Brasil, 1500 - São Paulo, SP",
                            "distanciaKm": 2.4,
                            "ativa": true,
                            "fotoCapa": "https://equadras.app/uploads/quadra-1-capa.jpg"
                          },
                          {
                            "id_quadra": 2,
                            "nome": "Praia & Sol Beach Tennis",
                            "tipo": "BEACH_TENNIS",
                            "valorHora": 90.00,
                            "coberta": false,
                            "endereco": "Rua das Palmeiras, 300 - São Paulo, SP",
                            "distanciaKm": 3.8,
                            "ativa": true,
                            "fotoCapa": "https://equadras.app/uploads/quadra-2-capa.jpg"
                          }
                        ]
                        """, "Lista de quadras ativas da arena");
                } else if ("POST".equalsIgnoreCase(method)) {
                    setRequestExample(operation, """
                        {
                          "nome": "Quadra de Vôlei de Areia",
                          "tipo": "VOLEI_PRAIA",
                          "valorHora": 100.00,
                          "coberta": false,
                          "endereco": "Av. Brasil, 1500 - São Paulo, SP",
                          "disponibilidades": [
                            { "diaSemana": "MONDAY", "horaInicio": "07:00:00", "horaFim": "23:00:00" },
                            { "diaSemana": "SATURDAY", "horaInicio": "08:00:00", "horaFim": "22:00:00" }
                          ]
                        }
                        """);
                    setResponseExample(operation, "201", """
                        {
                          "id_quadra": 3,
                          "nome": "Quadra de Vôlei de Areia",
                          "tipo": "VOLEI_PRAIA",
                          "valorHora": 100.00,
                          "coberta": false,
                          "endereco": "Av. Brasil, 1500 - São Paulo, SP",
                          "ativa": true,
                          "fotoCapa": null,
                          "fotos": []
                        }
                        """, "Quadra cadastrada com sucesso");
                }
            }

            case "/api/quadras/{id}" -> {
                if ("GET".equalsIgnoreCase(method)) {
                    setResponseExample(operation, "200", """
                        {
                          "id_quadra": 1,
                          "nome": "Arena Gol Society",
                          "tipoEsporte": "FUTEBOL",
                          "valorHora": 140.00,
                          "ativa": true,
                          "cep": "15000-000",
                          "logradouro": "Av. Brasil, 1500",
                          "bairro": "Jardim das Flores",
                          "cidade": "São José do Rio Preto",
                          "estado": "SP",
                          "latitude": -20.8113,
                          "longitude": -49.3758,
                          "descricao": "Grama sintética padrão FIFA com iluminação em LED e vestiários.",
                          "dataLimiteAgendamento": "2026-12-31",
                          "fotos": ["https://equadras.app/uploads/quadras/1_principal.jpg"],
                          "disponibilidades": [
                            { "diaSemana": "MONDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" },
                            { "diaSemana": "FRIDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" }
                          ]
                        }
                        """, "Detalhes completos da quadra informada");
                    addErrorResponse(operation, "404", "Quadra Não Encontrada", "Quadra não localizada para o ID fornecido.", path);
                } else if ("PUT".equalsIgnoreCase(method)) {
                    setRequestExample(operation, """
                        {
                          "nome": "Arena Gol Society Coberta",
                          "tipoEsporte": "FUTEBOL",
                          "valorHora": 150.00,
                          "cep": "15000-000",
                          "logradouro": "Av. Brasil, 1500",
                          "bairro": "Jardim das Flores",
                          "cidade": "São José do Rio Preto",
                          "estado": "SP",
                          "latitude": -20.8113,
                          "longitude": -49.3758,
                          "descricao": "Grama sintética padrão FIFA com iluminação em LED e vestiários.",
                          "dataLimiteAgendamento": "2026-12-31",
                          "fotos": ["https://equadras.app/uploads/quadras/1_principal.jpg"],
                          "disponibilidades": [
                            { "diaSemana": "MONDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" },
                            { "diaSemana": "FRIDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" }
                          ]
                        }
                        """);
                    setResponseExample(operation, "200", """
                        {
                          "id_quadra": 1,
                          "nome": "Arena Gol Society Coberta",
                          "tipoEsporte": "FUTEBOL",
                          "valorHora": 150.00,
                          "ativa": true,
                          "cep": "15000-000",
                          "logradouro": "Av. Brasil, 1500",
                          "bairro": "Jardim das Flores",
                          "cidade": "São José do Rio Preto",
                          "estado": "SP",
                          "latitude": -20.8113,
                          "longitude": -49.3758,
                          "descricao": "Grama sintética padrão FIFA com iluminação em LED e vestiários.",
                          "dataLimiteAgendamento": "2026-12-31",
                          "fotos": ["https://equadras.app/uploads/quadras/1_principal.jpg"],
                          "disponibilidades": [
                            { "diaSemana": "MONDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" },
                            { "diaSemana": "FRIDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" }
                          ]
                        }
                        """, "Dados cadastrais e horários da quadra atualizados");
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    setEmptySuccessResponse(operation, "204", "Quadra removida com sucesso");
                }
            }

            case "/api/quadras/{id}/status" -> {
                setResponseExample(operation, "200", """
                    {
                      "id_quadra": 1,
                      "ativa": false,
                      "mensagem": "Status operacional da quadra atualizado com sucesso."
                    }
                    """, "Confirmação de alteração de status");
            }

            case "/api/agendamentos" -> {
                if ("POST".equalsIgnoreCase(method)) {
                    setRequestExample(operation, """
                        {
                          "quadraId": 1,
                          "dataHoraInicio": "2026-09-12T19:00:00",
                          "dataHoraFim": "2026-09-12T20:00:00"
                        }
                        """);
                    setResponseExample(operation, "201", """
                        {
                          "id_agendamento": 42,
                          "usuarioId": 10,
                          "nomeUsuario": "Arthur Prado",
                          "telefoneUsuario": "(11) 99999-8888",
                          "quadraId": 1,
                          "nomeQuadra": "Arena Gol Society",
                          "dataHoraInicio": "2026-09-12T19:00:00",
                          "dataHoraFim": "2026-09-12T20:00:00",
                          "valorTotal": 140.00,
                          "status": "PENDENTE",
                          "transacaoPagamentoId": "mp-pix-987654321",
                          "pixCopiaECola": "00020126580014br.gov.bcb.pix0136123e4567-e89b-12d3-a456-4266141740005204000053039865405140.005802BR5913eQuadras Arena6009Sao Paulo62070503***6304ABCD",
                          "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAAAMgAAADICAYAAACtWK6e...",
                          "criadoEm": "2026-09-09T00:30:00"
                        }
                        """, "Reserva criada com sucesso e cobrança Pix gerada");
                    addErrorResponse(operation, "409", "Conflito de Horário", "A quadra já possui uma reserva confirmada ou bloqueio ativo para este horário.", path);
                } else if ("GET".equalsIgnoreCase(method)) {
                    setResponseExample(operation, "200", """
                        [
                          {
                            "id_agendamento": 42,
                            "usuarioId": 10,
                            "nomeUsuario": "Arthur Prado",
                            "telefoneUsuario": "(11) 99999-8888",
                            "quadraId": 1,
                            "nomeQuadra": "Arena Gol Society",
                            "dataHoraInicio": "2026-09-12T19:00:00",
                            "dataHoraFim": "2026-09-12T20:00:00",
                            "valorTotal": 140.00,
                            "status": "CONFIRMADO",
                            "transacaoPagamentoId": "mp-pix-987654321",
                            "pixCopiaECola": "00020126580014br.gov.bcb.pix0136123e4567-e89b-12d3...",
                            "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAA...",
                            "criadoEm": "2026-09-09T00:30:00"
                          }
                        ]
                        """, "Lista de agendamentos");
                }
            }

            case "/api/agendamentos/bot" -> {
                setRequestExample(operation, """
                    {
                      "nomeCliente": "Lucas Silveira",
                      "telefoneCliente": "(11) 97777-6666",
                      "quadraId": 1,
                      "dataHoraInicio": "2026-09-12T20:00:00",
                      "dataHoraFim": "2026-09-12T21:00:00"
                    }
                    """);
                setResponseExample(operation, "201", """
                    {
                      "id_agendamento": 43,
                      "usuarioId": 15,
                      "nomeUsuario": "Lucas Silveira",
                      "telefoneUsuario": "(11) 97777-6666",
                      "quadraId": 1,
                      "nomeQuadra": "Arena Gol Society",
                      "dataHoraInicio": "2026-09-12T20:00:00",
                      "dataHoraFim": "2026-09-12T21:00:00",
                      "valorTotal": 140.00,
                      "status": "PENDENTE",
                      "transacaoPagamentoId": "mp-pix-123456789",
                      "pixCopiaECola": "00020126580014br.gov.bcb.pix0136123e4567-e89b-12d3...",
                      "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAA...",
                      "criadoEm": "2026-09-09T00:30:00"
                    }
                    """, "Agendamento rápido via Bot criado com cobrança Pix");
                addErrorResponse(operation, "409", "Conflito de Horário", "A quadra já possui reserva ou bloqueio para o horário solicitado.", path);
            }

            case "/api/agendamentos/{id}" -> {
                setResponseExample(operation, "200", """
                    {
                      "id_agendamento": 42,
                      "usuarioId": 10,
                      "nomeUsuario": "Arthur Prado",
                      "telefoneUsuario": "(11) 99999-8888",
                      "quadraId": 1,
                      "nomeQuadra": "Arena Gol Society",
                      "dataHoraInicio": "2026-09-12T19:00:00",
                      "dataHoraFim": "2026-09-12T20:00:00",
                      "valorTotal": 140.00,
                      "status": "CONFIRMADO",
                      "transacaoPagamentoId": "mp-pix-987654321",
                      "pixCopiaECola": "00020126580014br.gov.bcb.pix0136123e4567-e89b-12d3...",
                      "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAA...",
                      "criadoEm": "2026-09-09T00:30:00"
                    }
                    """, "Detalhes completos do agendamento");
                addErrorResponse(operation, "404", "Agendamento Não Encontrado", "Agendamento não localizado para o ID fornecido.", path);
            }

            case "/api/agendamentos/{id}/cancelar" -> {
                setResponseExample(operation, "200", """
                    {
                      "id_agendamento": 42,
                      "usuarioId": 10,
                      "nomeUsuario": "Arthur Prado",
                      "telefoneUsuario": "(11) 99999-8888",
                      "quadraId": 1,
                      "nomeQuadra": "Arena Gol Society",
                      "dataHoraInicio": "2026-09-12T19:00:00",
                      "dataHoraFim": "2026-09-12T20:00:00",
                      "valorTotal": 140.00,
                      "status": "CANCELADO",
                      "transacaoPagamentoId": "mp-pix-987654321",
                      "pixCopiaECola": "00020126580014br.gov.bcb.pix0136123e4567-e89b-12d3...",
                      "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAA...",
                      "criadoEm": "2026-09-09T00:30:00"
                    }
                    """, "Agendamento cancelado com sucesso");
            }

            case "/api/agendamentos/quadra/{quadraId}/horarios-disponiveis", "/api/agendamentos/horarios-disponiveis" -> {
                setResponseExample(operation, "200", """
                    [
                      {
                        "inicio": "18:00:00",
                        "fim": "19:00:00",
                        "status": "DISPONIVEL",
                        "disponivel": true,
                        "motivo": null
                      },
                      {
                        "inicio": "19:00:00",
                        "fim": "20:00:00",
                        "status": "AGENDADO",
                        "disponivel": false,
                        "motivo": "Horário já reservado por outro atleta"
                      },
                      {
                        "inicio": "20:00:00",
                        "fim": "21:00:00",
                        "status": "BLOQUEADO",
                        "disponivel": false,
                        "motivo": "Manutenção periódica na quadra"
                      },
                      {
                        "inicio": "21:00:00",
                        "fim": "22:00:00",
                        "status": "DISPONIVEL",
                        "disponivel": true,
                        "motivo": null
                      }
                    ]
                    """, "Grade completa de slots de 1 hora para o dia com disponibilidade");
                addErrorResponse(operation, "400", "Data Inválida", "A data informada é inválida ou não segue o formato YYYY-MM-DD.", path);
                addErrorResponse(operation, "404", "Quadra Não Encontrada", "A quadra informada não foi localizada.", path);
            }

            case "/api/agendamentos/dia" -> {
                setResponseExample(operation, "200", """
                    {
                      "1": [
                        { "inicio": "18:00:00", "fim": "19:00:00", "status": "DISPONIVEL", "disponivel": true, "motivo": null },
                        { "inicio": "19:00:00", "fim": "20:00:00", "status": "AGENDADO", "disponivel": false, "motivo": "Agendado" }
                      ],
                      "2": [
                        { "inicio": "18:00:00", "fim": "19:00:00", "status": "DISPONIVEL", "disponivel": true, "motivo": null }
                      ]
                    }
                    """, "Grade consolidada de todas as quadras da arena para o dia indicado");
            }

            case "/api/quadras/bloqueios", "/api/quadras/{id}/bloqueios" -> {
                if ("POST".equalsIgnoreCase(method)) {
                    setRequestExample(operation, """
                        {
                          "quadraId": 1,
                          "data": "2026-09-12",
                          "horaInicio": "14:00:00",
                          "horaFim": "18:00:00",
                          "motivo": "Torneio Interno da Arena"
                        }
                        """);
                    setResponseExample(operation, "201", """
                        {
                          "id_bloqueio": 5,
                          "quadraId": 1,
                          "nomeQuadra": "Arena Gol Society",
                          "data": "2026-09-12",
                          "horaInicio": "14:00:00",
                          "horaFim": "18:00:00",
                          "motivo": "Torneio Interno da Arena",
                          "criadoEm": "2026-09-09T00:30:00"
                        }
                        """, "Bloqueio criado com sucesso");
                    addErrorResponse(operation, "409", "Conflito de Bloqueio", "Já existem reservas ou bloqueios no intervalo informado.", path);
                } else if ("GET".equalsIgnoreCase(method)) {
                    setResponseExample(operation, "200", """
                        [
                          {
                            "id_bloqueio": 5,
                            "quadraId": 1,
                            "nomeQuadra": "Arena Gol Society",
                            "data": "2026-09-12",
                            "horaInicio": "14:00:00",
                            "horaFim": "18:00:00",
                            "motivo": "Torneio Interno da Arena",
                            "criadoEm": "2026-09-09T00:30:00"
                          }
                        ]
                        """, "Lista de bloqueios da quadra");
                }
            }

            case "/api/pagamentos/{agendamentoId}/status" -> {
                setResponseExample(operation, "200", """
                    {
                      "agendamentoId": 42,
                      "status": "CONFIRMADO",
                      "pago": true,
                      "mensagem": "Pagamento Pix confirmado com sucesso."
                    }
                    """, "Status atualizado da liquidação do Pix");
            }

            case "/api/pagamentos/{agendamentoId}/simular-aprovacao" -> {
                setResponseExample(operation, "200", """
                    {
                      "agendamentoId": 42,
                      "status": "CONFIRMADO",
                      "pago": true,
                      "mensagem": "Pagamento aprovado em ambiente de desenvolvimento/testes."
                    }
                    """, "Simulação de aprovação concluída");
            }

            case "/api/pagamentos/webhook" -> {
                setResponseExample(operation, "200", """
                    {
                      "status": "ok"
                    }
                    """, "Webhook recebido e processado");
            }

            case "/api/notificacoes/admin" -> {
                setResponseExample(operation, "200", """
                    [
                      {
                        "id": 1,
                        "mensagem": "Nova reserva confirmada: Arthur Prado em Arena Gol Society às 19:00.",
                        "lida": false,
                        "dataCriacao": "2026-09-09T00:30:00"
                      }
                    ]
                    """, "Lista de alertas e notificações da arena");
            }

            case "/api/notificacoes/{id}/ler" -> {
                setResponseExample(operation, "200", """
                    {
                      "id": 1,
                      "lida": true
                    }
                    """, "Notificação marcada como lida");
            }

            case "/api/notificacoes/ler-todas" -> {
                setResponseExample(operation, "200", """
                    {
                      "mensagem": "Todas as notificações foram marcadas como lidas."
                    }
                    """, "Notificações marcadas como lidas");
            }

            case "/api/agendamentos/quadra/{quadraId}/data" -> {
                setResponseExample(operation, "200", """
                    [
                      {
                        "id_agendamento": 42,
                        "usuarioId": 10,
                        "nomeUsuario": "Arthur Prado",
                        "telefoneUsuario": "(11) 99999-8888",
                        "quadraId": 1,
                        "nomeQuadra": "Arena Gol Society",
                        "dataHoraInicio": "2026-09-12T19:00:00",
                        "dataHoraFim": "2026-09-12T20:00:00",
                        "valorTotal": 140.00,
                        "status": "CONFIRMADO",
                        "transacaoPagamentoId": "mp-pix-987654321",
                        "pixCopiaECola": null,
                        "qrCodeBase64": null,
                        "criadoEm": "2026-09-09T00:30:00"
                      }
                    ]
                    """, "Lista de agendamentos da quadra na data informada");
            }

            case "/api/quadras/{id}/fotos", "/api/quadras/fotos" -> {
                if ("GET".equalsIgnoreCase(method)) {
                    setResponseExample(operation, "200", """
                        [
                          {
                            "id": 1,
                            "nome": "Arena Gol Society",
                            "fotos": [
                              "https://equadras.app/uploads/quadras/1_principal.jpg"
                            ]
                          }
                        ]
                        """, "Galeria de fotos da quadra");
                } else if ("POST".equalsIgnoreCase(method)) {
                    setResponseExample(operation, "200", """
                        {
                          "id_quadra": 1,
                          "nome": "Arena Gol Society",
                          "tipoEsporte": "FUTEBOL",
                          "valorHora": 140.00,
                          "ativa": true,
                          "fotos": [
                            "https://equadras.app/uploads/quadras/1_principal.jpg",
                            "https://equadras.app/uploads/quadras/1_nova.jpg"
                          ]
                        }
                        """, "Fotos adicionadas à galeria com sucesso");
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    setResponseExample(operation, "200", """
                        {
                          "id_quadra": 1,
                          "nome": "Arena Gol Society",
                          "tipoEsporte": "FUTEBOL",
                          "valorHora": 140.00,
                          "ativa": true,
                          "fotos": []
                        }
                        """, "Foto removida com sucesso da galeria");
                }
            }

            case "/api/quadras/{quadraId}/bloqueios/{bloqueioId}" -> {
                if ("DELETE".equalsIgnoreCase(method)) {
                    setEmptySuccessResponse(operation, "204", "Bloqueio removido com sucesso");
                }
            }

            case "/api/quadras/{quadraId}/desbloquear" -> {
                setRequestExample(operation, """
                    {
                      "bloqueioId": 5,
                      "data": "2026-09-12",
                      "horaInicio": "14:00:00",
                      "horaFim": "18:00:00"
                    }
                    """);
                setResponseExample(operation, "200", """
                    {
                      "mensagem": "Horários desbloqueados com sucesso.",
                      "totalRemovidos": 1
                    }
                    """, "Horários desbloqueados com sucesso");
            }

            case "/api/notificacoes/stream" -> {
                setStreamResponseExample(operation, "200", """
                    data: {"id":1,"mensagem":"Nova reserva confirmada","lida":false,"dataCriacao":"2026-09-09T00:30:00"}
                    
                    """, "Stream SSE de eventos de notificações em tempo real");
            }

            default -> {
                // Caso padrão para endpoints auxiliares
            }
        }
    }

    private void setEmptySuccessResponse(Operation operation, String statusCode, String description) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        ApiResponse response = responses.get(statusCode);
        if (response == null) {
            response = new ApiResponse();
            responses.addApiResponse(statusCode, response);
        }
        response.setDescription(description);
    }

    private void setStreamResponseExample(Operation operation, String statusCode, String sseData, String description) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        ApiResponse response = responses.get(statusCode);
        if (response == null) {
            response = new ApiResponse();
            responses.addApiResponse(statusCode, response);
        }
        response.setDescription(description);
        if (response.getContent() == null) {
            response.setContent(new Content());
        }
        MediaType mediaType = new MediaType().example(sseData);
        response.getContent().addMediaType("text/event-stream", mediaType);
    }

    private void setRequestExample(Operation operation, String jsonExample) {
        try {
            JsonNode parsed = objectMapper.readTree(jsonExample);
            RequestBody body = operation.getRequestBody();
            if (body == null) {
                body = new RequestBody().required(true);
                operation.setRequestBody(body);
            }
            if (body.getContent() == null) {
                body.setContent(new Content());
            }
            MediaType mediaType = body.getContent().get("application/json");
            if (mediaType == null) {
                mediaType = new MediaType();
                body.getContent().addMediaType("application/json", mediaType);
            }
            mediaType.setExample(parsed);
        } catch (Exception ignored) {
        }
    }

    private void setResponseExample(Operation operation, String statusCode, String jsonExample, String description) {
        try {
            JsonNode parsed = objectMapper.readTree(jsonExample);
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }
            ApiResponse response = responses.get(statusCode);
            if (response == null) {
                response = new ApiResponse();
                responses.addApiResponse(statusCode, response);
            }
            if (description != null) {
                response.setDescription(description);
            }
            if (response.getContent() == null) {
                response.setContent(new Content());
            }
            MediaType mediaType = response.getContent().get("application/json");
            if (mediaType == null) {
                mediaType = new MediaType();
                response.getContent().addMediaType("application/json", mediaType);
            }
            mediaType.setExample(parsed);
        } catch (Exception ignored) {
        }
    }

    private void addErrorResponse(Operation operation, String statusCode, String title, String detail, String path) {
        try {
            String errorJson = String.format("""
                {
                  "type": "https://api.equadras.com/erros/%s",
                  "title": "%s",
                  "status": %s,
                  "detail": "%s",
                  "instance": "%s"
                }
                """, statusCode.equals("400") ? "bad-request" : statusCode.equals("404") ? "not-found" : "conflito-horario", title, statusCode, detail, path);

            JsonNode parsed = objectMapper.readTree(errorJson);
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }
            ApiResponse response = new ApiResponse().description(title);
            MediaType mediaType = new MediaType().example(parsed);
            response.setContent(new Content().addMediaType("application/json", mediaType));
            responses.addApiResponse(statusCode, response);
        } catch (Exception ignored) {
        }
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
