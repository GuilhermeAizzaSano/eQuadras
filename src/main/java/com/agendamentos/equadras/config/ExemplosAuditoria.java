package com.agendamentos.equadras.config;

import com.agendamentos.equadras.config.ExemplosDaApi.Erros;
import com.agendamentos.equadras.config.ExemplosDaApi.ExemploOperacao;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.agendamentos.equadras.config.ExemplosDaApi.chave;
import static com.agendamentos.equadras.config.ExemplosDaApi.operacao;

final class ExemplosAuditoria {

    private ExemplosAuditoria() {
    }

    static Map<String, ExemploOperacao> exemplos() {
        Map<String, ExemploOperacao> m = new LinkedHashMap<>();

        m.put(chave("GET", "/api/admin/auditoria"), operacao()
                .ok("200", "Página de logs de auditoria, do mais recente ao mais antigo", """
                    {
                      "content": [
                        {
                          "id": 981,
                          "usuarioId": 10,
                          "usuarioEmail": "arthur.prado@email.com",
                          "usuarioNome": "Arthur Prado",
                          "categoria": "AUTENTICACAO",
                          "acao": "LOGOUT",
                          "entidade": "USUARIO",
                          "recursoId": "10",
                          "tipoExecutor": "CLIENTE",
                          "detalhes": "Logout efetuado com sucesso.",
                          "ip": "203.0.113.10",
                          "userAgent": "Mozilla/5.0",
                          "criadoEm": "2026-09-29T15:40:00Z"
                        }
                      ],
                      "pageable": {"pageNumber": 0, "pageSize": 10, "sort": {"empty": false, "sorted": true, "unsorted": false}, "offset": 0, "paged": true, "unpaged": false},
                      "last": true,
                      "totalPages": 1,
                      "totalElements": 1,
                      "size": 10,
                      "number": 0,
                      "sort": {"empty": false, "sorted": true, "unsorted": false},
                      "first": true,
                      "numberOfElements": 1,
                      "empty": false
                    }
                    """)
                .erro(Erros.parametroInvalido("O parâmetro 'categoria' possui um valor inválido: 'X'."))
                .erro(Erros.acessoNegado("Acesso restrito ao Administrador Geral do sistema."))
                .build());

        m.put(chave("GET", "/api/admin/auditoria/estatisticas"), operacao()
                .ok("200", "Contadores de auditoria de hoje", """
                    {
                      "totalLoginsHoje": 42,
                      "totalFalhasLoginHoje": 3,
                      "totalAcoesHoje": 180,
                      "totalCancelamentosHoje": 2
                    }
                    """)
                .erro(Erros.acessoNegado("Acesso restrito ao Administrador Geral do sistema."))
                .build());

        return m;
    }
}
