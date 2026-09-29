package com.agendamentos.equadras.config;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compara um exemplo JSON com o schema OpenAPI (já desserializado) que o descreve.
 * Chaves do exemplo que não existem no schema são sempre problema; no modo estrito,
 * campos do schema ausentes no exemplo também.
 */
final class OpenApiExemploValidador {

    private static final String PREFIXO_REF = "#/components/schemas/";

    private final JsonNode raiz;

    OpenApiExemploValidador(JsonNode raiz) {
        this.raiz = raiz;
    }

    List<String> validar(JsonNode schema, JsonNode exemplo, boolean exigirTodosOsCampos, String caminho) {
        List<String> problemas = new ArrayList<>();
        comparar(schema, exemplo, exigirTodosOsCampos, caminho, problemas);
        return problemas;
    }

    private void comparar(JsonNode schemaBruto, JsonNode exemplo, boolean estrito, String caminho, List<String> problemas) {
        JsonNode schema = resolver(schemaBruto);
        if (schema == null || exemplo == null || exemplo.isNull()) {
            return;
        }
        if (schema.has("oneOf")) {
            List<String> melhor = null;
            for (JsonNode alternativa : schema.get("oneOf")) {
                List<String> tentativa = new ArrayList<>();
                comparar(alternativa, exemplo, estrito, caminho, tentativa);
                if (tentativa.isEmpty()) {
                    return;
                }
                if (melhor == null || tentativa.size() < melhor.size()) {
                    melhor = tentativa;
                }
            }
            problemas.add(caminho + ": nenhuma alternativa de oneOf aceita o exemplo (" + String.join("; ", melhor) + ")");
            return;
        }
        if (exemplo.isArray()) {
            JsonNode itens = schema.get("items");
            if (itens == null) {
                if (schema.has("properties") || "object".equals(schema.path("type").asText())) {
                    problemas.add(caminho + ": o exemplo é uma lista, mas o schema descreve um objeto");
                }
                return;
            }
            for (int i = 0; i < exemplo.size(); i++) {
                comparar(itens, exemplo.get(i), estrito, caminho + "[" + i + "]", problemas);
            }
            return;
        }
        if (!exemplo.isObject()) {
            return;
        }
        Map<String, JsonNode> propriedades = propriedades(schema);
        if (propriedades.isEmpty()) {
            return;
        }
        Iterator<String> nomes = exemplo.fieldNames();
        while (nomes.hasNext()) {
            String nome = nomes.next();
            if (!propriedades.containsKey(nome)) {
                problemas.add(caminho + "." + nome + ": chave inexistente no schema");
            } else {
                comparar(propriedades.get(nome), exemplo.get(nome), estrito, caminho + "." + nome, problemas);
            }
        }
        if (estrito) {
            for (String nome : propriedades.keySet()) {
                if (!exemplo.has(nome)) {
                    problemas.add(caminho + "." + nome + ": campo do schema ausente no exemplo");
                }
            }
        }
    }

    private Map<String, JsonNode> propriedades(JsonNode schemaResolvido) {
        Map<String, JsonNode> resultado = new LinkedHashMap<>();
        JsonNode props = schemaResolvido.get("properties");
        if (props != null) {
            props.fieldNames().forEachRemaining(n -> resultado.put(n, props.get(n)));
        }
        JsonNode allOf = schemaResolvido.get("allOf");
        if (allOf != null) {
            for (JsonNode parte : allOf) {
                resultado.putAll(propriedades(resolver(parte)));
            }
        }
        return resultado;
    }

    private JsonNode resolver(JsonNode schema) {
        if (schema == null) {
            return null;
        }
        JsonNode ref = schema.get("$ref");
        if (ref == null) {
            return schema;
        }
        String valor = ref.asText();
        if (!valor.startsWith(PREFIXO_REF)) {
            throw new IllegalStateException("$ref não suportado pelo validador: " + valor);
        }
        JsonNode alvo = raiz.path("components").path("schemas").path(valor.substring(PREFIXO_REF.length()));
        if (alvo.isMissingNode()) {
            throw new IllegalStateException("$ref sem schema em components: " + valor);
        }
        return resolver(alvo);
    }
}
