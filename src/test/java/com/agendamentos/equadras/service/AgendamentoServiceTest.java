package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.dto.response.HorarioDisponivelDTO;
import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private QuadraRepository quadraRepository;

    @Mock
    private NotificacaoService notificacaoService;

    @Mock
    private PagamentoService pagamentoService;

    @InjectMocks
    private AgendamentoService agendamentoService;

    private Usuario usuario;
    private Quadra quadra;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id_usuario(1L)
                .nome_usuario("Carlos")
                .email_usuario("carlos@email.com")
                .role(Role.CLIENT)
                .build();

        quadra = Quadra.builder()
                .id_quadra(1L)
                .nome("Quadra de Tênis")
                .tipoEsporte(TipoEsporte.TENIS)
                .valorHora(BigDecimal.valueOf(100.00))
                .ativa(true)
                .build();
    }

    @Test
    @DisplayName("Deve agendar com sucesso quando o horário estiver disponível")
    void deveAgendarComSucesso() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fim = inicio.plusHours(1);

        AgendamentoCriacaoDTO dto = new AgendamentoCriacaoDTO(1L, 1L, inicio, fim);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(quadraRepository.buscarComLockParaAgendamento(1L)).thenReturn(Optional.of(quadra));
        when(quadraRepository.findByIdWithAdmin(1L)).thenReturn(Optional.of(quadra));
        when(agendamentoRepository.existeConflitoHorario(eq(1L), eq(inicio), eq(fim), eq(StatusAgendamento.CANCELADO)))
                .thenReturn(false);
        when(pagamentoService.gerarPix(any(Agendamento.class)))
                .thenReturn(new PagamentoService.PixDados("tx-1", "pix-copia-cola", "qr-code-base64"));

        Agendamento agendamentoSalvo = Agendamento.builder()
                .id_agendamento(10L)
                .usuario(usuario)
                .quadra(quadra)
                .dataHoraInicio(inicio)
                .dataHoraFim(fim)
                .valorTotal(BigDecimal.valueOf(100.00))
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        when(agendamentoRepository.save(any(Agendamento.class))).thenReturn(agendamentoSalvo);

        AgendamentoResponseDTO resposta = agendamentoService.agendar(dto, 1L);

        assertNotNull(resposta);
        assertEquals(10L, resposta.id_agendamento());
        assertEquals("Quadra de Tênis", resposta.nomeQuadra());
        verify(agendamentoRepository, times(1)).save(any(Agendamento.class));
    }

    @Test
    @DisplayName("Deve lançar exceção e bloquear agendamento quando houver conflito de horário")
    void deveBloquearQuandoConflitoDeHorario() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fim = inicio.plusHours(1);

        AgendamentoCriacaoDTO dto = new AgendamentoCriacaoDTO(1L, 1L, inicio, fim);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(quadraRepository.findByIdWithAdmin(1L)).thenReturn(Optional.of(quadra));
        when(agendamentoRepository.existeConflitoHorario(eq(1L), eq(inicio), eq(fim), eq(StatusAgendamento.CANCELADO)))
                .thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> agendamentoService.agendar(dto, 1L));
        assertTrue(ex.getMessage().contains("não está disponível"));
        verify(agendamentoRepository, never()).save(any(Agendamento.class));
    }

    @Test
    @DisplayName("Deve listar horários e marcar como bloqueado o horário que já possui agendamento")
    void deveListarHorariosComBloqueioDeOcupados() {
        LocalDate dataAmanha = LocalDate.now().plusDays(1);
        LocalDateTime inicioAgendado = dataAmanha.atTime(10, 0);
        LocalDateTime fimAgendado = dataAmanha.atTime(11, 0);

        Agendamento agendamentoExistente = Agendamento.builder()
                .id_agendamento(5L)
                .quadra(quadra)
                .usuario(usuario)
                .dataHoraInicio(inicioAgendado)
                .dataHoraFim(fimAgendado)
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        when(quadraRepository.findById(1L)).thenReturn(Optional.of(quadra));
        when(agendamentoRepository.buscarPorQuadraEData(eq(1L), eq(StatusAgendamento.CANCELADO), any(), any()))
                .thenReturn(List.of(agendamentoExistente));

        List<HorarioDisponivelDTO> slots = agendamentoService.listarHorariosDisponiveis(1L, dataAmanha);

        assertFalse(slots.isEmpty());

        HorarioDisponivelDTO slot10as11 = slots.stream()
                .filter(s -> s.inicio().equals(LocalTime.of(10, 0)))
                .findFirst()
                .orElseThrow();

        assertFalse(slot10as11.disponivel());
        assertEquals("Horário ocupado", slot10as11.motivo());

        HorarioDisponivelDTO slot11as12 = slots.stream()
                .filter(s -> s.inicio().equals(LocalTime.of(11, 0)))
                .findFirst()
                .orElseThrow();

        assertTrue(slot11as12.disponivel());
        assertEquals("Disponível", slot11as12.motivo());
    }

    @Test
    @DisplayName("Deve bloquear todos os horários se a quadra estiver inativa")
    void deveBloquearHorariosSeQuadraInativa() {
        quadra.setAtiva(false);
        LocalDate dataAmanha = LocalDate.now().plusDays(1);

        when(quadraRepository.findById(1L)).thenReturn(Optional.of(quadra));
        when(agendamentoRepository.buscarPorQuadraEData(eq(1L), eq(StatusAgendamento.CANCELADO), any(), any()))
                .thenReturn(List.of());

        List<HorarioDisponivelDTO> slots = agendamentoService.listarHorariosDisponiveis(1L, dataAmanha);

        assertTrue(slots.stream().noneMatch(HorarioDisponivelDTO::disponivel));
        assertTrue(slots.stream().allMatch(s -> "Quadra inativa".equals(s.motivo())));
    }
}
