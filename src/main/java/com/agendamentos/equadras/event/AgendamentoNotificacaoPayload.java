package com.agendamentos.equadras.event;

import com.agendamentos.equadras.model.entity.Agendamento;

import java.time.LocalDateTime;

/**
 * Payload imutável contendo os dados necessários para envio de notificações de agendamento,
 * desacoplando os listeners assíncronos do ciclo de vida das entidades JPA.
 */
public record AgendamentoNotificacaoPayload(
        Long idAgendamento,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        String nomeCliente,
        String telefoneCliente,
        String nomeQuadra,
        Long idDonoQuadra
) {
    public static AgendamentoNotificacaoPayload fromEntity(Agendamento agendamento) {
        if (agendamento == null) {
            return null;
        }

        String nomeCliente = (agendamento.getUsuario() != null && agendamento.getUsuario().getNome_usuario() != null)
                ? agendamento.getUsuario().getNome_usuario()
                : "Cliente";

        String telefone = (agendamento.getUsuario() != null && agendamento.getUsuario().getPhone_usuario() != null)
                ? agendamento.getUsuario().getPhone_usuario()
                : "Não informado";

        String nomeQuadra = (agendamento.getQuadra() != null && agendamento.getQuadra().getNome() != null)
                ? agendamento.getQuadra().getNome()
                : "Quadra";

        Long idDonoQuadra = (agendamento.getQuadra() != null && agendamento.getQuadra().getAdmin() != null)
                ? agendamento.getQuadra().getAdmin().getId_usuario()
                : null;

        return new AgendamentoNotificacaoPayload(
                agendamento.getId_agendamento(),
                agendamento.getDataHoraInicio(),
                agendamento.getDataHoraFim(),
                nomeCliente,
                telefone,
                nomeQuadra,
                idDonoQuadra
        );
    }
}
