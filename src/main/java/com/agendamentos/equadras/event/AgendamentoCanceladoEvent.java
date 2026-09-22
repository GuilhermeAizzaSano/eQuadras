package com.agendamentos.equadras.event;

import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Usuario;

/**
 * Evento de domínio disparado quando um agendamento é cancelado no sistema.
 */
public record AgendamentoCanceladoEvent(
        Agendamento agendamento,
        Usuario executor
) {}
