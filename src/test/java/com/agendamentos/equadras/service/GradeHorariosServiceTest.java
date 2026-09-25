package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.response.GradeHorariosResponseDTO;
import com.agendamentos.equadras.dto.response.HorarioDisponivelDTO;
import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.BloqueioHorario;
import com.agendamentos.equadras.model.entity.DisponibilidadeDia;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.model.enums.StatusHorario;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.BloqueioHorarioRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradeHorariosServiceTest {

    @Mock
    private QuadraRepository quadraRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private BloqueioHorarioRepository bloqueioHorarioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private QuadraBuscaService quadraBuscaService;

    @Mock
    private Clock clock;

    @InjectMocks
    private GradeHorariosService gradeHorariosService;

    private Quadra quadra;
    private Usuario admin;
    private final ZoneId zoneId = ZoneId.of("America/Sao_Paulo");

    @BeforeEach
    void setUp() {
        lenient().when(clock.getZone()).thenReturn(zoneId);
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-09-25T10:00:00Z"));

        admin = Usuario.builder()
                .id_usuario(1L)
                .nome_usuario("Admin")
                .email_usuario("admin@equadras.com")
                .role(Role.ADMIN)
                .build();

        List<DisponibilidadeDia> disponibilidades = new ArrayList<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            disponibilidades.add(new DisponibilidadeDia(dia, LocalTime.of(8, 0), LocalTime.of(22, 0)));
        }

        quadra = Quadra.builder()
                .id_quadra(1L)
                .nome("Quadra Central")
                .tipoEsporte(TipoEsporte.FUTEBOL)
                .valorHora(BigDecimal.valueOf(100.00))
                .ativa(true)
                .admin(admin)
                .disponibilidades(disponibilidades)
                .build();
    }

    @Test
    @DisplayName("Deve listar slots de horários marcando agendados como ocupados")
    void deveListarHorariosDisponiveisComAgendamento() {
        LocalDate dataAmanha = LocalDate.now(clock).plusDays(1);
        LocalDateTime inicioAgendado = LocalDateTime.of(dataAmanha, LocalTime.of(10, 0));
        LocalDateTime fimAgendado = LocalDateTime.of(dataAmanha, LocalTime.of(11, 0));

        Agendamento agendamentoExistente = Agendamento.builder()
                .id_agendamento(10L)
                .quadra(quadra)
                .dataHoraInicio(inicioAgendado)
                .dataHoraFim(fimAgendado)
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        when(quadraRepository.findById(1L)).thenReturn(Optional.of(quadra));
        when(agendamentoRepository.buscarPorQuadraEData(eq(1L), eq(StatusAgendamento.CANCELADO), any(), any()))
                .thenReturn(List.of(agendamentoExistente));
        when(bloqueioHorarioRepository.findByQuadraIdAndData(1L, dataAmanha)).thenReturn(List.of());

        List<HorarioDisponivelDTO> slots = gradeHorariosService.listarHorariosDisponiveis(1L, dataAmanha);

        assertFalse(slots.isEmpty());
        HorarioDisponivelDTO slot10 = slots.stream().filter(s -> s.inicio().equals(LocalTime.of(10, 0))).findFirst().orElseThrow();
        assertFalse(slot10.disponivel());
        assertEquals(StatusHorario.AGENDADO, slot10.status());

        HorarioDisponivelDTO slot11 = slots.stream().filter(s -> s.inicio().equals(LocalTime.of(11, 0))).findFirst().orElseThrow();
        assertTrue(slot11.disponivel());
        assertEquals(StatusHorario.DISPONIVEL, slot11.status());
    }

    @Test
    @DisplayName("Deve marcar todos os slots como inativos quando quadra estiver desativada")
    void deveBloquearHorariosQuandoQuadraInativa() {
        quadra.setAtiva(false);
        LocalDate dataAmanha = LocalDate.now(clock).plusDays(1);

        when(quadraRepository.findById(1L)).thenReturn(Optional.of(quadra));
        when(agendamentoRepository.buscarPorQuadraEData(eq(1L), eq(StatusAgendamento.CANCELADO), any(), any()))
                .thenReturn(List.of());
        when(bloqueioHorarioRepository.findByQuadraIdAndData(1L, dataAmanha)).thenReturn(List.of());

        List<HorarioDisponivelDTO> slots = gradeHorariosService.listarHorariosDisponiveis(1L, dataAmanha);

        assertTrue(slots.stream().noneMatch(HorarioDisponivelDTO::disponivel));
        assertTrue(slots.stream().allMatch(s -> "Quadra inativa".equals(s.motivo())));
    }

    @Test
    @DisplayName("Deve marcar horários com bloqueio de dia inteiro")
    void deveBloquearDiaInteiroQuandoHouverBloqueioTotal() {
        LocalDate dataConsulta = LocalDate.now(clock).plusDays(3);
        BloqueioHorario bloqueio = new BloqueioHorario(quadra, dataConsulta, null, null, "Reforma Geral");

        when(quadraRepository.findById(1L)).thenReturn(Optional.of(quadra));
        when(agendamentoRepository.buscarPorQuadraEData(eq(1L), eq(StatusAgendamento.CANCELADO), any(), any()))
                .thenReturn(List.of());
        when(bloqueioHorarioRepository.findByQuadraIdAndData(1L, dataConsulta)).thenReturn(List.of(bloqueio));

        List<HorarioDisponivelDTO> slots = gradeHorariosService.listarHorariosDisponiveis(1L, dataConsulta);

        assertFalse(slots.isEmpty());
        assertTrue(slots.stream().noneMatch(HorarioDisponivelDTO::disponivel));
        assertTrue(slots.stream().allMatch(s -> "Bloqueado: Reforma Geral".equals(s.motivo())));
    }

    @Test
    @DisplayName("Deve consultar grade de horários por quadraId")
    void deveConsultarGradeHorariosPorQuadraId() {
        when(quadraBuscaService.buscarQuadrasAtivas(1L, null, null)).thenReturn(List.of(quadra));
        when(quadraRepository.findById(1L)).thenReturn(Optional.of(quadra));
        LocalDate amanha = LocalDate.now(clock).plusDays(1);

        when(agendamentoRepository.buscarPorQuadraEData(eq(1L), eq(StatusAgendamento.CANCELADO), any(), any()))
                .thenReturn(List.of());
        when(bloqueioHorarioRepository.findByQuadraIdAndData(1L, amanha)).thenReturn(List.of());

        List<GradeHorariosResponseDTO> grade =
                gradeHorariosService.consultarGradeHorarios(amanha, 1L, null, null, false);

        assertNotNull(grade);
        assertEquals(1, grade.size());
        assertEquals("Quadra Central", grade.get(0).nome_quadra());
    }

    @Test
    @DisplayName("Deve listar horários consolidados do dia para admin em lote")
    void deveListarHorariosDoDiaParaAdmin() {
        LocalDate data = LocalDate.now(clock).plusDays(1);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(quadraRepository.findByAdminId(1L)).thenReturn(List.of(quadra));
        when(agendamentoRepository.buscarPorQuadrasEDataLote(any(), any(), any(), any())).thenReturn(List.of());
        when(bloqueioHorarioRepository.findByQuadraIdsAndData(any(), eq(data))).thenReturn(List.of());

        Map<Long, List<HorarioDisponivelDTO>> resultado = gradeHorariosService.listarHorariosDoDiaParaAdmin(data, 1L);

        assertNotNull(resultado);
        assertTrue(resultado.containsKey(1L));
        assertFalse(resultado.get(1L).isEmpty());
    }
}
