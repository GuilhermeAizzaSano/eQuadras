package com.agendamentos.equadras.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalTime;

@IntervaloHorarioValido
public record DisponibilidadeDiaDTO(
        @Schema(description = "Dia da semana em inglês: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY ou SUNDAY. Não pode repetir dentro da mesma quadra", example = "MONDAY")
        DayOfWeek diaSemana,
        @Schema(description = "Hora de abertura (HH:mm:ss)", example = "08:00:00")
        LocalTime horaInicio,
        @Schema(description = "Hora de fechamento (HH:mm:ss). Deve ser posterior a horaInicio", example = "22:00:00")
        LocalTime horaFim
) {
}
