package com.agendamentos.equadras.dto.request;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DisponibilidadeDiaDTO(
        DayOfWeek diaSemana,
        LocalTime horaInicio,
        LocalTime horaFim
) {}
