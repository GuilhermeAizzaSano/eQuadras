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
        if (auditoriaService == null || event == null || event.payload() == null) return;

        var payload = event.payload();
        String tipoExecutor = event.tipoExecutor() != null ? event.tipoExecutor().name() : "SISTEMA";
        String nomeQuadra = payload.nomeQuadra() != null ? payload.nomeQuadra() : "N/A";
        String executorNome = event.executorNome() != null ? event.executorNome() : "SISTEMA";

        auditoriaService.registrarAcaoPorUsuarioId(
                event.executorId(),
                CategoriaAuditoria.AGENDAMENTO,
                "CANCELAR",
                "AGENDAMENTO",
                payload.idAgendamento().toString(),
                String.format("Agendamento #%d cancelado por %s (%s). Quadra: %s. Horário: %s até %s",
                        payload.idAgendamento(), executorNome, tipoExecutor, nomeQuadra,
                        payload.dataHoraInicio(), payload.dataHoraFim())
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
