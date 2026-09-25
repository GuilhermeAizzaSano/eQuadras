package com.agendamentos.equadras.listener;

import com.agendamentos.equadras.event.QuadraAlteradaEvent;
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
class QuadraAuditoriaListenerTest {

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private QuadraAuditoriaListener listener;

    @Test
    @DisplayName("Deve registrar auditoria com sucesso ao receber evento QuadraAlteradaEvent")
    void deveRegistrarAuditoriaAoReceberEvento() {
        QuadraAlteradaEvent event = new QuadraAlteradaEvent(
                10L,
                "Arena Central",
                "São Paulo",
                "SP",
                1L,
                "CRIAR",
                "Quadra cadastrada: Arena Central (São Paulo/SP)"
        );

        listener.onQuadraAlterada(event);

        verify(auditoriaService, times(1)).registrarAcaoPorUsuarioId(
                1L,
                CategoriaAuditoria.QUADRA,
                "CRIAR",
                "QUADRA",
                "10",
                "Quadra cadastrada: Arena Central (São Paulo/SP)"
        );
    }

    @Test
    @DisplayName("Não deve registrar auditoria quando evento for nulo")
    void naoDeveRegistrarAuditoriaQuandoEventoNulo() {
        listener.onQuadraAlterada(null);

        verifyNoInteractions(auditoriaService);
    }
}
