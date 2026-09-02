package com.agendamentos.equadras.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Dados para desbloquear horários de uma quadra via API")
public record DesbloqueioHorarioDTO(
        @Schema(description = "ID específico do bloqueio cadastrado (opcional se informada a data)", example = "1")
        Long bloqueioId,

        @Schema(description = "Data do bloqueio a ser removido (obrigatório se bloqueioId não informado)", example = "2026-09-05")
        LocalDate data,

        @Schema(description = "Hora de início do bloqueio (opcional)", example = "14:00:00")
        LocalTime horaInicio,

        @Schema(description = "Hora de término do bloqueio (opcional)", example = "16:00:00")
        LocalTime horaFim
) {}
