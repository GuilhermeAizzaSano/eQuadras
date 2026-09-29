package com.agendamentos.equadras.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiExemploValidadorTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String RAIZ = """
        {"components":{"schemas":{
          "Quadra":{"type":"object","properties":{"id":{"type":"integer"},"nome":{"type":"string"}}},
          "Lista":{"type":"object","properties":{"itens":{"type":"array","items":{"$ref":"#/components/schemas/Quadra"}}}}
        }}}
        """;

    private OpenApiExemploValidador validador() throws Exception {
        return new OpenApiExemploValidador(JSON.readTree(RAIZ));
    }

    private JsonNode json(String texto) throws Exception {
        return JSON.readTree(texto);
    }

    @Test
    void aceitaExemploComTodosOsCampos() throws Exception {
        List<String> problemas = validador().validar(
                json("{\"$ref\":\"#/components/schemas/Quadra\"}"), json("{\"id\":1,\"nome\":\"A\"}"), true, "$");
        assertTrue(problemas.isEmpty(), problemas.toString());
    }

    @Test
    void rejeitaChaveInexistenteNoSchema() throws Exception {
        List<String> problemas = validador().validar(
                json("{\"$ref\":\"#/components/schemas/Quadra\"}"), json("{\"id\":1,\"nome\":\"A\",\"tipo\":\"X\"}"), false, "$");
        assertEquals(1, problemas.size());
        assertTrue(problemas.get(0).contains("$.tipo"));
    }

    @Test
    void modoEstritoExigeCampoDoSchema() throws Exception {
        List<String> problemas = validador().validar(
                json("{\"$ref\":\"#/components/schemas/Quadra\"}"), json("{\"id\":1}"), true, "$");
        assertEquals(1, problemas.size());
        assertTrue(problemas.get(0).contains("$.nome"));
    }

    @Test
    void modoNaoEstritoAceitaCampoAusente() throws Exception {
        List<String> problemas = validador().validar(
                json("{\"$ref\":\"#/components/schemas/Quadra\"}"), json("{\"id\":1}"), false, "$");
        assertTrue(problemas.isEmpty(), problemas.toString());
    }

    @Test
    void percorreArraysAninhados() throws Exception {
        List<String> problemas = validador().validar(
                json("{\"$ref\":\"#/components/schemas/Lista\"}"),
                json("{\"itens\":[{\"id\":1,\"nome\":\"A\"},{\"id\":2,\"nome\":\"B\",\"coberta\":true}]}"), true, "$");
        assertEquals(1, problemas.size());
        assertTrue(problemas.get(0).contains("$.itens[1].coberta"));
    }

    @Test
    void oneOfAceitaSeUmaAlternativaServe() throws Exception {
        JsonNode schema = json("""
            {"oneOf":[{"$ref":"#/components/schemas/Lista"},{"type":"array","items":{"$ref":"#/components/schemas/Quadra"}}]}
            """);
        assertTrue(validador().validar(schema, json("[{\"id\":1,\"nome\":\"A\"}]"), true, "$").isEmpty());
    }

    @Test
    void oneOfRejeitaSeNenhumaAlternativaServe() throws Exception {
        JsonNode schema = json("""
            {"oneOf":[{"$ref":"#/components/schemas/Lista"},{"type":"array","items":{"$ref":"#/components/schemas/Quadra"}}]}
            """);
        List<String> problemas = validador().validar(schema, json("[{\"id\":1,\"nome\":\"A\",\"tipo\":\"X\"}]"), true, "$");
        assertEquals(1, problemas.size());
        assertTrue(problemas.get(0).contains("oneOf"));
    }

    @Test
    void mapaLivreSemPropriedadesNaoRestringeChaves() throws Exception {
        JsonNode schema = json("{\"type\":\"object\",\"additionalProperties\":{\"type\":\"string\"}}");
        assertTrue(validador().validar(schema, json("{\"status\":\"ok\",\"qualquer\":\"coisa\"}"), true, "$").isEmpty());
    }

    @Test
    void rejeitaStringNoLugarDeNumero() throws Exception {
        List<String> problemas = validador().validar(
                json("{\"$ref\":\"#/components/schemas/Quadra\"}"), json("{\"id\":\"1\",\"nome\":\"A\"}"), false, "$");
        assertEquals(1, problemas.size());
        assertTrue(problemas.get(0).contains("$.id"));
    }

    @Test
    void rejeitaNumeroNoLugarDeString() throws Exception {
        List<String> problemas = validador().validar(
                json("{\"$ref\":\"#/components/schemas/Quadra\"}"), json("{\"id\":1,\"nome\":7}"), false, "$");
        assertEquals(1, problemas.size());
        assertTrue(problemas.get(0).contains("$.nome"));
    }

    @Test
    void rejeitaDecimalNoLugarDeInteiro() throws Exception {
        List<String> problemas = validador().validar(
                json("{\"$ref\":\"#/components/schemas/Quadra\"}"), json("{\"id\":1.5,\"nome\":\"A\"}"), false, "$");
        assertEquals(1, problemas.size());
        assertTrue(problemas.get(0).contains("$.id"));
    }

    @Test
    void rejeitaObjetoNoLugarDeLista() throws Exception {
        List<String> problemas = validador().validar(
                json("{\"$ref\":\"#/components/schemas/Lista\"}"), json("{\"itens\":{\"id\":1}}"), false, "$");
        assertEquals(1, problemas.size());
        assertTrue(problemas.get(0).contains("$.itens"));
    }

    @Test
    void aceitaInteiroEmCampoNumber() throws Exception {
        JsonNode schema = json("{\"type\":\"object\",\"properties\":{\"valor\":{\"type\":\"number\"}}}");
        assertTrue(validador().validar(schema, json("{\"valor\":120}"), true, "$").isEmpty());
    }
}
