package com.agendamentos.equadras.listener;

import com.agendamentos.equadras.event.AgendamentoCanceladoEvent;
import com.agendamentos.equadras.event.AgendamentoPagamentoConfirmadoEvent;
import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.service.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;

@Component
public class AgendamentoNotificacaoListener {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoNotificacaoListener.class);
    private static final DateTimeFormatter FORMATADOR_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATADOR_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificacaoService notificacaoService;

    public AgendamentoNotificacaoListener(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendamentoPagamentoConfirmado(AgendamentoPagamentoConfirmadoEvent event) {
        Agendamento salvo = event.agendamento();
        try {
            if (salvo != null && salvo.getQuadra() != null && salvo.getQuadra().getAdmin() != null) {
                String dataFormatada = salvo.getDataHoraInicio().format(FORMATADOR_DATA);
                String horaInicio = salvo.getDataHoraInicio().format(FORMATADOR_HORA);
                String horaFim = salvo.getDataHoraFim().format(FORMATADOR_HORA);
                String telefone = salvo.getUsuario() != null && salvo.getUsuario().getPhone_usuario() != null && !salvo.getUsuario().getPhone_usuario().isBlank()
                        ? salvo.getUsuario().getPhone_usuario()
                        : "Não informado";
                String nomeUsuario = salvo.getUsuario() != null ? salvo.getUsuario().getNome_usuario() : "Cliente";

                String msg = String.format(
                        "Pagamento Pix confirmado!\n\nCliente: %s\nTelefone: %s\nQuadra: %s\nHorário: %s das %s às %s",
                        nomeUsuario,
                        telefone,
                        salvo.getQuadra().getNome(),
                        dataFormatada,
                        horaInicio,
                        horaFim
                );

                notificacaoService.enviarNotificacao(salvo.getQuadra().getAdmin().getId_usuario(), msg);
            }
        } catch (Exception e) {
            log.error("Falha ao enviar notificação de pagamento para admin do agendamento {}: {}",
                    salvo != null ? salvo.getId_agendamento() : "N/A", e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendamentoCancelado(AgendamentoCanceladoEvent event) {
        Agendamento agendamento = event.agendamento();
        Usuario executor = event.executor();
        try {
            if (agendamento != null && agendamento.getQuadra() != null && agendamento.getQuadra().getAdmin() != null) {
                String dataFormatada = agendamento.getDataHoraInicio().format(FORMATADOR_DATA);
                String horaInicio = agendamento.getDataHoraInicio().format(FORMATADOR_HORA);
                String horaFim = agendamento.getDataHoraFim().format(FORMATADOR_HORA);
                String telefone = agendamento.getUsuario() != null && agendamento.getUsuario().getPhone_usuario() != null && !agendamento.getUsuario().getPhone_usuario().isBlank()
                        ? agendamento.getUsuario().getPhone_usuario()
                        : "Não informado";
                String nomeCliente = agendamento.getUsuario() != null ? agendamento.getUsuario().getNome_usuario() : "Cliente";
                String nomeExecutor = executor != null ? executor.getNome_usuario() : "Sistema";

                String msg = String.format(
                        "Agendamento Cancelado!\n\nCliente: %s\nTelefone: %s\nQuadra: %s\nHorário: %s das %s às %s\nCancelado por: %s",
                        nomeCliente,
                        telefone,
                        agendamento.getQuadra().getNome(),
                        dataFormatada,
                        horaInicio,
                        horaFim,
                        nomeExecutor
                );

                notificacaoService.enviarNotificacao(agendamento.getQuadra().getAdmin().getId_usuario(), msg);
            }
        } catch (Exception e) {
            log.error("Falha ao enviar notificação de cancelamento para admin do agendamento {}: {}",
                    agendamento != null ? agendamento.getId_agendamento() : "N/A", e.getMessage(), e);
        }
    }
}
