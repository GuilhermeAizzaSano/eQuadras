package com.agendamentos.equadras.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para criação simplificada de reserva via Bot / WhatsApp")
public record AgendamentoBotRequestDTO(
        @Schema(description = "ID numérico da quadra (opcional se nomeQuadra ou tipoEsporte informado)", example = "1")
        Long quadraId,

        @Schema(description = "Nome ou parte do nome da quadra (opcional)", example = "Quadra Society Principal")
        String nomeQuadra,

        @Schema(description = "Tipo de esporte da quadra (opcional)", example = "SOCIETY")
        String tipoEsporte,

        @NotBlank(message = "A data da reserva é obrigatória")
        @Schema(description = "Data da reserva (ex: '2026-09-05', '15/09', 'amanha', 'sexta')", example = "2026-09-05")
        String data,

        @NotBlank(message = "O horário de início é obrigatório")
        @Schema(description = "Horário de início (ex: '19:00', '19h', '19')", example = "19:00")
        String horaInicio,

        @Schema(description = "Horário de término (opcional, padrão: início + 1h)", example = "20:00")
        String horaFim,

        @NotBlank(message = "O nome do cliente é obrigatório")
        @Schema(description = "Nome completo do cliente", example = "Arthur Prado")
        String nomeCliente,

        @NotBlank(message = "O telefone do cliente é obrigatório")
        @Schema(description = "Telefone ou WhatsApp do cliente com DDD", example = "11999998888")
        String telefoneCliente
) {
}
