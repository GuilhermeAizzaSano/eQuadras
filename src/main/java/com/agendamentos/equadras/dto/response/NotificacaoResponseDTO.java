package com.agendamentos.equadras.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.agendamentos.equadras.model.entity.Notificacao;

import java.time.LocalDateTime;

public record NotificacaoResponseDTO(
        @Schema(description = "ID da notificação", example = "1")
        Long id,
        @Schema(description = "Texto da notificação", example = "Nova reserva confirmada: Arthur Prado em Arena Gol Society às 19:00.")
        String mensagem,
        @Schema(description = "Indica se o administrador já leu a notificação", example = "false")
        boolean lida,
        @Schema(description = "Indica exclusão lógica; notificações excluídas não aparecem na listagem", example = "false")
        boolean excluida,
        @Schema(description = "Data e hora de criação", example = "2026-09-29T15:31:00")
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
