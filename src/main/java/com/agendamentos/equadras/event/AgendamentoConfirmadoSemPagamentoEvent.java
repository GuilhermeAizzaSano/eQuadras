package com.agendamentos.equadras.event;

/**
 * Evento de domínio disparado quando uma reserva é criada já confirmada, sem etapa de pagamento.
 */
public record AgendamentoConfirmadoSemPagamentoEvent(
        AgendamentoNotificacaoPayload payload
) {}
