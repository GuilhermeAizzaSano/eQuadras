package com.agendamentos.equadras.event;

import com.agendamentos.equadras.model.enums.TipoExecutor;

/**
 * Evento de domínio disparado quando um agendamento é cancelado no sistema.
 * Desacoplado das entidades JPA para consumo assíncrono e auditoria resiliente.
 */
public record AgendamentoCanceladoEvent(
        AgendamentoNotificacaoPayload payload,
        Long executorId,
        String executorEmail,
        String executorNome,
        TipoExecutor tipoExecutor
) {}
