package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.QuadraCriacaoDTO;
import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuadraServiceTest {

    @Mock
    private QuadraRepository quadraRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private AgendamentoService agendamentoService;

    @Mock
    private QuadraFotoService quadraFotoService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private QuadraService quadraService;

    private Usuario adminComum;
    private Usuario masterAdmin;
    private Quadra quadraAdminComum;

    @BeforeEach
    void setUp() {
        adminComum = Usuario.builder()
                .id_usuario(1L)
                .nome_usuario("Admin Comum")
                .email_usuario("admin@equadras.com")
                .role(Role.ADMIN)
                .build();

        masterAdmin = Usuario.builder()
                .id_usuario(99L)
                .nome_usuario("Master Admin")
                .email_usuario("gui@gmail.com")
                .role(Role.ADMIN)
                .build();

        quadraAdminComum = Quadra.builder()
                .id_quadra(10L)
                .nome("Quadra de Futebol")
                .tipoEsporte(TipoEsporte.FUTEBOL)
                .valorHora(BigDecimal.valueOf(100.00))
                .ativa(true)
                .admin(adminComum)
                .fotos(new ArrayList<>())
                .disponibilidades(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Master Admin deve conseguir editar quadra pertencente a outro administrador")
    void devePermitirMasterAdminEditarQuadraDeOutroAdmin() {
        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadraAdminComum));
        when(usuarioService.buscarPorIdEntidade(99L)).thenReturn(Optional.of(masterAdmin));
        when(quadraRepository.save(any(Quadra.class))).thenAnswer(i -> i.getArgument(0));

        QuadraCriacaoDTO dto = new QuadraCriacaoDTO(
                "Quadra Atualizada pelo Master",
                TipoEsporte.FUTEBOL,
                BigDecimal.valueOf(150.00),
                "01001-000",
                "Rua A",
                "Centro",
                "SP",
                "SP",
                -23.55,
                -46.63,
                "Descrição",
                null,
                List.of(),
                List.of()
        );

        QuadraResponseDTO atualizada = quadraService.editar(10L, dto, 99L);

        assertNotNull(atualizada);
        assertEquals("Quadra Atualizada pelo Master", atualizada.nome());
        verify(quadraRepository, times(1)).save(quadraAdminComum);
    }

    @Test
    @DisplayName("Admin comum não deve conseguir editar quadra de outro administrador")
    void deveBarrarAdminComumEditandoQuadraAlheia() {
        Usuario outroAdmin = Usuario.builder()
                .id_usuario(2L)
                .nome_usuario("Outro Admin")
                .email_usuario("outro@equadras.com")
                .role(Role.ADMIN)
                .build();

        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadraAdminComum));
        when(usuarioService.buscarPorIdEntidade(2L)).thenReturn(Optional.of(outroAdmin));

        QuadraCriacaoDTO dto = new QuadraCriacaoDTO(
                "Tentativa Hacker",
                TipoEsporte.FUTEBOL,
                BigDecimal.valueOf(150.00),
                "01001-000",
                "Rua A",
                "Centro",
                "SP",
                "SP",
                -23.55,
                -46.63,
                "Descrição",
                null,
                List.of(),
                List.of()
        );

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> quadraService.editar(10L, dto, 2L));
        verify(quadraRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve barrar exclusão de quadra quando houver agendamentos vinculados")
    void deveBarrarExclusaoComAgendamentosVinculados() {
        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadraAdminComum));
        when(usuarioService.buscarPorIdEntidade(1L)).thenReturn(Optional.of(adminComum));
        when(agendamentoService.possuiAgendamentos(10L)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                quadraService.excluir(10L, 1L));

        assertTrue(ex.getMessage().contains("possui agendamentos vinculados"));
        verify(quadraRepository, never()).delete(any(Quadra.class));
    }

    @Test
    @DisplayName("Deve excluir quadra com sucesso quando não houver agendamentos vinculados")
    void deveExcluirQuadraSemAgendamentos() {
        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadraAdminComum));
        when(usuarioService.buscarPorIdEntidade(1L)).thenReturn(Optional.of(adminComum));
        when(agendamentoService.possuiAgendamentos(10L)).thenReturn(false);

        assertDoesNotThrow(() -> quadraService.excluir(10L, 1L));

        verify(quadraRepository, times(1)).delete(quadraAdminComum);
        verify(eventPublisher, times(1)).publishEvent(any(com.agendamentos.equadras.event.QuadraAlteradaEvent.class));
    }
}
