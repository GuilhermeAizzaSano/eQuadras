package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.dto.request.DisponibilidadeDiaDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.enums.TipoEsporte;

import java.math.BigDecimal;
import java.util.List;

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
        java.time.LocalDate dataLimiteAgendamento,
        List<String> fotos,
        List<DisponibilidadeDiaDTO> disponibilidades
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
                quadra.getDataLimiteAgendamento(),
                quadra.getFotos() != null ? quadra.getFotos() : java.util.Collections.emptyList(),
                quadra.getDisponibilidades() != null
                        ? quadra.getDisponibilidades().stream()
                                .map(d -> new DisponibilidadeDiaDTO(d.getDiaSemana(), d.getHoraInicio(), d.getHoraFim()))
                                .toList()
                        : java.util.Collections.emptyList()
        );
    }
}