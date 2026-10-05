package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.model.enums.StatusAgendamento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// API externa (/api/**): a reserva nasce confirmada, então o retorno não carrega nenhum campo de pagamento
@Schema(description = "Reserva confirmada criada pela API externa, sem campos de pagamento")
public record ReservaConfirmadaResponseDTO(
        @Schema(description = "Identificador único do agendamento", example = "42")
        Long id_agendamento,

        @Schema(description = "ID do usuário que realizou a reserva", example = "10")
        Long usuarioId,

        @Schema(description = "Nome do atleta que reservou", example = "Arthur Prado")
        String nomeUsuario,

        @Schema(description = "Telefone de contato do atleta", example = "(11) 99999-8888")
        String telefoneUsuario,

        @Schema(description = "ID da quadra reservada", example = "1")
        Long quadraId,

        @Schema(description = "Nome da quadra esportiva", example = "Arena Gol Society")
        String nomeQuadra,

        @Schema(description = "Data e horário de início da partida", example = "2026-09-05T19:00:00")
        LocalDateTime dataHoraInicio,

        @Schema(description = "Data e horário de término da partida", example = "2026-09-05T20:00:00")
        LocalDateTime dataHoraFim,

        @Schema(description = "Valor total da reserva em reais", example = "120.00")
        BigDecimal valorTotal,

        @Schema(description = "Status do agendamento (sempre CONFIRMADO na criação)", example = "CONFIRMADO")
        StatusAgendamento status,

        @Schema(description = "Data e hora de criação do agendamento", example = "2026-09-04T15:30:00")
        LocalDateTime criadoEm,

        @Schema(description = "Data e hora de cancelamento do agendamento, se cancelado", example = "2026-09-04T16:00:00")
        LocalDateTime canceladoEm
) {
    public static ReservaConfirmadaResponseDTO de(AgendamentoResponseDTO agendamento) {
        return new ReservaConfirmadaResponseDTO(
                agendamento.id_agendamento(),
                agendamento.usuarioId(),
                agendamento.nomeUsuario(),
                agendamento.telefoneUsuario(),
                agendamento.quadraId(),
                agendamento.nomeQuadra(),
                agendamento.dataHoraInicio(),
                agendamento.dataHoraFim(),
                agendamento.valorTotal(),
                agendamento.status(),
                agendamento.criadoEm(),
                agendamento.canceladoEm()
        );
    }
}
