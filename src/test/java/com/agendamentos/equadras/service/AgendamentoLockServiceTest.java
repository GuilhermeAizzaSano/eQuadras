package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoLockServiceTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private QuadraRepository quadraRepository;

    @InjectMocks
    private AgendamentoLockService agendamentoLockService;

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
    @DisplayName("Deve criar agendamento pendente com lock com sucesso")
    void deveCriarAgendamentoPendenteComLockComSucesso() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fim = inicio.plusHours(1);

        AgendamentoCriacaoDTO dto = new AgendamentoCriacaoDTO(1L, 1L, inicio, fim);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(quadraRepository.buscarComLockParaAgendamento(1L)).thenReturn(Optional.of(quadra));
        when(agendamentoRepository.existeConflitoHorario(eq(1L), eq(inicio), eq(fim), eq(StatusAgendamento.CANCELADO)))
                .thenReturn(false);
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(invocation -> {
            Agendamento a = invocation.getArgument(0);
            a.setId_agendamento(10L);
            return a;
        });

        Agendamento resultado = agendamentoLockService.criarAgendamentoPendenteComLock(dto, 1L);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId_agendamento());
        assertEquals(StatusAgendamento.PENDENTE, resultado.getStatus());
        assertEquals(0, BigDecimal.valueOf(100.00).compareTo(resultado.getValorTotal()));
        assertEquals(usuario, resultado.getUsuario());
        assertEquals(quadra, resultado.getQuadra());

        verify(quadraRepository, times(1)).buscarComLockParaAgendamento(1L);
        verify(agendamentoRepository, times(1)).save(any(Agendamento.class));
    }

    @Test
    @DisplayName("Deve falhar ao criar agendamento se usuário não for encontrado")
    void deveFalharSeUsuarioNaoEncontrado() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime fim = inicio.plusHours(1);
        AgendamentoCriacaoDTO dto = new AgendamentoCriacaoDTO(999L, 1L, inicio, fim);

        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agendamentoLockService.criarAgendamentoPendenteComLock(dto, 999L));

        assertTrue(ex.getMessage().contains("Usuário não encontrado"));
        verify(quadraRepository, never()).buscarComLockParaAgendamento(any());
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar ao criar agendamento se quadra não for encontrada")
    void deveFalharSeQuadraNaoEncontrada() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime fim = inicio.plusHours(1);
        AgendamentoCriacaoDTO dto = new AgendamentoCriacaoDTO(1L, 999L, inicio, fim);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(quadraRepository.buscarComLockParaAgendamento(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agendamentoLockService.criarAgendamentoPendenteComLock(dto, 1L));

        assertTrue(ex.getMessage().contains("Quadra não encontrada"));
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar ao criar agendamento se quadra estiver inativa")
    void deveFalharSeQuadraInativa() {
        quadra.setAtiva(false);
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime fim = inicio.plusHours(1);
        AgendamentoCriacaoDTO dto = new AgendamentoCriacaoDTO(1L, 1L, inicio, fim);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(quadraRepository.buscarComLockParaAgendamento(1L)).thenReturn(Optional.of(quadra));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agendamentoLockService.criarAgendamentoPendenteComLock(dto, 1L));

        assertTrue(ex.getMessage().contains("inativa"));
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar ao criar agendamento se houver conflito de horário")
    void deveFalharSeConflitoHorario() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime fim = inicio.plusHours(1);
        AgendamentoCriacaoDTO dto = new AgendamentoCriacaoDTO(1L, 1L, inicio, fim);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(quadraRepository.buscarComLockParaAgendamento(1L)).thenReturn(Optional.of(quadra));
        when(agendamentoRepository.existeConflitoHorario(eq(1L), eq(inicio), eq(fim), eq(StatusAgendamento.CANCELADO)))
                .thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agendamentoLockService.criarAgendamentoPendenteComLock(dto, 1L));

        assertTrue(ex.getMessage().contains("não está disponível"));
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar dados Pix com sucesso")
    void deveAtualizarDadosPixComSucesso() {
        Agendamento agendamento = Agendamento.builder()
                .id_agendamento(10L)
                .usuario(usuario)
                .quadra(quadra)
                .status(StatusAgendamento.PENDENTE)
                .build();

        PagamentoService.PixDados pixDados = new PagamentoService.PixDados("tx-123", "pix-copia-cola", "qr-code-base64");

        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Agendamento resultado = agendamentoLockService.atualizarDadosPix(10L, pixDados);

        assertNotNull(resultado);
        assertEquals("tx-123", resultado.getTransacaoPagamentoId());
        assertEquals("pix-copia-cola", resultado.getPixCopiaECola());
        assertEquals("qr-code-base64", resultado.getQrCodeBase64());
        verify(agendamentoRepository, times(1)).save(agendamento);
    }

    @Test
    @DisplayName("Deve falhar ao atualizar dados Pix se agendamento não for encontrado")
    void deveFalharAoAtualizarDadosPixSeAgendamentoNaoEncontrado() {
        PagamentoService.PixDados pixDados = new PagamentoService.PixDados("tx-123", "pix-copia-cola", "qr-code-base64");

        when(agendamentoRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agendamentoLockService.atualizarDadosPix(999L, pixDados));

        assertTrue(ex.getMessage().contains("Agendamento não encontrado"));
        verify(agendamentoRepository, never()).save(any());
    }
}
