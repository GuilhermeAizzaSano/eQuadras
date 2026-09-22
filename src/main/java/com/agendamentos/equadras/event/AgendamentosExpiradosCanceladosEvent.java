package com.agendamentos.equadras.event;

/**
 * Evento de domínio disparado quando o job de limpeza cancela agendamentos expirados em lote.
 */
public record AgendamentosExpiradosCanceladosEvent(
        int quantidadeCancelados
) {}
