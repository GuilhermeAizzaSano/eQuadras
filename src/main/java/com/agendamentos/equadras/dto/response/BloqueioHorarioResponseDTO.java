package com.agendamentos.equadras.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.agendamentos.equadras.model.entity.BloqueioHorario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record BloqueioHorarioResponseDTO(
        @Schema(description = "ID do bloqueio", example = "5")
        Long id,
        @Schema(description = "ID da quadra bloqueada", example = "1")
        Long quadraId,
        @Schema(description = "Data do bloqueio", example = "2026-10-12")
        LocalDate data,
        @Schema(description = "Início do bloqueio", example = "14:00:00")
        LocalTime horaInicio,
        @Schema(description = "Fim do bloqueio", example = "18:00:00")
        LocalTime horaFim,
        @Schema(description = "Motivo informado na criação", example = "Torneio Interno da Arena")
        String motivo,
        @Schema(description = "Data e hora de criação do bloqueio", example = "2026-09-29T09:30:00")
        LocalDateTime criadoEm
) {
    public static BloqueioHorarioResponseDTO fromEntity(BloqueioHorario b) {
        return new BloqueioHorarioResponseDTO(
                b.getId(),
                b.getQuadra() != null ? b.getQuadra().getId_quadra() : null,
                b.getData(),
                b.getHoraInicio(),
                b.getHoraFim(),
                b.getMotivo(),
                b.getCriadoEm()
        );
    }
}
