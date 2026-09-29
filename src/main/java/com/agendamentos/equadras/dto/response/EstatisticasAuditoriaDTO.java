package com.agendamentos.equadras.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record EstatisticasAuditoriaDTO(
        @Schema(description = "Logins bem-sucedidos hoje", example = "42")
        long totalLoginsHoje,
        @Schema(description = "Tentativas de login com falha hoje", example = "3")
        long totalFalhasLoginHoje,
        @Schema(description = "Total de ações registradas hoje", example = "180")
        long totalAcoesHoje,
        @Schema(description = "Cancelamentos de reserva hoje", example = "2")
        long totalCancelamentosHoje
) {
}
