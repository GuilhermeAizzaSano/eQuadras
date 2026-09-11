package com.agendamentos.equadras.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record BloqueioHorarioCriacaoDTO(
        @NotNull(message = "A data do bloqueio é obrigatória")
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        @Size(max = 255, message = "O motivo deve ter no máximo 255 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "Caracteres HTML não são permitidos no motivo")
        String motivo,
        Boolean substituirDiaInteiro
) {
    public BloqueioHorarioCriacaoDTO(LocalDate data, LocalTime horaInicio, LocalTime horaFim, String motivo) {
        this(data, horaInicio, horaFim, motivo, false);
    }
}
