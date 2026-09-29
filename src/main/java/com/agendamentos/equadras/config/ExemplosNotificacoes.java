package com.agendamentos.equadras.config;

import com.agendamentos.equadras.config.ExemplosDaApi.Erros;
import com.agendamentos.equadras.config.ExemplosDaApi.ExemploOperacao;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.agendamentos.equadras.config.ExemplosDaApi.chave;
import static com.agendamentos.equadras.config.ExemplosDaApi.operacao;

final class ExemplosNotificacoes {

    private ExemplosNotificacoes() {
    }

    static Map<String, ExemploOperacao> exemplos() {
        Map<String, ExemploOperacao> m = new LinkedHashMap<>();

        m.put(chave("GET", "/api/notificacoes/stream"), operacao()
                .stream("200", "Conexão SSE aberta (timeout de 1 hora). Cada notificação chega como evento `notificacao`.", """
                    event: notificacao
                    data: {"id":1,"mensagem":"Nova reserva confirmada: Arthur Prado em Arena Gol Society às 19:00.","lida":false,"dataCriacao":"2026-09-29T15:31:00"}

                    """)
                .erro(Erros.papelInsuficiente())
                .build());

        m.put(chave("GET", "/api/notificacoes/admin"), operacao()
                .ok("200", "Página de notificações do administrador (as excluídas não voltam)", """
                    {
                      "content": [
                        {
                          "id": 1,
                          "mensagem": "Nova reserva confirmada: Arthur Prado em Arena Gol Society às 19:00.",
                          "lida": false,
                          "excluida": false,
                          "dataCriacao": "2026-09-29T15:31:00"
                        }
                      ],
                      "pageable": {"pageNumber": 0, "pageSize": 5, "sort": {"empty": true, "sorted": false, "unsorted": true}, "offset": 0, "paged": true, "unpaged": false},
                      "last": true,
                      "totalPages": 1,
                      "totalElements": 1,
                      "size": 5,
                      "number": 0,
                      "sort": {"empty": true, "sorted": false, "unsorted": true},
                      "first": true,
                      "numberOfElements": 1,
                      "empty": false
                    }
                    """)
                .erro(Erros.papelInsuficiente())
                .build());

        m.put(chave("PUT", "/api/notificacoes/{id}/ler"), operacao()
                .semCorpo("204", "Notificação marcada como lida")
                .erro(Erros.requisicaoInvalida("Você não tem permissão para alterar esta notificação."))
                .erro(Erros.papelInsuficiente())
                .build());

        m.put(chave("PUT", "/api/notificacoes/ler-todas"), operacao()
                .semCorpo("204", "Todas as notificações do administrador marcadas como lidas")
                .erro(Erros.papelInsuficiente())
                .build());

        m.put(chave("DELETE", "/api/notificacoes/todas"), operacao()
                .semCorpo("204", "Notificações do administrador excluídas (exclusão lógica)")
                .erro(Erros.papelInsuficiente())
                .build());

        return m;
    }
}
