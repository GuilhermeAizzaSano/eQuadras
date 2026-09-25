package com.agendamentos.equadras.service;

import com.agendamentos.equadras.event.AgendamentosExpiradosCanceladosEvent;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoExpiracaoSchedulerTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Clock clock;

    @InjectMocks
    private AgendamentoExpiracaoScheduler scheduler;

    private final ZoneId zoneId = ZoneId.of("America/Sao_Paulo");

    @BeforeEach
    void setUp() {
        lenient().when(clock.getZone()).thenReturn(zoneId);
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-09-25T10:00:00Z"));
    }

    @Test
    @DisplayName("Deve expirar agendamentos e publicar evento quando houver cancelamentos")
    void deveExpirarEPublicarEventoQuandoHouverCancelados() {
        when(agendamentoRepository.cancelarPendentesExpirados(
                eq(StatusAgendamento.PENDENTE),
                eq(StatusAgendamento.CANCELADO),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(3);

        scheduler.expirarAgendamentosPendentes();

        ArgumentCaptor<AgendamentosExpiradosCanceladosEvent> captor = ArgumentCaptor.forClass(AgendamentosExpiradosCanceladosEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertNotNull(captor.getValue());
        assertEquals(3, captor.getValue().quantidadeCancelados());
    }

    @Test
    @DisplayName("Não deve publicar evento quando nenhum agendamento for expirado")
    void naoDevePublicarEventoQuandoNaoHouverExpirados() {
        when(agendamentoRepository.cancelarPendentesExpirados(
                eq(StatusAgendamento.PENDENTE),
                eq(StatusAgendamento.CANCELADO),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(0);

        scheduler.expirarAgendamentosPendentes();

        verify(eventPublisher, never()).publishEvent(any());
    }
}
