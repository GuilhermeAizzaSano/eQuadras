package com.agendamentos.equadras.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AgendamentoCriacaoDTO(
        @Schema(description = "ID do usuário (opcional para usuários autenticados via JWT)", example = "10")
        Long usuarioId,

        @NotNull(message = "O ID da quadra é obrigatório")
        @Schema(description = "Identificador único da quadra esportiva", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long quadraId,

        @NotNull(message = "A data e hora de início é obrigatória")
        @Future(message = "A data de início deve estar no futuro")
        @Schema(description = "Data e hora de início. Deve ser hora cheia (minutos zerados: HH:00:00).", example = "2026-09-12T19:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime dataHoraInicio,

        @NotNull(message = "A data e hora de fim é obrigatória")
        @Future(message = "A data de fim deve estar no futuro")
        @Schema(description = "Data e hora de término. Deve ser hora cheia (minutos zerados: HH:00:00). A duração mínima é de 1 hora e múltipla de 60 minutos.", example = "2026-09-12T20:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime dataHoraFim
) {}