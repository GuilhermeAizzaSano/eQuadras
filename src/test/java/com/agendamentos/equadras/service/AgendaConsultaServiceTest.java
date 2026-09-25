package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.AbaAgendamento;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.ContagemPorAba;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.shared.pagination.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendaConsultaServiceTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private QuadraRepository quadraRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private AgendaConsultaService agendaConsultaService;

    private Usuario admin;
    private Usuario masterAdmin;
    private Quadra quadra;
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

        masterAdmin = Usuario.builder()
                .id_usuario(2L)
                .nome_usuario("Master")
                .email_usuario("gui@gmail.com")
                .role(Role.ADMIN)
                .build();

        quadra = Quadra.builder()
                .id_quadra(10L)
                .nome("Quadra Principal")
                .admin(admin)
                .ativa(true)
                .build();
    }

    @Test
    @DisplayName("Deve listar agenda completa do dia para admin autorizado")
    void deveListarAgendaCompleta() {
        LocalDate data = LocalDate.now(clock).plusDays(1);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(quadraRepository.findById(10L)).thenReturn(Optional.of(quadra));

        Agendamento ag = Agendamento.builder()
                .id_agendamento(100L)
                .quadra(quadra)
                .usuario(admin)
                .dataHoraInicio(data.atTime(10, 0))
                .dataHoraFim(data.atTime(11, 0))
                .valorTotal(BigDecimal.valueOf(100.00))
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        when(agendamentoRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(ag));

        List<AgendamentoResponseDTO> lista = agendaConsultaService.listarAgendaCompleta(1L, data, null, null, 10L);

        assertNotNull(lista);
        assertEquals(1, lista.size());
        assertEquals(100L, lista.get(0).id_agendamento());
    }

    @Test
    @DisplayName("Deve rejeitar intervalo superior a 24 horas em agenda completa")
    void deveRejeitarIntervaloSuperiorA24Horas() {
        LocalDateTime inicio = LocalDateTime.now(clock);
        LocalDateTime fim = inicio.plusHours(25);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                agendaConsultaService.listarAgendaCompleta(1L, null, inicio, fim, 10L)
        );

        assertTrue(ex.getMessage().contains("superior a 24 horas"));
    }

    @Test
    @DisplayName("Deve listar agenda do dia paginada com sucesso")
    void deveListarAgendaDoDiaPaginado() {
        LocalDate data = LocalDate.now(clock).plusDays(1);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

        Agendamento ag = Agendamento.builder()
                .id_agendamento(101L)
                .quadra(quadra)
                .usuario(admin)
                .dataHoraInicio(data.atTime(14, 0))
                .dataHoraFim(data.atTime(15, 0))
                .valorTotal(BigDecimal.valueOf(120.00))
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        Page<Agendamento> page = new PageImpl<>(List.of(ag));
        when(agendamentoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<AgendamentoResponseDTO> resp = agendaConsultaService.listarAgendaDoDiaPaginado(
                1L, data, null, null, null, AbaAgendamento.ATIVOS, PageRequest.of(0, 10)
        );

        assertNotNull(resp);
        assertEquals(1, resp.content().size());
        assertEquals(101L, resp.content().get(0).id_agendamento());
    }

    @Test
    @DisplayName("Deve contar agenda do dia por aba")
    void deveContarAgendaDoDiaPorAba() {
        LocalDate data = LocalDate.now(clock).plusDays(1);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        Map<AbaAgendamento, Long> porAba = new java.util.EnumMap<>(AbaAgendamento.class);
        for (AbaAgendamento aba : AbaAgendamento.values()) {
            porAba.put(aba, 5L);
        }
        when(agendamentoRepository.contarPorAba(any(), any())).thenReturn(new ContagemPorAba(15L, porAba));

        Map<AbaAgendamento, Long> contadores = agendaConsultaService.contarAgendaDoDiaPorAba(1L, data, null, null, null);

        assertNotNull(contadores);
        assertTrue(contadores.containsKey(AbaAgendamento.ATIVOS));
        assertEquals(5L, contadores.get(AbaAgendamento.ATIVOS));
    }

    @Test
    @DisplayName("Deve negar acesso quando admin não for proprietário da quadra")
    void deveNegarAcessoQuandoNaoForProprietario() {
        Usuario outroAdmin = Usuario.builder()
                .id_usuario(99L)
                .role(Role.ADMIN)
                .build();

        LocalDate data = LocalDate.now(clock).plusDays(1);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(outroAdmin));
        when(quadraRepository.findById(10L)).thenReturn(Optional.of(quadra));

        assertThrows(AccessDeniedException.class, () ->
                agendaConsultaService.listarAgendaCompleta(99L, data, null, null, 10L)
        );
    }
}
