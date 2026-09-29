package com.agendamentos.equadras.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.agendamentos.equadras.model.entity.Quadra;

import java.util.ArrayList;
import java.util.List;

public record QuadraFotosResponseDTO(
        @Schema(description = "ID da quadra", example = "1")
        Long id_quadra,
        @Schema(description = "Nome da quadra", example = "Arena Gol Society")
        String nome,
        @Schema(description = "Tipo de esporte da quadra", example = "FUTEBOL")
        String tipoEsporte,
        @Schema(description = "Cidade da quadra", example = "São José do Rio Preto")
        String cidade,
        @Schema(description = "Bairro da quadra", example = "Jardim das Flores")
        String bairro,
        @Schema(description = "URLs das fotos da galeria")
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
