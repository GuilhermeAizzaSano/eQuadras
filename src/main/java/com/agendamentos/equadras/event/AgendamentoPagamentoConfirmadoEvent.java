package com.agendamentos.equadras.event;

import com.agendamentos.equadras.model.entity.Agendamento;

/**
 * Evento de domínio disparado quando um pagamento Pix é confirmado para um agendamento.
 */
public record AgendamentoPagamentoConfirmadoEvent(
        Agendamento agendamento
) {}
