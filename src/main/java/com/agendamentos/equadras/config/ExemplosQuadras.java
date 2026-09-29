package com.agendamentos.equadras.config;

import com.agendamentos.equadras.config.ExemplosDaApi.Erros;
import com.agendamentos.equadras.config.ExemplosDaApi.ExemploOperacao;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.agendamentos.equadras.config.ExemplosDaApi.chave;
import static com.agendamentos.equadras.config.ExemplosDaApi.operacao;

final class ExemplosQuadras {

    private static final String QUADRA = """
        {
          "id_quadra": 1,
          "nome": "Arena Gol Society",
          "tipoEsporte": "FUTEBOL",
          "valorHora": 120.00,
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
          "fotos": ["/uploads/quadras/3f2a9c1e-7b4d-4e8a-9c21-5d6f7e8a9b0c.jpg"],
          "disponibilidades": [
            {"diaSemana": "MONDAY", "horaInicio": "08:00:00", "horaFim": "22:00:00"}
          ],
          "versao": 3
        }
        """;

    private static final String QUADRA_SEM_FOTOS = QUADRA.replace("[\"/uploads/quadras/3f2a9c1e-7b4d-4e8a-9c21-5d6f7e8a9b0c.jpg\"]", "[]");

    private static final String QUADRA_INATIVA = QUADRA.replace("\"ativa\": true", "\"ativa\": false");

    private static final String RESUMO = """
        {
          "id_quadra": 1,
          "nome": "Arena Gol Society",
          "tipoEsporte": "FUTEBOL",
          "valorHora": 120.00,
          "endereco": "Av. Brasil, 1500, Jardim das Flores - São José do Rio Preto",
          "cep": "15000-000"
        }
        """;

    private static final String FOTOS = """
        {
          "id_quadra": 1,
          "nome": "Arena Gol Society",
          "tipoEsporte": "FUTEBOL",
          "cidade": "São José do Rio Preto",
          "bairro": "Jardim das Flores",
          "fotos": ["/uploads/quadras/3f2a9c1e-7b4d-4e8a-9c21-5d6f7e8a9b0c.jpg"]
        }
        """;

    private static final String CRIACAO = """
        {
          "nome": "Arena Gol Society",
          "tipoEsporte": "FUTEBOL",
          "valorHora": 120.00,
          "cep": "15000-000",
          "logradouro": "Av. Brasil, 1500",
          "bairro": "Jardim das Flores",
          "cidade": "São José do Rio Preto",
          "estado": "SP",
          "latitude": -20.8113,
          "longitude": -49.3758,
          "descricao": "Grama sintética padrão FIFA com iluminação em LED e vestiários.",
          "dataLimiteAgendamento": "2026-12-31",
          "fotos": [],
          "disponibilidades": [
            {"diaSemana": "MONDAY", "horaInicio": "08:00:00", "horaFim": "22:00:00"}
          ]
        }
        """;

    private static final String EDICAO = """
        {
          "nome": "Arena Gol Society",
          "tipoEsporte": "FUTEBOL",
          "valorHora": 130.00,
          "cep": "15000-000",
          "logradouro": "Av. Brasil, 1500",
          "bairro": "Jardim das Flores",
          "cidade": "São José do Rio Preto",
          "estado": "SP",
          "latitude": -20.8113,
          "longitude": -49.3758,
          "descricao": "Grama sintética padrão FIFA com iluminação em LED e vestiários.",
          "dataLimiteAgendamento": "2026-12-31",
          "fotos": ["/uploads/quadras/3f2a9c1e-7b4d-4e8a-9c21-5d6f7e8a9b0c.jpg"],
          "disponibilidades": [
            {"diaSemana": "MONDAY", "horaInicio": "08:00:00", "horaFim": "22:00:00"}
          ],
          "versao": 3
        }
        """;

    private static final String PAGINA_DE_QUADRAS = """
        {
          "content": [%s],
          "pageable": {"pageNumber": 0, "pageSize": 6, "sort": {"empty": false, "sorted": true, "unsorted": false}, "offset": 0, "paged": true, "unpaged": false},
          "last": true,
          "totalPages": 1,
          "totalElements": 1,
          "size": 6,
          "number": 0,
          "sort": {"empty": false, "sorted": true, "unsorted": false},
          "first": true,
          "numberOfElements": 1,
          "empty": false
        }
        """;

    private ExemplosQuadras() {
    }

    static Map<String, ExemploOperacao> exemplos() {
        Map<String, ExemploOperacao> m = new LinkedHashMap<>();

        m.put(chave("GET", "/api/quadras"), operacao()
                .okNomeado("200", "resumido", "Formato resumido: `array<QuadraResumoResponseDTO>`. É o padrão em /api/quadras. `page` e `size` são ignorados e a lista é limitada a 200 itens.",
                        "[" + RESUMO + "]", "array:QuadraResumoResponseDTO")
                .okNomeado("200", "completoPaginado", "Formato completo com `page`: `PageQuadraResponseDTO` (size padrão 6, máximo 50).",
                        PAGINA_DE_QUADRAS.formatted(QUADRA), "PageQuadraResponseDTO")
                .okNomeado("200", "completoLista", "Formato completo sem `page`: `array<QuadraResponseDTO>`, limitada a 200 itens sem aviso.",
                        "[" + QUADRA + "]", "array:QuadraResponseDTO")
                .erro(Erros.parametroInvalido("O parâmetro 'raioKm' possui um valor inválido: 'abc'."))
                .erro(Erros.naoAutenticado())
                .build());

        m.put(chave("POST", "/api/quadras"), operacao()
                .request(CRIACAO)
                .ok("201", "Quadra criada. Reenvio com o mesmo `Idempotency-Key` em até 10 minutos devolve a quadra já criada.", QUADRA)
                .erro(Erros.validacao("Dados inválidos: Nome: O nome deve ter entre 3 e 100 caracteres"))
                .erro(Erros.requisicaoInvalida("Uma quadra pode ter no máximo 5 fotos."))
                .erro(Erros.papelInsuficiente())
                .erro(Erros.naoAutenticado())
                .build());

        m.put(chave("GET", "/api/quadras/{id}"), operacao()
                .ok("200", "Quadra completa", QUADRA)
                .erro(Erros.requisicaoInvalida("A quadra informada não foi encontrada."))
                .erro(Erros.naoAutenticado())
                .build());

        m.put(chave("PUT", "/api/quadras/{id}"), operacao()
                .request(EDICAO)
                .ok("200", "Quadra atualizada (a `versao` incrementa)", QUADRA)
                .erro(Erros.regraNegocio("VERSAO_OBRIGATORIA", "Não foi possível salvar a quadra. Recarregue a página e tente de novo."))
                .erro(Erros.conflito("CONFLITO_VERSAO", "Esta quadra foi alterada por outra pessoa. Recarregue os dados e tente de novo."))
                .erro(Erros.validacao("Dados inválidos: Nome: O nome deve ter entre 3 e 100 caracteres"))
                .erro(Erros.acessoNegado("Apenas o administrador dono da quadra ou o Master Admin pode editá-la."))
                .erro(Erros.requisicaoInvalida("A quadra informada não foi encontrada."))
                .build());

        m.put(chave("DELETE", "/api/quadras/{id}"), operacao()
                .semCorpo("204", "Quadra excluída")
                .erro(Erros.operacaoNaoPermitida("Esta quadra não pode ser excluída porque possui agendamentos vinculados (histórico de reservas). Recomendamos inativar a quadra."))
                .erro(Erros.acessoNegado("Apenas o administrador dono da quadra ou o Master Admin pode excluí-la."))
                .erro(Erros.requisicaoInvalida("A quadra informada não foi encontrada."))
                .build());

        m.put(chave("PATCH", "/api/quadras/{id}/status"), operacao()
                .ok("200", "Quadra com o novo status", QUADRA_INATIVA)
                .erro(Erros.acessoNegado("Apenas o administrador dono da quadra ou o Master Admin pode alterar seu status."))
                .erro(Erros.requisicaoInvalida("A quadra informada não foi encontrada."))
                .erro(Erros.parametroAusente("O parâmetro obrigatório 'ativa' não foi informado."))
                .build());

        m.put(chave("POST", "/api/quadras/{id}/fotos"), operacao()
                .ok("200", "Quadra com a galeria atualizada", QUADRA)
                .erro(Erros.requisicaoInvalida("Limite de 5 fotos por quadra atingido. Remova fotos existentes antes de enviar novas."))
                .erro(Erros.arquivoGrande("O arquivo enviado excede o tamanho máximo permitido."))
                .erro(Erros.acessoNegado("Apenas o administrador dono da quadra ou o Master Admin pode fazer upload de fotos."))
                .build());

        m.put(chave("DELETE", "/api/quadras/{id}/fotos"), operacao()
                .ok("200", "Quadra com a galeria sem a foto removida", QUADRA_SEM_FOTOS)
                .erro(Erros.acessoNegado("Apenas o administrador dono da quadra ou o Master Admin pode remover fotos."))
                .erro(Erros.parametroAusente("O parâmetro obrigatório 'fotoUrl' não foi informado."))
                .build());

        for (String rota : new String[]{"/api/quadras/{id}/fotos", "/api/quadras/fotos"}) {
            m.put(chave("GET", rota), operacao()
                    .okNomeado("200", "quadraUnica", "Uma quadra: com `id` (path ou query) ou quando o filtro corresponde a exatamente uma quadra.",
                            FOTOS, "QuadraFotosResponseDTO")
                    .okNomeado("200", "lista", "Lista de quadras: sem filtro (todas, até 200) ou filtro com várias correspondências.",
                            "[" + FOTOS + "]", "array:QuadraFotosResponseDTO")
                    .okNomeado("200", "semResultado", "Filtro sem nenhuma quadra correspondente.",
                            "{\"fotos\": []}", "FotosSemResultado")
                    .erro(Erros.requisicaoInvalida("A quadra informada não foi encontrada."))
                    .build());
        }

        return m;
    }
}
