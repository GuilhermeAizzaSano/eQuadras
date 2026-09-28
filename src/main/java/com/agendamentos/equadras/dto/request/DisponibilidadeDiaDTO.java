package com.agendamentos.equadras.dto.request;

import java.time.DayOfWeek;
import java.time.LocalTime;

@IntervaloHorarioValido
public record DisponibilidadeDiaDTO(
        DayOfWeek diaSemana,
        LocalTime horaInicio,
        LocalTime horaFim
) {
}
