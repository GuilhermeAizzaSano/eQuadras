package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.dto.request.DisponibilidadeDiaDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Detalhes cadastrais completos e horários de funcionamento de uma quadra")
public record QuadraResponseDTO(
        @Schema(description = "ID único da quadra", example = "1")
        Long id_quadra,

        @Schema(description = "Nome da quadra esportiva", example = "Arena Gol Society")
        String nome,

        @Schema(description = "Tipo de modalidade esportiva", example = "FUTEBOL")
        TipoEsporte tipoEsporte,

        @Schema(description = "Valor cobrado por hora de locação em reais", example = "120.00")
        BigDecimal valorHora,

        @Schema(description = "Status de ativação da quadra", example = "true")
        boolean ativa,

        @Schema(description = "CEP do local da quadra", example = "15000-000")
        String cep,

        @Schema(description = "Logradouro / Rua", example = "Av. Brasil, 1500")
        String logradouro,

        @Schema(description = "Bairro da quadra", example = "Jardim das Flores")
        String bairro,

        @Schema(description = "Cidade onde a quadra está localizada", example = "São José do Rio Preto")
        String cidade,

        @Schema(description = "UF do estado", example = "SP")
        String estado,

        @Schema(description = "Coordenada geográfica de latitude", example = "-20.8113")
        Double latitude,

        @Schema(description = "Coordenada geográfica de longitude", example = "-49.3758")
        Double longitude,

        @Schema(description = "Descrição detalhada da infraestrutura da quadra", example = "Grama sintética padrão FIFA com iluminação em LED e vestiários.")
        String descricao,

        @Schema(description = "Data limite máxima permitida para agendamentos futuros", example = "2026-12-31")
        java.time.LocalDate dataLimiteAgendamento,

        @Schema(description = "Lista de URLs das fotos da galeria da quadra")
        List<String> fotos,

        @Schema(description = "Horários de funcionamento semanais configurados para a quadra")
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