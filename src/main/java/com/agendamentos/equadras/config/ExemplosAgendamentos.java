package com.agendamentos.equadras.config;

import com.agendamentos.equadras.config.ExemplosDaApi.Erros;
import com.agendamentos.equadras.config.ExemplosDaApi.ExemploOperacao;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.agendamentos.equadras.config.ExemplosDaApi.chave;
import static com.agendamentos.equadras.config.ExemplosDaApi.operacao;

final class ExemplosAgendamentos {

    private static final String CONFIRMADO = """
        {
          "id_agendamento": 42,
          "usuarioId": 10,
          "nomeUsuario": "Arthur Prado",
          "telefoneUsuario": "(11) 99999-8888",
          "quadraId": 1,
          "nomeQuadra": "Arena Gol Society",
          "dataHoraInicio": "2026-10-12T19:00:00",
          "dataHoraFim": "2026-10-12T20:00:00",
          "valorTotal": 120.00,
          "status": "CONFIRMADO",
          "transacaoPagamentoId": "mp-pix-987654321",
          "pixCopiaECola": null,
          "qrCodeBase64": null,
          "criadoEm": "2026-09-29T15:30:00",
          "canceladoEm": null
        }
        """;

    private static final String RESERVA_CONFIRMADA = """
        {
          "id_agendamento": 42,
          "usuarioId": 10,
          "nomeUsuario": "Arthur Prado",
          "telefoneUsuario": "(11) 99999-8888",
          "quadraId": 1,
          "nomeQuadra": "Arena Gol Society",
          "dataHoraInicio": "2026-10-12T19:00:00",
          "dataHoraFim": "2026-10-12T20:00:00",
          "valorTotal": 120.00,
          "status": "CONFIRMADO",
          "criadoEm": "2026-09-29T15:30:00",
          "canceladoEm": null
        }
        """;

    private static final String CANCELADO = CONFIRMADO
            .replace("\"status\": \"CONFIRMADO\"", "\"status\": \"CANCELADO\"")
            .replace("\"canceladoEm\": null", "\"canceladoEm\": \"2026-09-30T08:00:00\"");

    private static final String PAGINA = """
        {
          "content": [%s],
          "page": 0,
          "size": 10,
          "totalElements": 1,
          "totalPages": 1
        }
        """;

    private static final String HORARIOS = """
        [
          {"inicio": "18:00:00", "fim": "19:00:00", "disponivel": true, "status": "DISPONIVEL", "motivo": "Disponível"},
          {"inicio": "19:00:00", "fim": "20:00:00", "disponivel": false, "status": "AGENDADO", "motivo": "Horário ocupado"},
          {"inicio": "20:00:00", "fim": "21:00:00", "disponivel": false, "status": "BLOQUEADO", "motivo": "Bloqueado: Torneio Interno da Arena"}
        ]
        """;

    private static final String GRADE = """
        [
          {
            "id_quadra": 1,
            "nome_quadra": "Arena Gol Society",
            "tipoEsporte": "FUTEBOL",
            "valorHora": 120.00,
            "data": "2026-10-12",
            "horarios": %s
          }
        ]
        """;

    private ExemplosAgendamentos() {
    }

    static Map<String, ExemploOperacao> exemplos() {
        Map<String, ExemploOperacao> m = new LinkedHashMap<>();

        m.put(chave("POST", "/api/agendamentos"), operacao()
                .request("""
                    {
                      "quadraId": 1,
                      "dataHoraInicio": "2026-10-12T19:00:00",
                      "dataHoraFim": "2026-10-12T20:00:00"
                    }
                    """)
                .okComCabecalhos("201", "Reserva criada já CONFIRMADA, sem cobrança Pix nem campos de pagamento", RESERVA_CONFIRMADA,
                        "ReservaConfirmadaResponseDTO", Map.of())
                .erro(Erros.validacao("Dados inválidos: Horário de início: A data de início deve estar no futuro"))
                .erro(Erros.regraNegocio("INTERVALO_INVALIDO", "A data/hora de término deve ser posterior à data/hora de início."))
                .erro(Erros.regraNegocio("HORARIO_PASSADO", "Não é possível realizar agendamentos em horários passados."))
                .erro(Erros.horarioIndisponivel())
                .build());

        m.put(chave("GET", "/api/agendamentos"), operacao()
                .ok("200", "Página de agendamentos do usuário (com `page`). Sem `page` o código devolve uma lista simples, limitada a 200 itens.",
                        PAGINA.formatted(CONFIRMADO))
                .erro(Erros.requisicaoInvalida("Aba de agendamento é obrigatória."))
                .erro(Erros.parametroInvalido("O parâmetro 'aba' possui um valor inválido: 'X'."))
                .build());

        m.put(chave("GET", "/api/agendamentos/contadores"), operacao()
                .ok("200", "Total de agendamentos do usuário por aba", """
                    {"ATIVOS": 3, "REALIZADOS": 12, "CANCELADOS": 1}
                    """)
                .build());

        m.put(chave("GET", "/api/agendamentos/{id}"), operacao()
                .ok("200", "Agendamento (Pix só aparece para o dono, enquanto PENDENTE)", CONFIRMADO)
                .erro(Erros.naoEncontrado("AGENDAMENTO_NAO_ENCONTRADO", "Agendamento não encontrado. ID: 999"))
                .build());

        m.put(chave("PATCH", "/api/agendamentos/{id}/cancelar"), operacao()
                .ok("200", "Agendamento cancelado", CANCELADO)
                .erro(Erros.naoEncontrado("AGENDAMENTO_NAO_ENCONTRADO", "Agendamento não encontrado para o ID: 999"))
                .erro(Erros.requisicaoInvalida("Este agendamento já está cancelado."))
                .erro(Erros.requisicaoInvalida("Não é possível cancelar um agendamento que está em andamento ou retroativo."))
                .erro(Erros.acessoNegado("Você não tem permissão para cancelar este agendamento."))
                .build());

        m.put(chave("GET", "/api/agendamentos/quadra/{quadraId}"), operacao()
                .ok("200", "Página de reservas da quadra (com `page`). Sem `page` o código devolve uma lista simples de até 200 itens.",
                        PAGINA.formatted(CONFIRMADO))
                .erro(Erros.requisicaoInvalida("A quadra informada não foi encontrada."))
                .erro(Erros.acessoNegado("Você não tem permissão para visualizar o histórico desta quadra."))
                .build());

        m.put(chave("GET", "/api/agendamentos/quadra/{quadraId}/contadores"), operacao()
                .ok("200", "Contadores das reservas da quadra", """
                    {"TODOS": 16, "ATIVOS": 3, "REALIZADOS": 12, "CANCELADOS": 1}
                    """)
                .erro(Erros.requisicaoInvalida("A quadra informada não foi encontrada."))
                .erro(Erros.acessoNegado("Você não tem permissão para visualizar o histórico desta quadra."))
                .build());

        m.put(chave("GET", "/api/agendamentos/quadra/{quadraId}/horarios-disponiveis"), operacao()
                .ok("200", "Grade de horários de 1 hora da quadra na data", HORARIOS)
                .erro(Erros.requisicaoInvalida("A quadra informada não foi encontrada."))
                .erro(Erros.parametroAusente("O parâmetro obrigatório 'data' não foi informado."))
                .erro(Erros.parametroInvalido("O parâmetro 'data' possui um valor inválido: 'amanha'."))
                .build());

        m.put(chave("GET", "/api/agendamentos/dia"), operacao()
                .ok("200", "Grade do dia por quadra do administrador (chave = ID da quadra)", "{\"1\": " + HORARIOS + "}")
                .erro(Erros.parametroAusente("O parâmetro obrigatório 'data' não foi informado."))
                .erro(Erros.papelInsuficiente())
                .build());

        m.put(chave("GET", "/api/agendamentos/agenda"), operacao()
                .ok("200", "Página da agenda das quadras do administrador", PAGINA.formatted(CONFIRMADO))
                .erro(Erros.requisicaoInvalida("Parâmetro 'data' ou intervalo ('inicio' e 'fim') é obrigatório."))
                .erro(Erros.requisicaoInvalida("O intervalo não pode ser superior a 24 horas."))
                .erro(Erros.papelInsuficiente())
                .build());

        m.put(chave("GET", "/api/agendamentos/agenda/contadores"), operacao()
                .ok("200", "Contadores da agenda por aba", """
                    {"ATIVOS": 3, "REALIZADOS": 12, "CANCELADOS": 1}
                    """)
                .erro(Erros.requisicaoInvalida("Parâmetro 'data' ou intervalo ('inicio' e 'fim') é obrigatório."))
                .erro(Erros.papelInsuficiente())
                .build());

        m.put(chave("GET", "/api/agendamentos/agenda/completa"), operacao()
                .ok("200", "Agendamentos não cancelados do período (até 24 h)", "[" + CONFIRMADO + "]")
                .erro(Erros.requisicaoInvalida("O intervalo não pode ser superior a 24 horas."))
                .erro(Erros.papelInsuficiente())
                .build());

        m.put(chave("GET", "/api/agendamentos/agenda/mensal"), operacao()
                .ok("200", "Agendamentos não cancelados do mês", "[" + CONFIRMADO + "]")
                .erro(Erros.requisicaoInvalida("Mês inválido: informe um valor entre 1 e 12."))
                .erro(Erros.requisicaoInvalida("Ano inválido: informe um valor entre 2000 e 2100."))
                .erro(Erros.papelInsuficiente())
                .build());

        m.put(chave("GET", "/api/agendamentos/dashboard/metricas"), operacao()
                .ok("200", "Métricas do dashboard do administrador", """
                    {
                      "totalQuadras": 4,
                      "quadrasAtivas": 3,
                      "totalReservas": 128,
                      "faturamentoTotal": 15420.00,
                      "reservasHoje": 6
                    }
                    """)
                .erro(Erros.papelInsuficiente())
                .build());

        m.put(chave("POST", "/api/agendamentos/bot"), operacao()
                .request("""
                    {
                      "quadraId": 1,
                      "nomeQuadra": "Arena Gol Society",
                      "tipoEsporte": "FUTEBOL",
                      "data": "amanha",
                      "horaInicio": "19h",
                      "horaFim": "20:00",
                      "nomeCliente": "Arthur Prado",
                      "telefoneCliente": "11999998888"
                    }
                    """)
                .okComCabecalhos("201", "Reserva criada já CONFIRMADA, sem cobrança Pix nem campos de pagamento; cliente criado ou vinculado pelo telefone",
                        RESERVA_CONFIRMADA, "ReservaConfirmadaResponseDTO", Map.of())
                .erro(Erros.validacao("Dados inválidos: data: A data da reserva é obrigatória"))
                .erro(Erros.requisicaoInvalida("Nenhuma quadra encontrada para o esporte ou nome informado."))
                .erro(Erros.requisicaoInvalida("Não foi possível entender a hora: 25h"))
                .erro(Erros.horarioIndisponivel())
                .build());

        m.put(chave("GET", "/api/agendamentos/horarios-disponiveis"), operacao()
                .ok("200", "Grade consolidada por quadra (lista vazia se nenhuma quadra corresponder aos filtros)", GRADE.formatted(HORARIOS))
                .erro(Erros.parametroInvalido("O parâmetro 'quadraId' possui um valor inválido: 'abc'."))
                .build());

        return m;
    }
}
