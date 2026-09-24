package com.agendamentos.equadras.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Métricas agregadas do painel administrativo")
public record DashboardMetricasDTO(
        @Schema(description = "Total de quadras pertencentes ao administrador", example = "4")
        long totalQuadras,

        @Schema(description = "Total de quadras ativas do administrador", example = "3")
        long quadrasAtivas,

        @Schema(description = "Total histórico de reservas válidas (não canceladas)", example = "128")
        long totalReservas,

        @Schema(description = "Faturamento total acumulado em reservas confirmadas (pagas)", example = "15420.00")
        BigDecimal faturamentoTotal,

        @Schema(description = "Total de reservas agendadas para o dia atual", example = "6")
        long reservasHoje
) {}
