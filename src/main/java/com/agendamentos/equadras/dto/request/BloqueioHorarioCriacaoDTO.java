package com.agendamentos.equadras.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record BloqueioHorarioCriacaoDTO(
        @NotNull(message = "A data do bloqueio é obrigatória")
        @Schema(description = "Data do bloqueio (yyyy-MM-dd). Não pode estar no passado", example = "2026-10-12")
        LocalDate data,
        @Schema(description = "Início do bloqueio (HH:mm:ss). Omita junto com horaFim para bloquear o dia inteiro", example = "14:00:00")
        LocalTime horaInicio,
        @Schema(description = "Fim do bloqueio (HH:mm:ss). Deve ser posterior a horaInicio; obrigatório quando horaInicio vier", example = "18:00:00")
        LocalTime horaFim,
        @Size(max = 255, message = "O motivo deve ter no máximo 255 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "Caracteres HTML não são permitidos no motivo")
        @Schema(description = "Motivo do bloqueio, até 255 caracteres, sem os símbolos < e >", example = "Torneio Interno da Arena")
        String motivo,
        @Schema(description = "true desbloqueia o restante de um dia já bloqueado por inteiro e mantém só este intervalo. Padrão false", example = "false")
        Boolean substituirDiaInteiro
) {
    public BloqueioHorarioCriacaoDTO(LocalDate data, LocalTime horaInicio, LocalTime horaFim, String motivo) {
        this(data, horaInicio, horaFim, motivo, false);
    }
}
