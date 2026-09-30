package com.agendamentos.equadras.listener;

import com.agendamentos.equadras.event.AgendamentoCanceladoEvent;
import com.agendamentos.equadras.event.AgendamentoConfirmadoSemPagamentoEvent;
import com.agendamentos.equadras.event.AgendamentoNotificacaoPayload;
import com.agendamentos.equadras.event.AgendamentoPagamentoConfirmadoEvent;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.service.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class AgendamentoNotificacaoListener {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoNotificacaoListener.class);
    private static final DateTimeFormatter FORMATADOR_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATADOR_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificacaoService notificacaoService;
    private final UsuarioRepository usuarioRepository;

    public AgendamentoNotificacaoListener(
            NotificacaoService notificacaoService,
            UsuarioRepository usuarioRepository) {
        this.notificacaoService = notificacaoService;
        this.usuarioRepository = usuarioRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onAgendamentoPagamentoConfirmado(AgendamentoPagamentoConfirmadoEvent event) {
        if (event == null || event.payload() == null) return;
        notificarReservaConfirmada("Pagamento Pix confirmado!", event.payload());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onAgendamentoConfirmadoSemPagamento(AgendamentoConfirmadoSemPagamentoEvent event) {
        if (event == null || event.payload() == null) return;
        notificarReservaConfirmada("Nova reserva confirmada!", event.payload());
    }

    private void notificarReservaConfirmada(String titulo, AgendamentoNotificacaoPayload payload) {
        try {
            String dataFormatada = payload.dataHoraInicio().format(FORMATADOR_DATA);
            String horaInicio = payload.dataHoraInicio().format(FORMATADOR_HORA);
            String horaFim = payload.dataHoraFim().format(FORMATADOR_HORA);

            String msg = String.format(
                    "%s\n\nCliente: %s\nTelefone: %s\nQuadra: %s\nHorário: %s das %s às %s",
                    titulo,
                    payload.nomeCliente(),
                    payload.telefoneCliente(),
                    payload.nomeQuadra(),
                    dataFormatada,
                    horaInicio,
                    horaFim
            );

            notificacaoService.notificarAdmins(obterDestinatariosNotificacao(payload.idDonoQuadra()), msg);
        } catch (Exception e) {
            log.error("Falha ao enviar notificação de reserva confirmada do agendamento {}: {}",
                    payload.idAgendamento(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onAgendamentoCancelado(AgendamentoCanceladoEvent event) {
        if (event == null || event.payload() == null) return;
        AgendamentoNotificacaoPayload payload = event.payload();
        String nomeExecutor = (event.executorNome() != null && !event.executorNome().isBlank())
                ? event.executorNome()
                : "Sistema";

        try {
            String dataFormatada = payload.dataHoraInicio().format(FORMATADOR_DATA);
            String horaInicio = payload.dataHoraInicio().format(FORMATADOR_HORA);
            String horaFim = payload.dataHoraFim().format(FORMATADOR_HORA);

            String msg = String.format(
                    "Agendamento Cancelado!\n\nCliente: %s\nTelefone: %s\nQuadra: %s\nHorário: %s das %s às %s\nCancelado por: %s",
                    payload.nomeCliente(),
                    payload.telefoneCliente(),
                    payload.nomeQuadra(),
                    dataFormatada,
                    horaInicio,
                    horaFim,
                    nomeExecutor
            );

            notificacaoService.notificarAdmins(obterDestinatariosNotificacao(payload.idDonoQuadra()), msg);
        } catch (Exception e) {
            log.error("Falha ao enviar notificação de cancelamento do agendamento {}: {}",
                    payload.idAgendamento(), e.getMessage(), e);
        }
    }

    private Set<Long> obterDestinatariosNotificacao(Long idDonoQuadra) {
        Set<Long> destinatarios = new LinkedHashSet<>();

        // 1. Notifica o dono da quadra (se existir)
        if (idDonoQuadra != null) {
            destinatarios.add(idDonoQuadra);
        }

        // 2. Notifica todos os administradores ativos (incluindo Master Admin)
        try {
            for (Usuario admin : usuarioRepository.findByRole(Role.ADMIN)) {
                if (admin.isAtivo() && admin.getId_usuario() != null) {
                    destinatarios.add(admin.getId_usuario());
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao buscar lista de administradores para notificação: {}", e.getMessage());
        }

        return destinatarios;
    }
}
