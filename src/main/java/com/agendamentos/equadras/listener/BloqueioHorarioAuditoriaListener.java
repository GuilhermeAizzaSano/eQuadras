package com.agendamentos.equadras.listener;

import com.agendamentos.equadras.event.BloqueioHorarioAlteradoEvent;
import com.agendamentos.equadras.model.enums.CategoriaAuditoria;
import com.agendamentos.equadras.service.AuditoriaService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BloqueioHorarioAuditoriaListener {

    private final AuditoriaService auditoriaService;

    public BloqueioHorarioAuditoriaListener(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBloqueioHorarioAlterado(BloqueioHorarioAlteradoEvent event) {
        if (auditoriaService == null || event == null) return;

        auditoriaService.registrarAcaoPorUsuarioId(
                event.usuarioId(),
                CategoriaAuditoria.BLOQUEIO,
                event.acao(),
                "BLOQUEIO",
                event.entidadeId(),
                event.detalhes()
        );
    }
}
