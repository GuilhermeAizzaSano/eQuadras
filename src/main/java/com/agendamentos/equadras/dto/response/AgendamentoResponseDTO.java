package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.enums.StatusAgendamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AgendamentoResponseDTO(
        Long id_agendamento,
        Long usuarioId,
        String nomeUsuario,
        String telefoneUsuario,
        Long quadraId,
        String nomeQuadra,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        BigDecimal valorTotal,
        StatusAgendamento status,
        String transacaoPagamentoId,
        String pixCopiaECola,
        String qrCodeBase64,
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