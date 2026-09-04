package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.enums.TipoEsporte;

import java.math.BigDecimal;

public record QuadraResumoResponseDTO(
        Long id_quadra,
        String nome,
        TipoEsporte tipoEsporte,
        BigDecimal valorHora,
        String endereco,
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
