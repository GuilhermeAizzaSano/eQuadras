package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Resumo cadastral simplificado da quadra para listagens e bots")
public record QuadraResumoResponseDTO(
        @Schema(description = "Identificador único da quadra", example = "1")
        Long id_quadra,

        @Schema(description = "Nome da quadra esportiva", example = "Arena Gol Society")
        String nome,

        @Schema(description = "Tipo de esporte praticado na quadra", example = "FUTEBOL")
        TipoEsporte tipoEsporte,

        @Schema(description = "Valor cobrado por hora de locação", example = "120.00")
        BigDecimal valorHora,

        @Schema(description = "Endereço textual formatado", example = "Rua das Palmeiras, Centro - São Paulo")
        String endereco,

        @Schema(description = "CEP da quadra", example = "15000-000")
        String cep
) {
    public static QuadraResumoResponseDTO fromEntity(Quadra quadra) {
        StringBuilder end = new StringBuilder();
        if (quadra.getLogradouro() != null && !quadra.getLogradouro().isBlank()) {
            end.append(quadra.getLogradouro());
        }
        if (quadra.getBairro() != null && !quadra.getBairro().isBlank()) {
            if (end.length() > 0) {
                end.append(", ");
            }
            end.append(quadra.getBairro());
        }
        if (quadra.getCidade() != null && !quadra.getCidade().isBlank()) {
            if (end.length() > 0) {
                end.append(" - ");
            }
            end.append(quadra.getCidade());
        }

        return new QuadraResumoResponseDTO(
                quadra.getId_quadra(),
                quadra.getNome(),
                quadra.getTipoEsporte(),
                quadra.getValorHora(),
                end.toString(),
                quadra.getCep()
        );
    }
}
