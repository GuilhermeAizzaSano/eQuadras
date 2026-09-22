package com.agendamentos.equadras.listener;

import com.agendamentos.equadras.event.AgendamentoCanceladoEvent;
import com.agendamentos.equadras.event.AgendamentosExpiradosCanceladosEvent;
import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.CategoriaAuditoria;
import com.agendamentos.equadras.service.AuditoriaService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AgendamentoAuditoriaListener {

    private final AuditoriaService auditoriaService;

    public AgendamentoAuditoriaListener(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendamentoCancelado(AgendamentoCanceladoEvent event) {
        if (auditoriaService == null) return;

        Agendamento agendamento = event.agendamento();
        Usuario usuario = event.executor();

        String tipoExecutor = usuario != null && usuario.isMasterAdmin()
                ? "MASTER_ADMIN"
                : (usuario != null && usuario.getRole() == com.agendamentos.equadras.model.enums.Role.ADMIN ? "ADMIN_QUADRA" : "CLIENTE");

        String nomeQuadra = agendamento.getQuadra() != null ? agendamento.getQuadra().getNome() : "N/A";
        String executorNome = usuario != null ? usuario.getNome_usuario() : "SISTEMA";

        auditoriaService.registrarAcao(
                usuario,
                CategoriaAuditoria.AGENDAMENTO,
                "CANCELAR",
                "AGENDAMENTO",
                agendamento.getId_agendamento().toString(),
                String.format("Agendamento #%d cancelado por %s (%s). Quadra: %s. Horário: %s até %s",
                        agendamento.getId_agendamento(), executorNome, tipoExecutor, nomeQuadra,
                        agendamento.getDataHoraInicio(), agendamento.getDataHoraFim())
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendamentosExpiradosCancelados(AgendamentosExpiradosCanceladosEvent event) {
        if (auditoriaService == null || event.quantidadeCancelados() <= 0) return;

        auditoriaService.registrarAcaoSistema(
                CategoriaAuditoria.AGENDAMENTO,
                "CANCELAR",
                "AGENDAMENTO",
                "BATCH",
                "Cancelamento automático de " + event.quantidadeCancelados() + " agendamento(s) pendente(s) expirado(s) por timeout de pagamento Pix (15 min)."
        );
    }
}
