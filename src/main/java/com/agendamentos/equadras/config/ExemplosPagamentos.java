package com.agendamentos.equadras.config;

import com.agendamentos.equadras.config.ExemplosDaApi.Erros;
import com.agendamentos.equadras.config.ExemplosDaApi.ExemploOperacao;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.agendamentos.equadras.config.ExemplosDaApi.chave;
import static com.agendamentos.equadras.config.ExemplosDaApi.operacao;

final class ExemplosPagamentos {

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

    private ExemplosPagamentos() {
    }

    static Map<String, ExemploOperacao> exemplos() {
        Map<String, ExemploOperacao> m = new LinkedHashMap<>();

        m.put(chave("GET", "/api/pagamentos/{agendamentoId}/status"), operacao()
                .ok("200", "Agendamento com o status atual (`PENDENTE`, `CONFIRMADO` ou `CANCELADO`)", CONFIRMADO)
                .erro(Erros.naoEncontrado("AGENDAMENTO_NAO_ENCONTRADO", "Agendamento não encontrado. ID: 999"))
                .build());

        m.put(chave("POST", "/api/pagamentos/{agendamentoId}/simular-aprovacao"), operacao()
                .ok("200", "Agendamento confirmado", CONFIRMADO)
                .erro(Erros.naoEncontrado("AGENDAMENTO_NAO_ENCONTRADO", "Agendamento não encontrado. ID: 999"))
                .erro(Erros.regraNegocio("STATUS_INVALIDO", "Não é possível confirmar pagamento de um agendamento cancelado."))
                .erro(Erros.regraNegocio("ACESSO_NEGADO", "Você não tem permissão para confirmar o pagamento deste agendamento."))
                .erro(Erros.conflito("CONFLITO_STATUS", "Não foi possível confirmar o agendamento pois ele foi expirado ou cancelado concorrentemente."))
                .build());

        m.put(chave("POST", "/api/pagamentos/webhook"), operacao()
                .okNomeado("200", "ignored", "Notificação sem ID de pagamento relevante: ignorada.",
                        "{\"status\": \"ignored\"}", null)
                .okNomeado("200", "received", "Pagamento consultado, mas ainda não aprovado (ou não encontrado no gateway).",
                        "{\"status\": \"received\"}", null)
                .okNomeado("200", "processed", "Pagamento aprovado e agendamento confirmado.",
                        "{\"status\": \"processed\", \"payment_status\": \"approved\"}", null)
                .okNomeado("500", "error", "Falha ao confirmar ou consultar o pagamento: o gateway deve reenviar.",
                        "{\"status\": \"error\", \"message\": \"Falha ao processar confirmação de pagamento. Solicitando retentativa.\"}", null)
                .build());

        return m;
    }
}
