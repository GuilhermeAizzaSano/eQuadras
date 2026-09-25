package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.AgendamentoBotRequestDTO;
import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.QuadraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

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
    private QuadraRepository quadraRepository;

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

        AgendamentoResponseDTO resultado = agendamentoBotService.agendarViaBot(dto);

        assertNotNull(resultado);
        assertEquals(100L, resultado.id_agendamento());
        verify(agendamentoService, times(1)).agendar(any(AgendamentoCriacaoDTO.class), eq(1L));
        verify(quadraRepository, never()).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve resolver quadra ativa via specification quando quadraId for nulo")
    void deveResolverQuadraPorNomeOuEsporte() {
        when(quadraRepository.findAll(any(Specification.class))).thenReturn(List.of(quadra));
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

        AgendamentoResponseDTO resultado = agendamentoBotService.agendarViaBot(dto);

        assertNotNull(resultado);
        assertEquals(101L, resultado.id_agendamento());
        verify(quadraRepository, times(1)).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando nenhuma quadra for encontrada")
    void deveFalharSeNenhumaQuadraEncontrada() {
        when(quadraRepository.findAll(any(Specification.class))).thenReturn(List.of());

        AgendamentoBotRequestDTO dto = new AgendamentoBotRequestDTO(
                null, "Inexistente", null, "2026-09-26", "19:00", null, "Robson", "11999998888"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                agendamentoBotService.agendarViaBot(dto));

        assertEquals("Nenhuma quadra encontrada para o esporte ou nome informado.", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção se hora início for igual ou posterior à hora fim")
    void deveFalharSeHoraInicioMaiorOuIgualFim() {
        AgendamentoBotRequestDTO dto = new AgendamentoBotRequestDTO(
                10L, null, null, "2026-09-26", "20:00", "19:00", "Robson", "11999998888"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                agendamentoBotService.agendarViaBot(dto));

        assertEquals("Hora de início deve ser anterior à hora de término.", ex.getMessage());
    }
}
