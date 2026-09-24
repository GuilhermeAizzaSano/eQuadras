package com.agendamentos.equadras.event;

/**
 * Evento de domínio disparado quando um pagamento Pix é confirmado para um agendamento.
 */
public record AgendamentoPagamentoConfirmadoEvent(
        AgendamentoNotificacaoPayload payload
) {}
