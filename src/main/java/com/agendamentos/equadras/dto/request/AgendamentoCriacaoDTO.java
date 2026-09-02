package com.agendamentos.equadras.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AgendamentoCriacaoDTO(
        Long usuarioId,

        @NotNull(message = "O ID da quadra é obrigatório")
        Long quadraId,

        @NotNull(message = "A data e hora de início é obrigatória")
        @Future(message = "A data de início deve estar no futuro")
        LocalDateTime dataHoraInicio,

        @NotNull(message = "A data e hora de fim é obrigatória")
        @Future(message = "A data de fim deve estar no futuro")
        LocalDateTime dataHoraFim
) {}