package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.model.entity.Quadra;

import java.util.ArrayList;
import java.util.List;

public record QuadraFotosResponseDTO(
        Long id_quadra,
        String nome,
        String tipoEsporte,
        String cidade,
        String bairro,
        List<String> fotos
) {
    public static QuadraFotosResponseDTO fromEntity(Quadra q) {
        return new QuadraFotosResponseDTO(
                q.getId_quadra(),
                q.getNome(),
                q.getTipoEsporte() != null ? q.getTipoEsporte().name() : null,
                q.getCidade() != null ? q.getCidade() : "",
                q.getBairro() != null ? q.getBairro() : "",
                q.getFotos() != null ? new ArrayList<>(q.getFotos()) : List.of());
    }
}
