package com.agendamentos.equadras.dto.response;

public record EstatisticasAuditoriaDTO(
        long totalLoginsHoje,
        long totalFalhasLoginHoje,
        long totalAcoesHoje,
        long totalCancelamentosHoje
) {
}
