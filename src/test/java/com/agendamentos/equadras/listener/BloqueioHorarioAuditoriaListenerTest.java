package com.agendamentos.equadras.listener;

import com.agendamentos.equadras.event.BloqueioHorarioAlteradoEvent;
import com.agendamentos.equadras.model.enums.CategoriaAuditoria;
import com.agendamentos.equadras.service.AuditoriaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BloqueioHorarioAuditoriaListenerTest {

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private BloqueioHorarioAuditoriaListener listener;

    @Test
    @DisplayName("Deve registrar auditoria ao receber BloqueioHorarioAlteradoEvent")
    void deveRegistrarAuditoriaAoReceberEvento() {
        BloqueioHorarioAlteradoEvent event = new BloqueioHorarioAlteradoEvent(
                1L,
                "CRIAR",
                "10",
                "Bloqueio criado na quadra Central"
        );

        listener.onBloqueioHorarioAlterado(event);

        verify(auditoriaService, times(1)).registrarAcaoPorUsuarioId(
                1L,
                CategoriaAuditoria.BLOQUEIO,
                "CRIAR",
                "BLOQUEIO",
                "10",
                "Bloqueio criado na quadra Central"
        );
    }

    @Test
    @DisplayName("Não deve lançar erro se auditoriaService for nulo ou evento nulo")
    void naoDeveLancarErroQuandoNulo() {
        listener.onBloqueioHorarioAlterado(null);
        verifyNoInteractions(auditoriaService);

        BloqueioHorarioAuditoriaListener listenerSemAuditoria = new BloqueioHorarioAuditoriaListener(null);
        listenerSemAuditoria.onBloqueioHorarioAlterado(new BloqueioHorarioAlteradoEvent(1L, "CRIAR", "1", "detalhes"));
    }
}
