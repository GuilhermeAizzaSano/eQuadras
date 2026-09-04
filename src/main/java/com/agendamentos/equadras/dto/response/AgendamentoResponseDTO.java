package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Dados completos de retorno de um agendamento esportivo")
public record AgendamentoResponseDTO(
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

        @Schema(description = "Status do agendamento (PENDENTE, CONFIRMADO, CANCELADO)", example = "CONFIRMADO")
        StatusAgendamento status,

        @Schema(description = "Identificador da transação no gateway de pagamento", example = "mp-pix-987654321")
        String transacaoPagamentoId,

        @Schema(description = "Chave Pix Copia e Cola emitida pelo gateway", example = "00020126580014br.gov.bcb.pix...")
        String pixCopiaECola,

        @Schema(description = "Imagem do QR Code Pix codificada em Base64", example = "iVBORw0KGgoAAAANSUhEUgAAA...")
        String qrCodeBase64,

        @Schema(description = "Data e hora de criação do agendamento", example = "2026-09-04T15:30:00")
        LocalDateTime criadoEm
) {
    public static AgendamentoResponseDTO fromEntity(Agendamento agendamento) {
        return new AgendamentoResponseDTO(
                agendamento.getId_agendamento(),
                agendamento.getUsuario().getId_usuario(),
                agendamento.getUsuario().getNome_usuario(),
                agendamento.getUsuario().getPhone_usuario(),
                agendamento.getQuadra().getId_quadra(),
                agendamento.getQuadra().getNome(),
                agendamento.getDataHoraInicio(),
                agendamento.getDataHoraFim(),
                agendamento.getValorTotal(),
                agendamento.getStatus(),
                agendamento.getTransacaoPagamentoId(),
                agendamento.getPixCopiaECola(),
                agendamento.getQrCodeBase64(),
                agendamento.getCriadoEm()
        );
    }

    public static AgendamentoResponseDTO fromEntitySemPix(Agendamento agendamento) {
        return new AgendamentoResponseDTO(
                agendamento.getId_agendamento(),
                agendamento.getUsuario().getId_usuario(),
                agendamento.getUsuario().getNome_usuario(),
                agendamento.getUsuario().getPhone_usuario(),
                agendamento.getQuadra().getId_quadra(),
                agendamento.getQuadra().getNome(),
                agendamento.getDataHoraInicio(),
                agendamento.getDataHoraFim(),
                agendamento.getValorTotal(),
                agendamento.getStatus(),
                agendamento.getTransacaoPagamentoId(),
                null,
                null,
                agendamento.getCriadoEm()
        );
    }
}