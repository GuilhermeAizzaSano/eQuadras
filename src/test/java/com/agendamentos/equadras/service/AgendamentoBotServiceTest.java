package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.AgendamentoBotRequestDTO;
import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoBotServiceTest {

    @Mock
    private QuadraBuscaService quadraBuscaService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private AgendamentoService agendamentoService;

    @Mock
    private Clock clock;

    @InjectMocks
    private AgendamentoBotService agendamentoBotService;

    private Usuario usuario;
    private Quadra quadra;

    @BeforeEach
    void setUp() {
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("America/Sao_Paulo"));
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-09-25T10:00:00Z"));

        usuario = Usuario.builder()
                .id_usuario(1L)
                .nome_usuario("Robson")
                .role(Role.CLIENT)
                .build();

        quadra = Quadra.builder()
                .id_quadra(10L)
                .nome("Quadra Society Central")
                .tipoEsporte(TipoEsporte.FUTEBOL)
                .valorHora(BigDecimal.valueOf(120.00))
                .ativa(true)
                .build();
    }

    @Test
    @DisplayName("Deve agendar com sucesso quando quadraId for fornecido explicitamente")
    void deveAgendarComQuadraIdInformado() {
        when(usuarioService.obterOuCriarUsuarioBot("Robson", "11999998888")).thenReturn(usuario);

        AgendamentoResponseDTO esperado = new AgendamentoResponseDTO(
                100L, 1L, "Robson", "11999998888", 10L, "Quadra Society Central",
                LocalDateTime.of(2026, 9, 26, 19, 0),
                LocalDateTime.of(2026, 9, 26, 20, 0),
                BigDecimal.valueOf(120.00), null, null, null, null, null, null
        );

        when(agendamentoService.agendar(any(AgendamentoCriacaoDTO.class), eq(1L))).thenReturn(esperado);

        AgendamentoBotRequestDTO dto = new AgendamentoBotRequestDTO(
                10L, null, null, "2026-09-26", "19:00", "20:00", "Robson", "11999998888"
        );

        AgendamentoResponseDTO resultado = agendamentoBotService.agendarViaBot(dto, false);

        assertNotNull(resultado);
        assertEquals(100L, resultado.id_agendamento());
        verify(agendamentoService, times(1)).agendar(any(AgendamentoCriacaoDTO.class), eq(1L));
        verify(quadraBuscaService, never()).buscarQuadrasAtivas(any(), any(), any());
    }

    @Test
    @DisplayName("Deve resolver quadra ativa via specification quando quadraId for nulo")
    void deveResolverQuadraPorNomeOuEsporte() {
        when(quadraBuscaService.buscarQuadrasAtivas(null, "Futebol", "Society")).thenReturn(List.of(quadra));
        when(usuarioService.obterOuCriarUsuarioBot("Robson", "11999998888")).thenReturn(usuario);

        AgendamentoResponseDTO esperado = new AgendamentoResponseDTO(
                101L, 1L, "Robson", "11999998888", 10L, "Quadra Society Central",
                LocalDateTime.of(2026, 9, 26, 19, 0),
                LocalDateTime.of(2026, 9, 26, 20, 0),
                BigDecimal.valueOf(120.00), null, null, null, null, null, null
        );

        when(agendamentoService.agendar(any(AgendamentoCriacaoDTO.class), eq(1L))).thenReturn(esperado);

        AgendamentoBotRequestDTO dto = new AgendamentoBotRequestDTO(
                null, "Society", "Futebol", "2026-09-26", "19:00", null, "Robson", "11999998888"
        );

        AgendamentoResponseDTO resultado = agendamentoBotService.agendarViaBot(dto, false);

        assertNotNull(resultado);
        assertEquals(101L, resultado.id_agendamento());
        verify(quadraBuscaService, times(1)).buscarQuadrasAtivas(null, "Futebol", "Society");
    }

    @Test
    @DisplayName("Deve lançar exceção quando nenhuma quadra for encontrada")
    void deveFalharSeNenhumaQuadraEncontrada() {
        when(quadraBuscaService.buscarQuadrasAtivas(null, null, "Inexistente")).thenReturn(List.of());

        AgendamentoBotRequestDTO dto = new AgendamentoBotRequestDTO(
                null, "Inexistente", null, "2026-09-26", "19:00", null, "Robson", "11999998888"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                agendamentoBotService.agendarViaBot(dto, false));

        assertEquals("Nenhuma quadra encontrada para o esporte ou nome informado.", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção se hora início for igual ou posterior à hora fim")
    void deveFalharSeHoraInicioMaiorOuIgualFim() {
        AgendamentoBotRequestDTO dto = new AgendamentoBotRequestDTO(
                10L, null, null, "2026-09-26", "20:00", "19:00", "Robson", "11999998888"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                agendamentoBotService.agendarViaBot(dto, false));

        assertEquals("Hora de início deve ser anterior à hora de término.", ex.getMessage());
    }

    @ParameterizedTest(name = "\"{0}\" vira {1}")
    @DisplayName("Deve interpretar os formatos de hora aceitos pelo bot")
    @CsvSource(delimiter = '|', value = {
            "19:00|19:00", "19|19:00", "7|07:00", "19h|19:00", "9h|09:00", "19 h|19:00", " 19 |19:00",
            "19H|19:00", "19h00|19:00", "19h30|19:30", "9:00|09:00", "9:30|09:30", "19:00:00|19:00", "1900|19:00"
    })
    void deveAceitarFormatosDeHoraDoBot(String horaEntrada, LocalTime horaEsperada) {
        when(usuarioService.obterOuCriarUsuarioBot("Robson", "11999998888")).thenReturn(usuario);

        agendamentoBotService.agendarViaBot(new AgendamentoBotRequestDTO(
                10L, null, null, "2026-09-26", horaEntrada, null, "Robson", "11999998888"), false);

        ArgumentCaptor<AgendamentoCriacaoDTO> captor = ArgumentCaptor.forClass(AgendamentoCriacaoDTO.class);
        verify(agendamentoService).agendar(captor.capture(), eq(1L));
        assertEquals(LocalDate.of(2026, 9, 26).atTime(horaEsperada), captor.getValue().dataHoraInicio());
        assertEquals(LocalDate.of(2026, 9, 26).atTime(horaEsperada.plusHours(1)), captor.getValue().dataHoraFim());
    }

    @Test
    @DisplayName("Com confirmarDireto, cria a reserva confirmada sem passar pelo fluxo Pix")
    void deveAgendarConfirmadoQuandoConfirmarDireto() {
        when(usuarioService.obterOuCriarUsuarioBot("Robson", "11999998888")).thenReturn(usuario);

        agendamentoBotService.agendarViaBot(new AgendamentoBotRequestDTO(
                10L, null, null, "2026-09-26", "19h", null, "Robson", "11999998888"), true);

        verify(agendamentoService).agendarConfirmado(any(AgendamentoCriacaoDTO.class), eq(1L));
        verify(agendamentoService, never()).agendar(any(), any());
    }

    @ParameterizedTest(name = "\"{0}\" é rejeitada")
    @DisplayName("Deve rejeitar horas inválidas com mensagem clara")
    @ValueSource(strings = {"abc", "25h", "19:60", "24", "9h5", "19h.", "", "19:"})
    void deveRejeitarHoraInvalida(String horaEntrada) {
        AgendamentoBotRequestDTO dto = new AgendamentoBotRequestDTO(
                10L, null, null, "2026-09-26", horaEntrada, null, "Robson", "11999998888");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                agendamentoBotService.agendarViaBot(dto, false));

        assertTrue(ex.getMessage().contains("hora") || ex.getMessage().contains("Hora"), ex.getMessage());
    }
}
