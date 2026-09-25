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
    private PagamentoService pagamentoService;

    @Mock
    private AgendamentoLockService agendamentoLockService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private java.time.Clock clock;

    @InjectMocks
    private AgendamentoService agendamentoService;

    private Usuario usuario;
    private Quadra quadra;

    @BeforeEach
    void setUp() {
        lenient().when(clock.getZone()).thenReturn(com.agendamentos.equadras.config.ClockConfig.ZONE_BRASIL);
        lenient().when(clock.instant()).thenAnswer(invocation -> java.time.Instant.now());

        usuario = Usuario.builder()
                .id_usuario(1L)
                .nome_usuario("Carlos")
                .email_usuario("carlos@email.com")
                .role(Role.CLIENT)
                .build();

        List<com.agendamentos.equadras.model.entity.DisponibilidadeDia> disponibilidades = new java.util.ArrayList<>();
        for (java.time.DayOfWeek dia : java.time.DayOfWeek.values()) {
            disponibilidades.add(new com.agendamentos.equadras.model.entity.DisponibilidadeDia(dia, LocalTime.of(6, 0), LocalTime.of(23, 0)));
        }

        quadra = Quadra.builder()
                .id_quadra(1L)
                .nome("Quadra de Tênis")
                .tipoEsporte(TipoEsporte.TENIS)
                .valorHora(BigDecimal.valueOf(100.00))
                .ativa(true)
                .disponibilidades(disponibilidades)
                .build();
    }

    @Test
    @DisplayName("Deve agendar com sucesso quando o horário estiver disponível")
    void deveAgendarComSucesso() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fim = inicio.plusHours(1);

        AgendamentoCriacaoDTO dto = new AgendamentoCriacaoDTO(1L, 1L, inicio, fim);

        Agendamento agendamentoPendente = Agendamento.builder()
                .id_agendamento(10L)
                .usuario(usuario)
                .quadra(quadra)
                .dataHoraInicio(inicio)
                .dataHoraFim(fim)
                .valorTotal(BigDecimal.valueOf(100.00))
                .status(StatusAgendamento.PENDENTE)
                .build();

        Agendamento agendamentoComPix = Agendamento.builder()
                .id_agendamento(10L)
                .usuario(usuario)
                .quadra(quadra)
                .dataHoraInicio(inicio)
                .dataHoraFim(fim)
                .valorTotal(BigDecimal.valueOf(100.00))
                .status(StatusAgendamento.PENDENTE)
                .transacaoPagamentoId("tx-1")
                .pixCopiaECola("pix-copia-cola")
                .qrCodeBase64("qr-code-base64")
                .build();

        PagamentoService.PixDados pixDados = new PagamentoService.PixDados("tx-1", "pix-copia-cola", "qr-code-base64");

        when(agendamentoLockService.criarAgendamentoPendenteComLock(dto, 1L)).thenReturn(agendamentoPendente);
        when(pagamentoService.gerarPix(agendamentoPendente)).thenReturn(pixDados);
        when(agendamentoLockService.atualizarDadosPix(10L, pixDados)).thenReturn(agendamentoComPix);

        AgendamentoResponseDTO resposta = agendamentoService.agendar(dto, 1L);

        assertNotNull(resposta);
        assertEquals(10L, resposta.id_agendamento());
        assertEquals("Quadra de Tênis", resposta.nomeQuadra());
        verify(agendamentoLockService, times(1)).criarAgendamentoPendenteComLock(dto, 1L);
        verify(pagamentoService, times(1)).gerarPix(agendamentoPendente);
        verify(agendamentoLockService, times(1)).atualizarDadosPix(10L, pixDados);
    }

    @Test
    @DisplayName("Deve lançar exceção e bloquear agendamento quando houver conflito de horário")
    void deveBloquearQuandoConflitoDeHorario() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fim = inicio.plusHours(1);

        AgendamentoCriacaoDTO dto = new AgendamentoCriacaoDTO(1L, 1L, inicio, fim);

        when(agendamentoLockService.criarAgendamentoPendenteComLock(dto, 1L))
                .thenThrow(new IllegalArgumentException("Este horário não está disponível para agendamento."));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> agendamentoService.agendar(dto, 1L));
        assertTrue(ex.getMessage().contains("não está disponível"));
        verify(pagamentoService, never()).gerarPix(any());
    }



    @Test
    @DisplayName("Deve listar agendamentos confirmados omitindo dados Pix (qrCodeBase64 e pixCopiaECola)")
    void deveListarTodosOmitindoPix() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime fim = inicio.plusHours(1);

        Agendamento agendamento = Agendamento.builder()
                .id_agendamento(20L)
                .quadra(quadra)
                .usuario(usuario)
                .dataHoraInicio(inicio)
                .dataHoraFim(fim)
                .valorTotal(BigDecimal.valueOf(100.00))
                .status(StatusAgendamento.CONFIRMADO)
                .transacaoPagamentoId("tx-123")
                .pixCopiaECola("copia-e-cola-pesado")
                .qrCodeBase64("base64-pesado")
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(agendamentoRepository.findAtivosByUsuarioId(eq(1L), eq(StatusAgendamento.CANCELADO), any(LocalDateTime.class)))
                .thenReturn(List.of(agendamento));

        List<AgendamentoResponseDTO> resultado = agendamentoService.listarTodos(1L);

        assertEquals(1, resultado.size());
        AgendamentoResponseDTO dto = resultado.get(0);
        assertEquals(20L, dto.id_agendamento());
        assertEquals("tx-123", dto.transacaoPagamentoId());
        assertNull(dto.pixCopiaECola());
        assertNull(dto.qrCodeBase64());
        verify(agendamentoRepository, times(1)).findAtivosByUsuarioId(eq(1L), eq(StatusAgendamento.CANCELADO), any(LocalDateTime.class));
        verify(agendamentoRepository, never()).findByUsuarioId(1L);
    }

    @Test
    @DisplayName("Deve manter dados Pix para agendamentos pendentes do próprio cliente")
    void deveManterPixParaAgendamentoPendenteDoCliente() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime fim = inicio.plusHours(1);

        Agendamento agendamento = Agendamento.builder()
                .id_agendamento(20L)
                .quadra(quadra)
                .usuario(usuario)
                .dataHoraInicio(inicio)
                .dataHoraFim(fim)
                .valorTotal(BigDecimal.valueOf(100.00))
                .status(StatusAgendamento.PENDENTE)
                .transacaoPagamentoId("tx-123")
                .pixCopiaECola("copia-e-cola-pesado")
                .qrCodeBase64("base64-pesado")
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(agendamentoRepository.findAtivosByUsuarioId(eq(1L), eq(StatusAgendamento.CANCELADO), any(LocalDateTime.class)))
                .thenReturn(List.of(agendamento));

        List<AgendamentoResponseDTO> resultado = agendamentoService.listarTodos(1L);

        assertEquals(1, resultado.size());
        AgendamentoResponseDTO dto = resultado.get(0);
        assertEquals(20L, dto.id_agendamento());
        assertEquals("copia-e-cola-pesado", dto.pixCopiaECola());
        assertEquals("base64-pesado", dto.qrCodeBase64());
    }

    @Test
    @DisplayName("Deve listar histórico completo do cliente quando historico for true")
    void deveListarHistoricoCompletoQuandoHistoricoTrue() {
        Agendamento agendamento = Agendamento.builder()
                .id_agendamento(21L)
                .quadra(quadra)
                .usuario(usuario)
                .dataHoraInicio(LocalDateTime.now().minusDays(2))
                .dataHoraFim(LocalDateTime.now().minusDays(2).plusHours(1))
                .valorTotal(BigDecimal.valueOf(80.00))
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(agendamentoRepository.findByUsuarioId(1L)).thenReturn(List.of(agendamento));

        List<AgendamentoResponseDTO> resultado = agendamentoService.listarTodos(1L, true);

        assertEquals(1, resultado.size());
        assertEquals(21L, resultado.get(0).id_agendamento());
        verify(agendamentoRepository, times(1)).findByUsuarioId(1L);
        verify(agendamentoRepository, never()).findAtivosByUsuarioId(any(), any(), any());
    }

    @Test
    @DisplayName("Deve listar agendamentos por quadra e data omitindo dados Pix")
    void deveListarPorQuadraEDataOmitindoPix() {
        LocalDate dataAmanha = LocalDate.now().plusDays(1);
        LocalDateTime inicio = dataAmanha.atTime(14, 0);
        LocalDateTime fim = dataAmanha.atTime(15, 0);

        Agendamento agendamento = Agendamento.builder()
                .id_agendamento(30L)
                .quadra(quadra)
                .usuario(usuario)
                .dataHoraInicio(inicio)
                .dataHoraFim(fim)
                .valorTotal(BigDecimal.valueOf(100.00))
                .status(StatusAgendamento.CONFIRMADO)
                .transacaoPagamentoId("tx-456")
                .pixCopiaECola("copia-e-cola-pesado")
                .qrCodeBase64("base64-pesado")
                .build();

        when(agendamentoRepository.buscarPorQuadraEData(eq(1L), eq(StatusAgendamento.CANCELADO), any(), any()))
                .thenReturn(List.of(agendamento));

        List<AgendamentoResponseDTO> resultado = agendamentoService.listarPorQuadraEData(1L, dataAmanha);

        assertEquals(1, resultado.size());
        AgendamentoResponseDTO dto = resultado.get(0);
        assertEquals(30L, dto.id_agendamento());
        assertEquals("tx-456", dto.transacaoPagamentoId());
        assertNull(dto.pixCopiaECola());
        assertNull(dto.qrCodeBase64());
    }



    @Test
    @DisplayName("Deve confirmar pagamento via webhook localizando por transacaoPagamentoId")
    void deveConfirmarPagamentoPorWebhookComSucesso() {
        Agendamento agendamentoPendente = Agendamento.builder()
                .id_agendamento(99L)
                .quadra(quadra)
                .usuario(usuario)
                .dataHoraInicio(LocalDateTime.now().plusDays(1).withHour(18).withMinute(0))
                .dataHoraFim(LocalDateTime.now().plusDays(1).withHour(19).withMinute(0))
                .valorTotal(BigDecimal.valueOf(100.00))
                .status(StatusAgendamento.PENDENTE)
                .transacaoPagamentoId("mp-payment-999")
                .build();

        when(agendamentoRepository.findByTransacaoPagamentoId("mp-payment-999"))
                .thenReturn(Optional.of(agendamentoPendente));
        when(agendamentoRepository.confirmarPagamentoPendente(eq(99L), eq("mp-payment-999"), eq(StatusAgendamento.CONFIRMADO), eq(StatusAgendamento.PENDENTE)))
                .thenReturn(1);

        AgendamentoResponseDTO response = agendamentoService.confirmarPagamentoPorWebhook(null, "mp-payment-999");

        assertNotNull(response);
        assertEquals(StatusAgendamento.CONFIRMADO, response.status());
        assertEquals(99L, response.id_agendamento());
        verify(agendamentoRepository, times(1)).confirmarPagamentoPendente(99L, "mp-payment-999", StatusAgendamento.CONFIRMADO, StatusAgendamento.PENDENTE);
    }

    @Test
    @DisplayName("Deve buscar agendamento por ID para usuário dono")
    void deveBuscarAgendamentoPorId() {
        Agendamento agendamento = Agendamento.builder()
                .id_agendamento(50L)
                .quadra(quadra)
                .usuario(usuario)
                .dataHoraInicio(LocalDateTime.now().plusDays(1).withHour(15).withMinute(0))
                .dataHoraFim(LocalDateTime.now().plusDays(1).withHour(16).withMinute(0))
                .valorTotal(BigDecimal.valueOf(100.00))
                .status(StatusAgendamento.PENDENTE)
                .build();

        when(agendamentoRepository.buscarPorIdEEscopo(50L, 1L)).thenReturn(Optional.of(agendamento));

        AgendamentoResponseDTO dto = agendamentoService.buscarPorId(50L, 1L);

        assertNotNull(dto);
        assertEquals(50L, dto.id_agendamento());
        assertEquals(1L, dto.usuarioId());
    }

    @Test
    @DisplayName("Deve listar todos os agendamentos de todas as quadras quando for Master Admin")
    void deveListarTodosAgendamentosQuandoMasterAdmin() {
        Usuario masterAdmin = Usuario.builder()
                .id_usuario(99L)
                .nome_usuario("Master")
                .email_usuario("gui@gmail.com")
                .role(Role.ADMIN)
                .build();
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(masterAdmin));

        Agendamento a1 = Agendamento.builder()
                .id_agendamento(1L)
                .quadra(quadra)
                .usuario(usuario)
                .dataHoraInicio(LocalDateTime.now().plusDays(1))
                .dataHoraFim(LocalDateTime.now().plusDays(1).plusHours(1))
                .valorTotal(BigDecimal.valueOf(100.00))
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        when(agendamentoRepository.findAllOrderByDataHoraInicioDesc()).thenReturn(List.of(a1));

        List<AgendamentoResponseDTO> lista = agendamentoService.listarTodos(99L, true);

        assertEquals(1, lista.size());
        verify(agendamentoRepository, times(1)).findAllOrderByDataHoraInicioDesc();
        verify(agendamentoRepository, never()).findByAdminId(99L);
    }

    @Test
    @DisplayName("Deve permitir ao Master Admin cancelar qualquer agendamento")
    void devePermitirMasterAdminCancelarQualquerAgendamento() {
        Usuario masterAdmin = Usuario.builder()
                .id_usuario(99L)
                .nome_usuario("Master")
                .email_usuario("gui@gmail.com")
                .role(Role.ADMIN)
                .build();
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(masterAdmin));

        Agendamento agendamento = Agendamento.builder()
                .id_agendamento(50L)
                .quadra(quadra)
                .usuario(usuario)
                .dataHoraInicio(LocalDateTime.now().plusDays(1).withHour(15).withMinute(0))
                .dataHoraFim(LocalDateTime.now().plusDays(1).withHour(16).withMinute(0))
                .valorTotal(BigDecimal.valueOf(100.00))
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        when(agendamentoRepository.findById(50L)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(i -> i.getArgument(0));

        AgendamentoResponseDTO response = agendamentoService.cancelar(50L, 99L);

        assertNotNull(response);
        assertEquals(StatusAgendamento.CANCELADO, response.status());
        assertNotNull(response.canceladoEm());
        assertNotNull(agendamento.getCanceladoEm());
        verify(agendamentoRepository, times(1)).save(agendamento);
    }

    @Test
    @DisplayName("Cancelamento deve usar o Clock injetado como referência de 'agora'")
    void cancelarDeveUsarClockInjetado() {
        LocalDateTime inicioReserva = LocalDateTime.of(2020, 1, 1, 10, 0);
        LocalDateTime agoraFixo = inicioReserva.minusHours(2);
        when(clock.instant()).thenReturn(agoraFixo.atZone(com.agendamentos.equadras.config.ClockConfig.ZONE_BRASIL).toInstant());
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        Agendamento agendamento = Agendamento.builder()
                .id_agendamento(60L)
                .quadra(quadra)
                .usuario(usuario)
                .dataHoraInicio(inicioReserva)
                .dataHoraFim(inicioReserva.plusHours(1))
                .valorTotal(BigDecimal.valueOf(100.00))
                .status(StatusAgendamento.CONFIRMADO)
                .build();
        when(agendamentoRepository.findById(60L)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(i -> i.getArgument(0));

        AgendamentoResponseDTO response = agendamentoService.cancelar(60L, 1L);

        assertEquals(StatusAgendamento.CANCELADO, response.status());
        assertEquals(agoraFixo, agendamento.getCanceladoEm());
    }

    @Test
    @DisplayName("Deve listar agendamentos por quadra quando o usuário for o dono da quadra")
    void deveListarPorQuadraQuandoForDono() {
        Usuario admin = Usuario.builder()
                .id_usuario(2L)
                .nome_usuario("Admin")
                .role(Role.ADMIN)
                .build();
        quadra.setAdmin(admin);

        when(quadraRepository.findById(1L)).thenReturn(Optional.of(quadra));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(admin));

        Agendamento ag = Agendamento.builder()
                .id_agendamento(100L)
                .quadra(quadra)
                .usuario(usuario)
                .dataHoraInicio(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0))
                .dataHoraFim(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0))
                .valorTotal(BigDecimal.valueOf(100.00))
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        when(agendamentoRepository.findByQuadraIdOrderByDataHoraInicioDesc(1L)).thenReturn(List.of(ag));

        List<AgendamentoResponseDTO> resultado = agendamentoService.listarPorQuadra(1L, 2L);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(100L, resultado.get(0).id_agendamento());
        verify(agendamentoRepository, times(1)).findByQuadraIdOrderByDataHoraInicioDesc(1L);
    }

    @Test
    @DisplayName("Deve listar agendamentos por quadra quando o usuário for Master Admin")
    void deveListarPorQuadraQuandoForMasterAdmin() {
        Usuario masterAdmin = Usuario.builder()
                .id_usuario(99L)
                .nome_usuario("Master")
                .email_usuario("gui@gmail.com")
                .role(Role.ADMIN)
                .build();

        when(quadraRepository.findById(1L)).thenReturn(Optional.of(quadra));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(masterAdmin));
        when(agendamentoRepository.findByQuadraIdOrderByDataHoraInicioDesc(1L)).thenReturn(List.of());

        List<AgendamentoResponseDTO> resultado = agendamentoService.listarPorQuadra(1L, 99L);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verify(agendamentoRepository, times(1)).findByQuadraIdOrderByDataHoraInicioDesc(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao listar por quadra se usuário não for dono nem Master Admin")
    void deveLancarExcecaoAoListarPorQuadraSeNaoAutorizado() {
        Usuario outroAdmin = Usuario.builder()
                .id_usuario(3L)
                .nome_usuario("Outro Admin")
                .email_usuario("outro@email.com")
                .role(Role.ADMIN)
                .build();

        Usuario adminDono = Usuario.builder()
                .id_usuario(2L)
                .nome_usuario("Dono")
                .email_usuario("dono@email.com")
                .role(Role.ADMIN)
                .build();
        quadra.setAdmin(adminDono);

        when(quadraRepository.findById(1L)).thenReturn(Optional.of(quadra));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(outroAdmin));

        org.springframework.security.access.AccessDeniedException ex = assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                agendamentoService.listarPorQuadra(1L, 3L)
        );

        assertEquals("Você não tem permissão para visualizar o histórico desta quadra.", ex.getMessage());
        verify(agendamentoRepository, never()).findByQuadraIdOrderByDataHoraInicioDesc(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando a quadra não for encontrada ao listar por quadra")
    void deveLancarExcecaoQuandoQuadraNaoEncontradaAoListarPorQuadra() {
        when(quadraRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                agendamentoService.listarPorQuadra(999L, 1L)
        );

        assertEquals("Quadra não encontrada para o ID: 999", ex.getMessage());
    }





    @Test
    @DisplayName("Deve verificar se quadra possui agendamentos delegando para o repository")
    void deveVerificarSePossuiAgendamentos() {
        when(agendamentoRepository.existsByQuadraId(1L)).thenReturn(true);
        when(agendamentoRepository.existsByQuadraId(2L)).thenReturn(false);

        assertTrue(agendamentoService.possuiAgendamentos(1L));
        assertFalse(agendamentoService.possuiAgendamentos(2L));
        assertFalse(agendamentoService.possuiAgendamentos(null));

        verify(agendamentoRepository, times(1)).existsByQuadraId(1L);
        verify(agendamentoRepository, times(1)).existsByQuadraId(2L);
        verify(agendamentoRepository, never()).existsByQuadraId(null);
    }

    @Test
    @DisplayName("Deve verificar se existe conflito de horário para reservas da quadra")
    void deveVerificarSeExisteConflitoHorario() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        LocalDateTime fim = inicio.plusHours(1);
        when(agendamentoRepository.existeConflitoHorario(1L, inicio, fim, StatusAgendamento.CANCELADO)).thenReturn(true);

        boolean conflito = agendamentoService.existeConflitoHorario(1L, inicio, fim);

        assertTrue(conflito);
        verify(agendamentoRepository).existeConflitoHorario(1L, inicio, fim, StatusAgendamento.CANCELADO);
    }
}
