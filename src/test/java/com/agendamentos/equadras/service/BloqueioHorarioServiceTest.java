package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.BloqueioHorarioCriacaoDTO;
import com.agendamentos.equadras.dto.response.BloqueioHorarioResponseDTO;
import com.agendamentos.equadras.model.entity.BloqueioHorario;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.AgendamentoRepository;
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

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private AuditoriaService auditoriaService;

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

        lenient().when(usuarioService.isMasterAdmin(1L)).thenReturn(false);
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

        org.springframework.security.access.AccessDeniedException ex = assertThrows(org.springframework.security.access.AccessDeniedException.class,
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
        when(bloqueioHorarioRepository.findByQuadraId(eq(10L), any(LocalDate.class))).thenReturn(List.of(b1));

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

    @Test
    @DisplayName("Deve lançar erro ao tentar criar bloqueio de horário quando dia todo já estiver bloqueado sem confirmação")
    void deveLancarErroAoBloquearHorarioComDiaTodoBloqueado() {
        LocalDate dataBloqueio = LocalDate.now().plusDays(2);
        BloqueioHorario diaTodo = new BloqueioHorario(50L, quadra, dataBloqueio, null, null, "Dia Todo", null);

        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));
        when(bloqueioHorarioRepository.findByQuadraIdAndData(10L, dataBloqueio)).thenReturn(List.of(diaTodo));

        BloqueioHorarioCriacaoDTO dto = new BloqueioHorarioCriacaoDTO(
                dataBloqueio, LocalTime.of(14, 0), LocalTime.of(16, 0), "Parcial", false
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                bloqueioHorarioService.criarBloqueio(10L, dto, 1L)
        );

        assertTrue(ex.getMessage().contains("DIA_INTEIRO_BLOQUEADO"));
        verify(bloqueioHorarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve substituir bloqueio de dia todo por horário parcial quando confirmado")
    void deveSubstituirDiaTodoPorHorarioParcialQuandoConfirmado() {
        LocalDate dataBloqueio = LocalDate.now().plusDays(2);
        BloqueioHorario diaTodo = new BloqueioHorario(50L, quadra, dataBloqueio, null, null, "Dia Todo", null);
        BloqueioHorario novoParcial = new BloqueioHorario(51L, quadra, dataBloqueio, LocalTime.of(14, 0), LocalTime.of(16, 0), "Parcial", null);

        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));
        when(bloqueioHorarioRepository.findByQuadraIdAndData(10L, dataBloqueio)).thenReturn(List.of(diaTodo));
        when(bloqueioHorarioRepository.save(any(BloqueioHorario.class))).thenReturn(novoParcial);

        BloqueioHorarioCriacaoDTO dto = new BloqueioHorarioCriacaoDTO(
                dataBloqueio, LocalTime.of(14, 0), LocalTime.of(16, 0), "Parcial", true
        );

        BloqueioHorarioResponseDTO res = bloqueioHorarioService.criarBloqueio(10L, dto, 1L);

        assertNotNull(res);
        verify(bloqueioHorarioRepository, times(1)).deleteAll(List.of(diaTodo));
        verify(bloqueioHorarioRepository, times(1)).save(any(BloqueioHorario.class));
    }

    @Test
    @DisplayName("Deve listar todos os bloqueios do admin em lote")
    void deveListarTodosDoAdmin() {
        BloqueioHorario b1 = new BloqueioHorario(50L, quadra, LocalDate.now().plusDays(1), null, null, "B1", null);
        when(bloqueioHorarioRepository.findAllByAdminId(eq(1L), any(LocalDate.class))).thenReturn(List.of(b1));

        List<BloqueioHorarioResponseDTO> lista = bloqueioHorarioService.listarTodosDoAdmin(1L);

        assertEquals(1, lista.size());
        assertEquals("B1", lista.get(0).motivo());
        verify(bloqueioHorarioRepository, times(1)).findAllByAdminId(eq(1L), any(LocalDate.class));
    }

    @Test
    @DisplayName("Deve listar todos os bloqueios de todas as quadras quando for Master Admin")
    void deveListarTodosQuandoMasterAdmin() {
        when(usuarioService.isMasterAdmin(99L)).thenReturn(true);

        BloqueioHorario bGlobal = new BloqueioHorario(51L, quadra, LocalDate.now().plusDays(1), null, null, "Global", null);
        when(bloqueioHorarioRepository.findAllOrdered(any(LocalDate.class))).thenReturn(List.of(bGlobal));

        List<BloqueioHorarioResponseDTO> lista = bloqueioHorarioService.listarTodosDoAdmin(99L);

        assertEquals(1, lista.size());
        assertEquals("Global", lista.get(0).motivo());
        verify(bloqueioHorarioRepository, times(1)).findAllOrdered(any(LocalDate.class));
        verify(bloqueioHorarioRepository, never()).findAllByAdminId(eq(99L), any(LocalDate.class));
    }

    @Test
    @DisplayName("Deve permitir ao Master Admin remover bloqueio de quadra de outro admin")
    void devePermitirMasterAdminRemoverBloqueio() {
        when(usuarioService.isMasterAdmin(99L)).thenReturn(true);

        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));
        BloqueioHorario bloqueio = new BloqueioHorario(200L, quadra, LocalDate.now().plusDays(2), null, null, "Motivo", null);
        when(bloqueioHorarioRepository.findById(200L)).thenReturn(Optional.of(bloqueio));

        bloqueioHorarioService.removerBloqueio(10L, 200L, 99L);

        verify(bloqueioHorarioRepository, times(1)).delete(bloqueio);
    }

    @Test
    @DisplayName("Deve falhar ao criar bloqueio se houver agendamento ativo no período")
    void deveFalharSeHouverConflitoComReservaAtiva() {
        LocalDate dataBloqueio = LocalDate.now().plusDays(2);
        LocalTime inicio = LocalTime.of(14, 0);
        LocalTime fim = LocalTime.of(16, 0);
        BloqueioHorarioCriacaoDTO dto = new BloqueioHorarioCriacaoDTO(dataBloqueio, inicio, fim, "Manutenção");

        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));
        when(agendamentoRepository.existeConflitoHorario(eq(10L), any(), any(), any())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                bloqueioHorarioService.criarBloqueio(10L, dto, 1L));

        assertTrue(ex.getMessage().contains("já existem reservas ativas"));
        verify(bloqueioHorarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar ao criar bloqueio de dia inteiro se já existir um bloqueio de dia inteiro")
    void deveFalharAoCriarBloqueioDiaInteiroSeJaExistir() {
        LocalDate dataBloqueio = LocalDate.now().plusDays(2);
        BloqueioHorarioCriacaoDTO dto = new BloqueioHorarioCriacaoDTO(dataBloqueio, null, null, "Torneio");

        when(quadraRepository.buscarComLockParaAgendamento(10L)).thenReturn(Optional.of(quadra));
        when(agendamentoRepository.existeConflitoHorario(eq(10L), any(), any(), any())).thenReturn(false);

        BloqueioHorario existente = new BloqueioHorario(201L, quadra, dataBloqueio, null, null, "Já bloqueado", null);
        when(bloqueioHorarioRepository.findByQuadraIdAndData(10L, dataBloqueio)).thenReturn(List.of(existente));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                bloqueioHorarioService.criarBloqueio(10L, dto, 1L));

        assertTrue(ex.getMessage().contains("já possui um bloqueio cadastrado para o dia todo"));
        verify(bloqueioHorarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve remover todos os bloqueios duplicados de dia inteiro na mesma data")
    void deveRemoverTodosOsBloqueiosDuplicadosDeDiaInteiro() {
        LocalDate data = LocalDate.now().plusDays(2);
        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));

        BloqueioHorario b1 = new BloqueioHorario(301L, quadra, data, null, null, "Duplicado 1", null);
        BloqueioHorario b2 = new BloqueioHorario(302L, quadra, data, null, null, "Duplicado 2", null);

        when(bloqueioHorarioRepository.findById(301L)).thenReturn(Optional.of(b1));
        when(bloqueioHorarioRepository.findByQuadraIdAndData(10L, data)).thenReturn(List.of(b1, b2));

        bloqueioHorarioService.removerBloqueio(10L, 301L, 1L);

        verify(bloqueioHorarioRepository, times(1)).deleteAll(List.of(b1, b2));
    }

    @Test
    @DisplayName("Deve desbloquear slot inicial (06:00 às 07:00) de bloqueio de dia todo mantendo o restante (07:00 às 23:00) bloqueado")
    void deveDesbloquearSlotInicialDeBloqueioDiaTodo() {
        LocalDate data = LocalDate.now().plusDays(2);
        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));

        BloqueioHorario diaTodo = new BloqueioHorario(500L, quadra, data, null, null, "Manutenção", null);
        when(bloqueioHorarioRepository.findByQuadraIdAndData(10L, data)).thenReturn(List.of(diaTodo));

        com.agendamentos.equadras.dto.request.DesbloqueioHorarioDTO dto =
                new com.agendamentos.equadras.dto.request.DesbloqueioHorarioDTO(null, data, LocalTime.of(6, 0), LocalTime.of(7, 0));

        int total = bloqueioHorarioService.desbloquearHorarios(10L, dto, 1L);

        assertEquals(1, total);
        verify(bloqueioHorarioRepository, times(1)).deleteAll(List.of(diaTodo));

        org.mockito.ArgumentCaptor<List<BloqueioHorario>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(bloqueioHorarioRepository, times(1)).saveAll(captor.capture());

        List<BloqueioHorario> novos = captor.getValue();
        assertEquals(1, novos.size());
        assertEquals(LocalTime.of(7, 0), novos.get(0).getHoraInicio());
        assertEquals(LocalTime.of(23, 59, 59), novos.get(0).getHoraFim());
    }

    @Test
    @DisplayName("Deve desbloquear slot intermediário (12:00 às 13:00) de bloqueio de dia todo gerando dois blocos residuais")
    void deveDesbloquearSlotIntermediarioDeBloqueioDiaTodo() {
        LocalDate data = LocalDate.now().plusDays(2);
        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));

        BloqueioHorario diaTodo = new BloqueioHorario(501L, quadra, data, null, null, "Torneio", null);
        when(bloqueioHorarioRepository.findByQuadraIdAndData(10L, data)).thenReturn(List.of(diaTodo));

        com.agendamentos.equadras.dto.request.DesbloqueioHorarioDTO dto =
                new com.agendamentos.equadras.dto.request.DesbloqueioHorarioDTO(null, data, LocalTime.of(12, 0), LocalTime.of(13, 0));

        int total = bloqueioHorarioService.desbloquearHorarios(10L, dto, 1L);

        assertEquals(1, total);
        verify(bloqueioHorarioRepository, times(1)).deleteAll(List.of(diaTodo));

        org.mockito.ArgumentCaptor<List<BloqueioHorario>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(bloqueioHorarioRepository, times(1)).saveAll(captor.capture());

        List<BloqueioHorario> novos = captor.getValue();
        assertEquals(2, novos.size());
        assertEquals(LocalTime.of(6, 0), novos.get(0).getHoraInicio());
        assertEquals(LocalTime.of(12, 0), novos.get(0).getHoraFim());
        assertEquals(LocalTime.of(13, 0), novos.get(1).getHoraInicio());
        assertEquals(LocalTime.of(23, 59, 59), novos.get(1).getHoraFim());
    }
}
