package com.agendamentos.equadras.listener;

import com.agendamentos.equadras.event.AgendamentoCanceladoEvent;
import com.agendamentos.equadras.event.AgendamentoPagamentoConfirmadoEvent;
import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.AgendamentoRepository;
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
    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;

    public AgendamentoNotificacaoListener(
            NotificacaoService notificacaoService,
            AgendamentoRepository agendamentoRepository,
            UsuarioRepository usuarioRepository) {
        this.notificacaoService = notificacaoService;
        this.agendamentoRepository = agendamentoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendamentoPagamentoConfirmado(AgendamentoPagamentoConfirmadoEvent event) {
        Agendamento salvo = event.agendamento();
        if (salvo == null) return;

        try {
            // Recarrega com entity graph para garantir que quadra e quadra.admin estejam inicializados fora da sessão
            Agendamento agendamento = agendamentoRepository.buscarComAdminEUsuarioPorId(salvo.getId_agendamento())
                    .orElse(salvo);

            String dataFormatada = agendamento.getDataHoraInicio().format(FORMATADOR_DATA);
            String horaInicio = agendamento.getDataHoraInicio().format(FORMATADOR_HORA);
            String horaFim = agendamento.getDataHoraFim().format(FORMATADOR_HORA);
            String telefone = agendamento.getUsuario() != null && agendamento.getUsuario().getPhone_usuario() != null && !agendamento.getUsuario().getPhone_usuario().isBlank()
                    ? agendamento.getUsuario().getPhone_usuario()
                    : "Não informado";
            String nomeUsuario = agendamento.getUsuario() != null ? agendamento.getUsuario().getNome_usuario() : "Cliente";
            String nomeQuadra = agendamento.getQuadra() != null ? agendamento.getQuadra().getNome() : "Quadra";

            String msg = String.format(
                    "Pagamento Pix confirmado!\n\nCliente: %s\nTelefone: %s\nQuadra: %s\nHorário: %s das %s às %s",
                    nomeUsuario,
                    telefone,
                    nomeQuadra,
                    dataFormatada,
                    horaInicio,
                    horaFim
            );

            Set<Long> destinatarios = obterDestinatariosNotificacao(agendamento);
            for (Long adminId : destinatarios) {
                try {
                    notificacaoService.enviarNotificacao(adminId, msg);
                } catch (Exception ex) {
                    log.error("Erro ao enviar notificação de pagamento para o admin {}: {}", adminId, ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Falha ao enviar notificação de pagamento do agendamento {}: {}",
                    salvo.getId_agendamento(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendamentoCancelado(AgendamentoCanceladoEvent event) {
        Agendamento salvo = event.agendamento();
        Usuario executor = event.executor();
        if (salvo == null) return;

        try {
            // Recarrega com entity graph para garantir que quadra e quadra.admin estejam inicializados fora da sessão
            Agendamento agendamento = agendamentoRepository.buscarComAdminEUsuarioPorId(salvo.getId_agendamento())
                    .orElse(salvo);

            String dataFormatada = agendamento.getDataHoraInicio().format(FORMATADOR_DATA);
            String horaInicio = agendamento.getDataHoraInicio().format(FORMATADOR_HORA);
            String horaFim = agendamento.getDataHoraFim().format(FORMATADOR_HORA);
            String telefone = agendamento.getUsuario() != null && agendamento.getUsuario().getPhone_usuario() != null && !agendamento.getUsuario().getPhone_usuario().isBlank()
                    ? agendamento.getUsuario().getPhone_usuario()
                    : "Não informado";
            String nomeCliente = agendamento.getUsuario() != null ? agendamento.getUsuario().getNome_usuario() : "Cliente";
            String nomeExecutor = executor != null ? executor.getNome_usuario() : "Sistema";
            String nomeQuadra = agendamento.getQuadra() != null ? agendamento.getQuadra().getNome() : "Quadra";

            String msg = String.format(
                    "Agendamento Cancelado!\n\nCliente: %s\nTelefone: %s\nQuadra: %s\nHorário: %s das %s às %s\nCancelado por: %s",
                    nomeCliente,
                    telefone,
                    nomeQuadra,
                    dataFormatada,
                    horaInicio,
                    horaFim,
                    nomeExecutor
            );

            Set<Long> destinatarios = obterDestinatariosNotificacao(agendamento);
            for (Long adminId : destinatarios) {
                try {
                    notificacaoService.enviarNotificacao(adminId, msg);
                } catch (Exception ex) {
                    log.error("Erro ao enviar notificação de cancelamento para o admin {}: {}", adminId, ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Falha ao enviar notificação de cancelamento do agendamento {}: {}",
                    salvo.getId_agendamento(), e.getMessage(), e);
        }
    }

    private Set<Long> obterDestinatariosNotificacao(Agendamento agendamento) {
        Set<Long> destinatarios = new LinkedHashSet<>();

        // 1. Notifica o dono da quadra (se existir)
        if (agendamento.getQuadra() != null && agendamento.getQuadra().getAdmin() != null) {
            destinatarios.add(agendamento.getQuadra().getAdmin().getId_usuario());
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

