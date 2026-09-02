package com.agendamentos.equadras.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record BloqueioHorarioCriacaoDTO(
        @NotNull(message = "A data do bloqueio é obrigatória")
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        String motivo,
        Boolean substituirDiaInteiro
) {
    public BloqueioHorarioCriacaoDTO(LocalDate data, LocalTime horaInicio, LocalTime horaFim, String motivo) {
        this(data, horaInicio, horaFim, motivo, false);
    }
}
