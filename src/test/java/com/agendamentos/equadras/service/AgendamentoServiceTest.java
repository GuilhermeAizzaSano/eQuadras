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

    @Mock
    private AgendamentoLockService agendamentoLockService;

    @Mock
    private com.agendamentos.equadras.repository.BloqueioHorarioRepository bloqueioHorarioRepository;

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
        assertEquals(com.agendamentos.equadras.model.enums.StatusHorario.AGENDADO, slot10as11.status());
        assertEquals("Horário ocupado / agendado", slot10as11.motivo());

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

    @Test
    @DisplayName("Deve listar todos os agendamentos omitindo dados Pix (qrCodeBase64 e pixCopiaECola)")
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
                .status(StatusAgendamento.PENDENTE)
                .transacaoPagamentoId("tx-123")
                .pixCopiaECola("copia-e-cola-pesado")
                .qrCodeBase64("base64-pesado")
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(agendamentoRepository.findByUsuarioId(1L)).thenReturn(List.of(agendamento));

        List<AgendamentoResponseDTO> resultado = agendamentoService.listarTodos(1L);

        assertEquals(1, resultado.size());
        AgendamentoResponseDTO dto = resultado.get(0);
        assertEquals(20L, dto.id_agendamento());
        assertEquals("tx-123", dto.transacaoPagamentoId());
        assertNull(dto.pixCopiaECola());
        assertNull(dto.qrCodeBase64());
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
    @DisplayName("Deve retornar lista vazia quando a quadra não funcionar no dia da semana selecionado")
    void deveRetornarListaVaziaQuandoQuadraFechadaNoDia() {
        quadra.setDisponibilidades(List.of(
                new com.agendamentos.equadras.model.entity.DisponibilidadeDia(java.time.DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0))
        ));

        // Encontrar uma data que não seja MONDAY
        LocalDate dataFechada = LocalDate.now();
        while (dataFechada.getDayOfWeek() == java.time.DayOfWeek.MONDAY) {
            dataFechada = dataFechada.plusDays(1);
        }

        when(quadraRepository.findById(1L)).thenReturn(Optional.of(quadra));

        List<HorarioDisponivelDTO> slots = agendamentoService.listarHorariosDisponiveis(1L, dataFechada);

        assertTrue(slots.isEmpty());
    }

    @Test
    @DisplayName("Deve marcar todos os slots como indisponíveis quando data for após dataLimiteAgendamento")
    void deveMarcarSlotsIndisponiveisQuandoAposDataLimite() {
        LocalDate limite = LocalDate.now().plusDays(2);
        quadra.setDataLimiteAgendamento(limite);
        LocalDate dataConsulta = limite.plusDays(1);

        when(quadraRepository.findById(1L)).thenReturn(Optional.of(quadra));
        when(agendamentoRepository.buscarPorQuadraEData(eq(1L), eq(StatusAgendamento.CANCELADO), any(), any()))
                .thenReturn(List.of());
        when(bloqueioHorarioRepository.findByQuadraIdAndData(1L, dataConsulta))
                .thenReturn(List.of());

        List<HorarioDisponivelDTO> slots = agendamentoService.listarHorariosDisponiveis(1L, dataConsulta);

        assertFalse(slots.isEmpty());
        assertTrue(slots.stream().noneMatch(HorarioDisponivelDTO::disponivel));
        assertTrue(slots.stream().allMatch(s -> "Data limite de agendamento encerrada".equals(s.motivo())));
    }

    @Test
    @DisplayName("Deve marcar todos os slots com motivo do bloqueio quando houver bloqueio de dia inteiro")
    void deveBloquearDiaInteiroQuandoHouverBloqueioTotal() {
        LocalDate dataConsulta = LocalDate.now().plusDays(3);
        com.agendamentos.equadras.model.entity.BloqueioHorario bloqueio = new com.agendamentos.equadras.model.entity.BloqueioHorario(
                quadra,
                dataConsulta,
                null,
                null,
                "Feriado Municipal"
        );

        when(quadraRepository.findById(1L)).thenReturn(Optional.of(quadra));
        when(agendamentoRepository.buscarPorQuadraEData(eq(1L), eq(StatusAgendamento.CANCELADO), any(), any()))
                .thenReturn(List.of());
        when(bloqueioHorarioRepository.findByQuadraIdAndData(1L, dataConsulta))
                .thenReturn(List.of(bloqueio));

        List<HorarioDisponivelDTO> slots = agendamentoService.listarHorariosDisponiveis(1L, dataConsulta);

        assertFalse(slots.isEmpty());
        assertTrue(slots.stream().noneMatch(HorarioDisponivelDTO::disponivel));
        assertTrue(slots.stream().allMatch(s -> "Bloqueado: Feriado Municipal".equals(s.motivo())));
    }

    @Test
    @DisplayName("Deve marcar apenas slots colidentes como indisponíveis quando houver bloqueio parcial")
    void deveBloquearApenasSlotsColidentesNoBloqueioParcial() {
        LocalDate dataConsulta = LocalDate.now().plusDays(3);
        com.agendamentos.equadras.model.entity.BloqueioHorario bloqueio = new com.agendamentos.equadras.model.entity.BloqueioHorario(
                quadra,
                dataConsulta,
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                "Manutenção da rede"
        );

        when(quadraRepository.findById(1L)).thenReturn(Optional.of(quadra));
        when(agendamentoRepository.buscarPorQuadraEData(eq(1L), eq(StatusAgendamento.CANCELADO), any(), any()))
                .thenReturn(List.of());
        when(bloqueioHorarioRepository.findByQuadraIdAndData(1L, dataConsulta))
                .thenReturn(List.of(bloqueio));

        List<HorarioDisponivelDTO> slots = agendamentoService.listarHorariosDisponiveis(1L, dataConsulta);

        HorarioDisponivelDTO slot14 = slots.stream().filter(s -> s.inicio().equals(LocalTime.of(14, 0))).findFirst().orElseThrow();
        HorarioDisponivelDTO slot15 = slots.stream().filter(s -> s.inicio().equals(LocalTime.of(15, 0))).findFirst().orElseThrow();
        HorarioDisponivelDTO slot10 = slots.stream().filter(s -> s.inicio().equals(LocalTime.of(10, 0))).findFirst().orElseThrow();

        assertFalse(slot14.disponivel());
        assertEquals(com.agendamentos.equadras.model.enums.StatusHorario.BLOQUEADO, slot14.status());
        assertEquals("Bloqueado: Manutenção da rede", slot14.motivo());

        assertFalse(slot15.disponivel());
        assertEquals(com.agendamentos.equadras.model.enums.StatusHorario.BLOQUEADO, slot15.status());
        assertEquals("Bloqueado: Manutenção da rede", slot15.motivo());

        assertTrue(slot10.disponivel());
        assertEquals(com.agendamentos.equadras.model.enums.StatusHorario.DISPONIVEL, slot10.status());
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
        when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgendamentoResponseDTO response = agendamentoService.confirmarPagamentoPorWebhook(null, "mp-payment-999");

        assertNotNull(response);
        assertEquals(StatusAgendamento.CONFIRMADO, response.status());
        assertEquals(99L, response.id_agendamento());
        verify(agendamentoRepository, times(1)).save(agendamentoPendente);
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

        when(agendamentoRepository.findById(50L)).thenReturn(Optional.of(agendamento));

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

        when(agendamentoRepository.findAll()).thenReturn(List.of(a1));

        List<AgendamentoResponseDTO> lista = agendamentoService.listarTodos(99L);

        assertEquals(1, lista.size());
        verify(agendamentoRepository, times(1)).findAll();
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
        verify(agendamentoRepository, times(1)).save(agendamento);
    }
}
