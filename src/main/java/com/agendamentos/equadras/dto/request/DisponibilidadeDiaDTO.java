package com.agendamentos.equadras.dto.request;

import jakarta.validation.constraints.AssertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DisponibilidadeDiaDTO(
        DayOfWeek diaSemana,
        LocalTime horaInicio,
        LocalTime horaFim
) {
    @com.fasterxml.jackson.annotation.JsonIgnore
    @AssertTrue(message = "O horário de fim deve ser posterior ao de início")
    public boolean isIntervaloValido() {
        return horaInicio == null || horaFim == null || horaFim.isAfter(horaInicio);
    }
}
