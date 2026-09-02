package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.BloqueioHorarioCriacaoDTO;
import com.agendamentos.equadras.dto.response.BloqueioHorarioResponseDTO;
import com.agendamentos.equadras.model.entity.BloqueioHorario;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.BloqueioHorarioRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BloqueioHorarioServiceTest {

    @Mock
    private BloqueioHorarioRepository bloqueioHorarioRepository;

    @Mock
    private QuadraRepository quadraRepository;

    @InjectMocks
    private BloqueioHorarioService bloqueioHorarioService;

    private Usuario admin;
    private Quadra quadra;

    @BeforeEach
    void setUp() {
        admin = Usuario.builder()
                .id_usuario(1L)
                .nome_usuario("Admin Silva")
                .email_usuario("admin@equadras.com")
                .role(Role.ADMIN)
                .build();

        quadra = Quadra.builder()
                .id_quadra(10L)
                .nome("Quadra Central")
                .tipoEsporte(TipoEsporte.FUTEBOL)
                .valorHora(BigDecimal.valueOf(120.00))
                .ativa(true)
                .admin(admin)
                .build();
    }

    @Test
    @DisplayName("Deve criar bloqueio de dia inteiro com sucesso")
    void deveCriarBloqueioDiaInteiroComSucesso() {
        LocalDate dataBloqueio = LocalDate.now().plusDays(2);
        BloqueioHorarioCriacaoDTO dto = new BloqueioHorarioCriacaoDTO(dataBloqueio, null, null, "Reforma no piso");

        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));
        when(bloqueioHorarioRepository.save(any(BloqueioHorario.class))).thenAnswer(invocation -> {
            BloqueioHorario b = invocation.getArgument(0);
            b.setId(100L);
            return b;
        });

        BloqueioHorarioResponseDTO resultado = bloqueioHorarioService.criarBloqueio(10L, dto, 1L);

        assertNotNull(resultado);
        assertEquals(100L, resultado.id());
        assertEquals(10L, resultado.quadraId());
        assertEquals(dataBloqueio, resultado.data());
        assertNull(resultado.horaInicio());
        assertNull(resultado.horaFim());
        assertEquals("Reforma no piso", resultado.motivo());
        verify(bloqueioHorarioRepository, times(1)).save(any(BloqueioHorario.class));
    }

    @Test
    @DisplayName("Deve criar bloqueio com horários válidos com sucesso")
    void deveCriarBloqueioComHorariosComSucesso() {
        LocalDate dataBloqueio = LocalDate.now().plusDays(2);
        LocalTime inicio = LocalTime.of(14, 0);
        LocalTime fim = LocalTime.of(18, 0);
        BloqueioHorarioCriacaoDTO dto = new BloqueioHorarioCriacaoDTO(dataBloqueio, inicio, fim, "Torneio interno");

        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));
        when(bloqueioHorarioRepository.save(any(BloqueioHorario.class))).thenAnswer(invocation -> {
            BloqueioHorario b = invocation.getArgument(0);
            b.setId(101L);
            return b;
        });

        BloqueioHorarioResponseDTO resultado = bloqueioHorarioService.criarBloqueio(10L, dto, 1L);

        assertNotNull(resultado);
        assertEquals(101L, resultado.id());
        assertEquals(inicio, resultado.horaInicio());
        assertEquals(fim, resultado.horaFim());
    }

    @Test
    @DisplayName("Deve falhar ao criar bloqueio se admin não for dono da quadra")
    void deveFalharSeAdminNaoForDono() {
        LocalDate dataBloqueio = LocalDate.now().plusDays(2);
        BloqueioHorarioCriacaoDTO dto = new BloqueioHorarioCriacaoDTO(dataBloqueio, null, null, "Evento");

        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bloqueioHorarioService.criarBloqueio(10L, dto, 999L));

        assertTrue(ex.getMessage().contains("Apenas o administrador dono da quadra"));
        verify(bloqueioHorarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar ao criar bloqueio com data no passado")
    void deveFalharSeDataNoPassado() {
        LocalDate dataPassada = LocalDate.now().minusDays(1);
        BloqueioHorarioCriacaoDTO dto = new BloqueioHorarioCriacaoDTO(dataPassada, null, null, "Passado");

        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bloqueioHorarioService.criarBloqueio(10L, dto, 1L));

        assertTrue(ex.getMessage().contains("não pode ser no passado"));
        verify(bloqueioHorarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar ao criar bloqueio com hora início posterior a hora fim")
    void deveFalharSeHoraInicioPosteriorAFim() {
        LocalDate dataBloqueio = LocalDate.now().plusDays(1);
        BloqueioHorarioCriacaoDTO dto = new BloqueioHorarioCriacaoDTO(dataBloqueio, LocalTime.of(18, 0), LocalTime.of(14, 0), "Inválido");

        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bloqueioHorarioService.criarBloqueio(10L, dto, 1L));

        assertTrue(ex.getMessage().contains("anterior à hora de término"));
        verify(bloqueioHorarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve listar bloqueios da quadra")
    void deveListarBloqueios() {
        BloqueioHorario b1 = new BloqueioHorario(1L, quadra, LocalDate.now().plusDays(1), null, null, "Feriado", null);
        when(bloqueioHorarioRepository.findByQuadraId(10L)).thenReturn(List.of(b1));

        List<BloqueioHorarioResponseDTO> lista = bloqueioHorarioService.listarBloqueios(10L);

        assertEquals(1, lista.size());
        assertEquals("Feriado", lista.get(0).motivo());
    }

    @Test
    @DisplayName("Deve remover bloqueio com sucesso")
    void deveRemoverBloqueioComSucesso() {
        BloqueioHorario b = new BloqueioHorario(50L, quadra, LocalDate.now().plusDays(1), null, null, "Motivo", null);

        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));
        when(bloqueioHorarioRepository.findById(50L)).thenReturn(Optional.of(b));

        bloqueioHorarioService.removerBloqueio(10L, 50L, 1L);

        verify(bloqueioHorarioRepository, times(1)).delete(b);
    }

    @Test
    @DisplayName("Deve desbloquear horários por data e horários com sucesso")
    void deveDesbloquearHorariosPorDataComSucesso() {
        LocalDate dataBloqueio = LocalDate.now().plusDays(2);
        BloqueioHorario b = new BloqueioHorario(50L, quadra, dataBloqueio, LocalTime.of(14, 0), LocalTime.of(16, 0), "Motivo", null);

        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));
        when(bloqueioHorarioRepository.findByQuadraIdAndData(10L, dataBloqueio)).thenReturn(List.of(b));

        com.agendamentos.equadras.dto.request.DesbloqueioHorarioDTO dto =
                new com.agendamentos.equadras.dto.request.DesbloqueioHorarioDTO(null, dataBloqueio, LocalTime.of(14, 0), LocalTime.of(16, 0));

        int removidos = bloqueioHorarioService.desbloquearHorarios(10L, dto, 1L);

        assertEquals(1, removidos);
        verify(bloqueioHorarioRepository, times(1)).deleteAll(anyList());
    }
}
