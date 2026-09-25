package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.response.DashboardMetricasDTO;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private QuadraRepository quadraRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private DashboardService dashboardService;

    private Usuario adminComum;
    private Usuario masterAdmin;

    @BeforeEach
    void setUp() {
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("America/Sao_Paulo"));
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-09-25T10:00:00Z"));

        adminComum = Usuario.builder()
                .id_usuario(1L)
                .nome_usuario("Admin Comum")
                .email_usuario("admin@equadras.com")
                .role(Role.ADMIN)
                .build();

        masterAdmin = Usuario.builder()
                .id_usuario(99L)
                .nome_usuario("Master Admin")
                .email_usuario("gui@gmail.com")
                .role(Role.ADMIN)
                .build();
    }

    @Test
    @DisplayName("Deve obter métricas do dashboard para admin comum com sucesso")
    void deveObterMetricasParaAdminComum() {
        when(usuarioService.buscarPorIdEntidade(1L)).thenReturn(Optional.of(adminComum));
        when(quadraRepository.obterMetricasQuadrasPorAdminId(1L))
                .thenReturn(List.<Object[]>of(new Object[]{5L, 4L}));
        when(agendamentoRepository.obterMetricasAgendamentosPorAdminId(eq(1L), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{10L, BigDecimal.valueOf(1500.00), 2L}));

        DashboardMetricasDTO metricas = dashboardService.obterMetricasDashboard(1L);

        assertNotNull(metricas);
        assertEquals(5L, metricas.totalQuadras());
        assertEquals(4L, metricas.quadrasAtivas());
        assertEquals(10L, metricas.totalReservas());
        assertEquals(BigDecimal.valueOf(1500.00), metricas.faturamentoTotal());
        assertEquals(2L, metricas.reservasHoje());

        verify(quadraRepository, times(1)).obterMetricasQuadrasPorAdminId(1L);
        verify(agendamentoRepository, times(1)).obterMetricasAgendamentosPorAdminId(eq(1L), any(), any());
        verify(quadraRepository, never()).obterMetricasQuadrasMasterAdmin();
    }

    @Test
    @DisplayName("Deve obter métricas globais do dashboard para master admin")
    void deveObterMetricasParaMasterAdmin() {
        when(usuarioService.buscarPorIdEntidade(99L)).thenReturn(Optional.of(masterAdmin));
        when(quadraRepository.obterMetricasQuadrasMasterAdmin())
                .thenReturn(List.<Object[]>of(new Object[]{20L, 18L}));
        when(agendamentoRepository.obterMetricasAgendamentosMasterAdmin(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{100L, BigDecimal.valueOf(15000.00), 12L}));

        DashboardMetricasDTO metricas = dashboardService.obterMetricasDashboard(99L);

        assertNotNull(metricas);
        assertEquals(20L, metricas.totalQuadras());
        assertEquals(18L, metricas.quadrasAtivas());
        assertEquals(100L, metricas.totalReservas());
        assertEquals(BigDecimal.valueOf(15000.00), metricas.faturamentoTotal());
        assertEquals(12L, metricas.reservasHoje());

        verify(quadraRepository, times(1)).obterMetricasQuadrasMasterAdmin();
        verify(agendamentoRepository, times(1)).obterMetricasAgendamentosMasterAdmin(any(), any());
        verify(quadraRepository, never()).obterMetricasQuadrasPorAdminId(any());
    }

    @Test
    @DisplayName("Deve lançar AccessDeniedException quando usuário não for ADMIN")
    void deveNegarAcessoQuandoNaoForAdmin() {
        Usuario cliente = Usuario.builder()
                .id_usuario(5L)
                .nome_usuario("Cliente")
                .role(Role.CLIENT)
                .build();

        when(usuarioService.buscarPorIdEntidade(5L)).thenReturn(Optional.of(cliente));

        assertThrows(AccessDeniedException.class, () -> dashboardService.obterMetricasDashboard(5L));
    }

    @Test
    @DisplayName("Deve lançar AccessDeniedException quando adminId for nulo")
    void deveNegarAcessoQuandoAdminIdNulo() {
        assertThrows(AccessDeniedException.class, () -> dashboardService.obterMetricasDashboard(null));
    }
}
