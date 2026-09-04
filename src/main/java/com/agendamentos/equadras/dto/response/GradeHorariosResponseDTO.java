package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.model.enums.TipoEsporte;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Representação consolidada da grade de horários de uma quadra com contexto")
public record GradeHorariosResponseDTO(
        @Schema(description = "ID único da quadra", example = "1")
        Long id_quadra,

        @Schema(description = "Nome da quadra", example = "Quadra Society Principal")
        String nome_quadra,

        @Schema(description = "Tipo de esporte praticado na quadra", example = "FUTEBOL")
        TipoEsporte tipoEsporte,

        @Schema(description = "Valor cobrado por hora de locação", example = "120.00")
        BigDecimal valorHora,

        @Schema(description = "Data consultada", example = "2026-09-05")
        LocalDate data,

        @Schema(description = "Lista dos slots de horários do dia")
        List<HorarioDisponivelDTO> horarios
) {
}
