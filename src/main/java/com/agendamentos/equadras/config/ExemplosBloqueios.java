package com.agendamentos.equadras.config;

import com.agendamentos.equadras.config.ExemplosDaApi.Erros;
import com.agendamentos.equadras.config.ExemplosDaApi.ExemploOperacao;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.agendamentos.equadras.config.ExemplosDaApi.chave;
import static com.agendamentos.equadras.config.ExemplosDaApi.operacao;

final class ExemplosBloqueios {

    private static final String BLOQUEIO = """
        {
          "id": 5,
          "quadraId": 1,
          "data": "2026-10-12",
          "horaInicio": "14:00:00",
          "horaFim": "18:00:00",
          "motivo": "Torneio Interno da Arena",
          "criadoEm": "2026-09-29T09:30:00"
        }
        """;

    private ExemplosBloqueios() {
    }

    static Map<String, ExemploOperacao> exemplos() {
        Map<String, ExemploOperacao> m = new LinkedHashMap<>();

        m.put(chave("POST", "/api/quadras/{id}/bloqueios"), operacao()
                .request("""
                    {
                      "data": "2026-10-12",
                      "horaInicio": "14:00:00",
                      "horaFim": "18:00:00",
                      "motivo": "Torneio Interno da Arena",
                      "substituirDiaInteiro": false
                    }
                    """)
                .ok("201", "Bloqueio criado", BLOQUEIO)
                .erro(Erros.requisicaoInvalida("Já existe um bloqueio cadastrado que coincide com este horário nesta data."))
                .erro(Erros.requisicaoInvalida("Não é possível bloquear este horário pois já existem reservas ativas no período."))
                .erro(Erros.validacao("Dados inválidos: data: A data do bloqueio é obrigatória"))
                .erro(Erros.acessoNegado("Apenas o administrador dono da quadra ou o Master Admin pode criar bloqueios."))
                .build());

        m.put(chave("GET", "/api/quadras/bloqueios"), operacao()
                .ok("200", "Bloqueios de todas as quadras do administrador autenticado", "[" + BLOQUEIO + "]")
                .build());

        m.put(chave("GET", "/api/quadras/{id}/bloqueios"), operacao()
                .ok("200", "Bloqueios ativos e futuros da quadra", "[" + BLOQUEIO + "]")
                .erro(Erros.requisicaoInvalida("A quadra informada não foi encontrada."))
                .build());

        m.put(chave("DELETE", "/api/quadras/{quadraId}/bloqueios/{bloqueioId}"), operacao()
                .semCorpo("204", "Bloqueio removido")
                .erro(Erros.requisicaoInvalida("O bloqueio informado não pertence a esta quadra."))
                .erro(Erros.acessoNegado("Apenas o administrador dono da quadra ou o Master Admin pode remover bloqueios."))
                .build());

        m.put(chave("POST", "/api/quadras/{quadraId}/desbloquear"), operacao()
                .request("""
                    {
                      "bloqueioId": 5,
                      "data": "2026-10-12",
                      "horaInicio": "14:00:00",
                      "horaFim": "16:00:00"
                    }
                    """)
                .ok("200", "Resultado do desbloqueio", """
                    {
                      "mensagem": "Horários desbloqueados com sucesso.",
                      "totalRemovidos": 1
                    }
                    """)
                .erro(Erros.requisicaoInvalida("Informe o ID do bloqueio ou a data a ser desbloqueada."))
                .erro(Erros.acessoNegado("Apenas o administrador dono da quadra ou o Master Admin pode remover bloqueios."))
                .build());

        return m;
    }
}
