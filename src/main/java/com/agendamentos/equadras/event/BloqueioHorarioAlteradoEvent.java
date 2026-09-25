package com.agendamentos.equadras.event;

/**
 * Evento de domínio disparado quando um bloqueio de horário é criado, removido ou desbloqueado.
 * Permite o desacoplamento da auditoria e persistência assíncrona/AFTER_COMMIT.
 */
public record BloqueioHorarioAlteradoEvent(
        Long usuarioId,
        String acao,        // "CRIAR", "EXCLUIR"
        String entidadeId,  // id do bloqueio ou id da quadra
        String detalhes
) {}
