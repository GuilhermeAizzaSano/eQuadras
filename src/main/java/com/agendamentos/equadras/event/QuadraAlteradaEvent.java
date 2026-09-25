package com.agendamentos.equadras.event;

/**
 * Evento de domínio disparado quando uma quadra é criada, editada, excluída ou tem seu status alterado.
 * Desacoplado de entidades JPA para auditoria resiliente e execução assíncrona/AFTER_COMMIT.
 */
public record QuadraAlteradaEvent(
        Long quadraId,
        String nomeQuadra,
        String cidade,
        String estado,
        Long usuarioId,
        String acao,        // "CRIAR", "EDITAR", "EXCLUIR", "STATUS"
        String detalhes
) {}
