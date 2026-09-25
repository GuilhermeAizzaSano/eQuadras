package com.agendamentos.equadras.listener;

import com.agendamentos.equadras.event.QuadraAlteradaEvent;
import com.agendamentos.equadras.model.enums.CategoriaAuditoria;
import com.agendamentos.equadras.service.AuditoriaService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class QuadraAuditoriaListener {

    private final AuditoriaService auditoriaService;

    public QuadraAuditoriaListener(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQuadraAlterada(QuadraAlteradaEvent event) {
        if (auditoriaService == null || event == null) return;

        auditoriaService.registrarAcaoPorUsuarioId(
                event.usuarioId(),
                CategoriaAuditoria.QUADRA,
                event.acao(),
                "QUADRA",
                event.quadraId() != null ? event.quadraId().toString() : null,
                event.detalhes()
        );
    }
}
