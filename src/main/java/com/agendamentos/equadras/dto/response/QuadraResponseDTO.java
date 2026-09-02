package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.enums.TipoEsporte;

import java.math.BigDecimal;

public record QuadraResponseDTO(
        Long id_quadra,
        String nome,
        TipoEsporte tipoEsporte,
        BigDecimal valorHora,
        boolean ativa,
        String cep,
        String logradouro,
        String bairro,
        String cidade,
        String estado,
        Double latitude,
        Double longitude,
        String descricao,
        java.util.List<String> fotos
) {
    public static QuadraResponseDTO fromEntity(Quadra quadra) {
        return new QuadraResponseDTO(
                quadra.getId_quadra(),
                quadra.getNome(),
                quadra.getTipoEsporte(),
                quadra.getValorHora(),
                quadra.isAtiva(),
                quadra.getCep(),
                quadra.getLogradouro(),
                quadra.getBairro(),
                quadra.getCidade(),
                quadra.getEstado(),
                quadra.getLatitude(),
                quadra.getLongitude(),
                quadra.getDescricao(),
                quadra.getFotos() != null ? quadra.getFotos() : java.util.Collections.emptyList()
        );
    }
}