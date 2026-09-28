package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.model.entity.Notificacao;

import java.time.LocalDateTime;

public record NotificacaoResponseDTO(
        Long id,
        String mensagem,
        boolean lida,
        boolean excluida,
        LocalDateTime dataCriacao
) {
    public static NotificacaoResponseDTO fromEntity(Notificacao n) {
        return new NotificacaoResponseDTO(
                n.getId(),
                n.getMensagem(),
                n.isLida(),
                n.isExcluida(),
                n.getDataCriacao());
    }
}
