package com.agendamentos.equadras.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class OpenApiDocumentacaoTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> METODOS = List.of("get", "post", "put", "patch", "delete");
    private static final Set<String> ERROS_DO_HANDLER = Set.of("400", "401", "403", "404", "409", "413", "415", "422", "429", "500");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private tools.jackson.databind.ObjectMapper mapperDaAplicacao;

    private JsonNode docs;

    private record Operacao(String metodo, String path, JsonNode corpo) {
        String nome() {
            return metodo.toUpperCase() + " " + path;
        }
    }

    @BeforeEach
    void carregarDocumento() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        docs = JSON.readTree(json);
    }

    private List<Operacao> operacoesDaApi() {
        List<Operacao> lista = new ArrayList<>();
        Iterator<String> paths = docs.get("paths").fieldNames();
        while (paths.hasNext()) {
            String path = paths.next();
            if (!path.startsWith("/api")) {
                continue;
            }
            for (String metodo : METODOS) {
                JsonNode op = docs.get("paths").get(path).get(metodo);
                if (op != null) {
                    lista.add(new Operacao(metodo, path, op));
                }
            }
        }
        return lista;
    }

    private static boolean vazio(JsonNode no) {
        return no == null || no.asText("").isBlank();
    }

    /** Exemplos declarados em um media type: `example` ou cada `examples.*.value`. */
    private static List<JsonNode> exemplosDe(JsonNode media) {
        List<JsonNode> exemplos = new ArrayList<>();
        if (media.has("example")) {
            exemplos.add(media.get("example"));
        }
        if (media.has("examples")) {
            media.get("examples").elements().forEachRemaining(e -> exemplos.add(e.get("value")));
        }
        return exemplos;
    }

    private static Iterable<Map.Entry<String, JsonNode>> campos(JsonNode no) {
        List<Map.Entry<String, JsonNode>> lista = new ArrayList<>();
        no.fieldNames().forEachRemaining(n -> lista.add(Map.entry(n, no.get(n))));
        return lista;
    }

    // Critério 1: summary, description e exemplo de resposta 2xx (exceto 204)
    @Test
    void todaOperacaoDaApiTemSummaryDescriptionEExemploDeSucesso() {
        List<String> problemas = new ArrayList<>();
        for (Operacao op : operacoesDaApi()) {
            if (vazio(op.corpo().get("summary"))) {
                problemas.add(op.nome() + ": sem summary");
            }
            if (vazio(op.corpo().get("description"))) {
                problemas.add(op.nome() + ": sem description");
            }
            for (Map.Entry<String, JsonNode> resposta : campos(op.corpo().get("responses"))) {
                String codigo = resposta.getKey();
                if (!codigo.startsWith("2") || codigo.equals("204")) {
                    continue;
                }
                JsonNode conteudo = resposta.getValue().get("content");
                if (conteudo == null || conteudo.isEmpty()) {
                    problemas.add(op.nome() + ": resposta " + codigo + " sem conteúdo (resposta automática espúria?)");
                    continue;
                }
                for (Map.Entry<String, JsonNode> media : campos(conteudo)) {
                    if (exemplosDe(media.getValue()).isEmpty()) {
                        problemas.add(op.nome() + ": resposta " + codigo + " (" + media.getKey() + ") sem exemplo");
                    }
                }
            }
        }
        assertTrue(problemas.isEmpty(), String.join("\n", problemas));
    }

    // Critério 2: nenhuma chave de exemplo fora do schema (e, em 2xx, nenhum campo do schema faltando)
    @Test
    void exemplosRespeitamOsSchemasReferenciados() {
        OpenApiExemploValidador validador = new OpenApiExemploValidador(docs);
        List<String> problemas = new ArrayList<>();
        for (Operacao op : operacoesDaApi()) {
            JsonNode requestJson = op.corpo().path("requestBody").path("content").path("application/json");
            if (!requestJson.isMissingNode() && requestJson.has("schema")) {
                for (JsonNode exemplo : exemplosDe(requestJson)) {
                    validador.validar(requestJson.get("schema"), exemplo, false, "$")
                            .forEach(p -> problemas.add(op.nome() + " [request]: " + p));
                }
            }
            for (Map.Entry<String, JsonNode> resposta : campos(op.corpo().get("responses"))) {
                boolean sucesso = resposta.getKey().startsWith("2");
                for (Map.Entry<String, JsonNode> media : campos(resposta.getValue().path("content"))) {
                    // text/event-stream traz exemplo em texto, não comparável a schema JSON
                    if (!media.getKey().contains("json") || !media.getValue().has("schema")) {
                        continue;
                    }
                    for (JsonNode exemplo : exemplosDe(media.getValue())) {
                        validador.validar(media.getValue().get("schema"), exemplo, sucesso, "$")
                                .forEach(p -> problemas.add(op.nome() + " [" + resposta.getKey() + "]: " + p));
                    }
                }
            }
        }
        assertTrue(problemas.isEmpty(), String.join("\n", problemas));
    }

    // Critério 3: GET /api/quadras documenta as variantes
    @Test
    void listagemDeQuadrasDocumentaParametrosEVariantes() {
        JsonNode op = docs.path("paths").path("/api/quadras").path("get");
        assertTrue(op.has("parameters"), "GET /api/quadras sem parâmetros");
        Map<String, String> descricoes = new java.util.HashMap<>();
        for (JsonNode p : op.get("parameters")) {
            descricoes.put(p.get("name").asText(), p.path("description").asText(""));
        }
        for (String nome : List.of("resumido", "page", "size", "X-Client", "X-View")) {
            assertTrue(descricoes.containsKey(nome), "parâmetro ausente: " + nome);
            assertTrue(!descricoes.get(nome).isBlank(), "parâmetro sem descrição: " + nome);
        }
        assertTrue(!descricoes.containsKey("Origin"), "Origin não é usado e deve sair do contrato");

        String texto = (op.path("description").asText("") + " " + String.join(" ", descricoes.values())).toLowerCase();
        assertTrue(texto.contains("resumido"), "descrição não cita o formato resumido");
        assertTrue(texto.contains("completo"), "descrição não cita o formato completo");
        assertTrue(texto.contains("ignorad"), "descrição não diz que page/size são ignorados no resumido");
        assertTrue(texto.contains("200"), "descrição não cita o teto de 200 itens");

        JsonNode exemplos = op.path("responses").path("200").path("content").path("application/json").path("examples");
        for (String nome : List.of("resumido", "completoPaginado", "completoLista")) {
            assertTrue(exemplos.has(nome), "exemplo nomeado ausente: " + nome);
        }
    }

    // Todo parâmetro (query, path, header) descrito
    @Test
    void todoParametroDaApiTemDescricao() {
        List<String> problemas = new ArrayList<>();
        for (Operacao op : operacoesDaApi()) {
            for (JsonNode p : op.corpo().path("parameters")) {
                if (vazio(p.get("description"))) {
                    problemas.add(op.nome() + ": parâmetro '" + p.path("name").asText() + "' (" + p.path("in").asText() + ") sem descrição");
                }
            }
        }
        assertTrue(problemas.isEmpty(), String.join("\n", problemas));
    }

    // Erros declarados só com status que o GlobalExceptionHandler/filtros realmente produzem
    @Test
    void errosDeclaradosPertencemAoConjuntoDoHandler() {
        List<String> problemas = new ArrayList<>();
        for (Operacao op : operacoesDaApi()) {
            for (Map.Entry<String, JsonNode> resposta : campos(op.corpo().get("responses"))) {
                String codigo = resposta.getKey();
                boolean erro = codigo.startsWith("4") || codigo.startsWith("5") || codigo.equals("default");
                if (erro && !ERROS_DO_HANDLER.contains(codigo)) {
                    problemas.add(op.nome() + ": status de erro não produzido pelo handler: " + codigo);
                }
            }
        }
        assertTrue(problemas.isEmpty(), String.join("\n", problemas));
    }

    // Login/logout: cookie de sessão; rotas públicas sem exigência de segurança
    @Test
    void loginELogoutDocumentamCookieERotasPublicasNaoExigemCredencial() {
        assertTrue(docs.path("paths").path("/api/usuarios/login").path("post").path("responses").path("200").path("headers").has("Set-Cookie"));
        assertTrue(docs.path("paths").path("/api/usuarios/logout").path("post").path("responses").path("204").path("headers").has("Set-Cookie"));
        for (String rota : List.of("/api/usuarios/login", "/api/usuarios/logout", "/api/agendamentos/bot", "/api/pagamentos/webhook")) {
            JsonNode seguranca = docs.path("paths").path(rota).path("post").path("security");
            assertTrue(seguranca.isArray() && seguranca.isEmpty(), rota + " deve declarar security: [] (rota pública)");
        }
    }

    // ResponseEntity<?>: o schema real precisa ser declarado, senão o exemplo não é validado
    @Test
    void loginERegeneracaoDeApiKeyReferenciamODtoRealEO429DaChaveEJsonSimples() {
        JsonNode login = docs.path("paths").path("/api/usuarios/login").path("post").path("responses");
        JsonNode chave = docs.path("paths").path("/api/usuarios/api-key/regenerar").path("post").path("responses");
        assertEquals("#/components/schemas/UsuarioResponseDTO",
                login.path("200").path("content").path("application/json").path("schema").path("$ref").asText());
        assertEquals("#/components/schemas/ApiKeyCriadaDTO",
                chave.path("200").path("content").path("application/json").path("schema").path("$ref").asText());
        // UsuarioController.regenerarApiKey devolve um Map, serializado como application/json
        assertTrue(chave.path("429").path("content").has("application/json"));
        assertTrue(!chave.path("429").path("content").has("application/problem+json"));
    }

    // Todo item do registro aponta para uma operação real (evita erro de digitação em path/método)
    @Test
    void todaChaveDoRegistroExisteNoDocumento() {
        Set<String> reais = new TreeSet<>();
        operacoesDaApi().forEach(op -> reais.add(op.nome()));
        List<String> orfas = new ArrayList<>();
        for (String chave : ExemplosDaApi.todos().keySet()) {
            if (!reais.contains(chave)) {
                orfas.add(chave);
            }
        }
        assertTrue(orfas.isEmpty(), "Chaves do registro sem operação: " + orfas);
    }

    // Os exemplos de Page nascem desta forma real de serialização (Spring Data no Boot 4.1.1)
    @Test
    void pageDoSpringDataSerializaComAsChavesDocumentadas() throws Exception {
        String json = mapperDaAplicacao.writeValueAsString(new PageImpl<>(List.of("a"), PageRequest.of(0, 5), 1));
        Set<String> chaves = new TreeSet<>();
        JSON.readTree(json).fieldNames().forEachRemaining(chaves::add);
        assertEquals(new TreeSet<>(Set.of("content", "pageable", "last", "totalPages", "totalElements", "size",
                "number", "sort", "first", "numberOfElements", "empty")), chaves);
    }

    // Fim do "some em silêncio": JSON de exemplo inválido derruba a geração do documento
    @Test
    void jsonDeExemploInvalidoFalhaExplicitamente() {
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> OpenApiConfig.lerJsonDeExemplo("{invalido", "GET /api/x"));
        assertTrue(erro.getMessage().contains("GET /api/x"));
    }
}
