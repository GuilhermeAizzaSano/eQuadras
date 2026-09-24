package com.agendamentos.equadras.listener;

import com.agendamentos.equadras.event.AgendamentoCanceladoEvent;
import com.agendamentos.equadras.event.AgendamentoPagamentoConfirmadoEvent;
import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.service.NotificacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoNotificacaoListenerTest {

    @Mock
    private NotificacaoService notificacaoService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AgendamentoNotificacaoListener listener;

    private Agendamento agendamento;
    private Usuario cliente;
    private Usuario adminQuadra;
    private Usuario outroAdmin;
    private Quadra quadra;

    @BeforeEach
    void setUp() {
        cliente = new Usuario();
        cliente.setId_usuario(1L);
        cliente.setNome_usuario("Cliente 1");
        cliente.setEmail_usuario("1@gmail.com");
        cliente.setPhone_usuario("11999990001");
        cliente.setRole(Role.CLIENT);

        adminQuadra = new Usuario();
        adminQuadra.setId_usuario(2L);
        adminQuadra.setNome_usuario("Admin Quadra");
        adminQuadra.setEmail_usuario("2@gmail.com");
        adminQuadra.setRole(Role.ADMIN);
        adminQuadra.setAtivo(true);

        outroAdmin = new Usuario();
        outroAdmin.setId_usuario(3L);
        outroAdmin.setNome_usuario("Outro Admin");
        outroAdmin.setEmail_usuario("admin@equadras.com");
        outroAdmin.setRole(Role.ADMIN);
        outroAdmin.setAtivo(true);

        quadra = new Quadra();
        quadra.setId_quadra(10L);
        quadra.setNome("Quadra Central");
        quadra.setAdmin(adminQuadra);

        agendamento = new Agendamento();
        agendamento.setId_agendamento(100L);
        agendamento.setUsuario(cliente);
        agendamento.setQuadra(quadra);
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        agendamento.setDataHoraInicio(LocalDateTime.of(2026, 10, 15, 14, 0));
        agendamento.setDataHoraFim(LocalDateTime.of(2026, 10, 15, 15, 0));
    }

    @Test
    @DisplayName("Deve enviar notificação de pagamento Pix para o dono da quadra e demais administradores ativos")
    void deveEnviarNotificacaoPagamentoParaAdmins() {
        when(usuarioRepository.findByRole(Role.ADMIN)).thenReturn(List.of(adminQuadra, outroAdmin));

        var payload = com.agendamentos.equadras.event.AgendamentoNotificacaoPayload.fromEntity(agendamento);
        listener.onAgendamentoPagamentoConfirmado(new AgendamentoPagamentoConfirmadoEvent(payload));

        verify(notificacaoService, times(1)).enviarNotificacao(eq(2L), anyString());
        verify(notificacaoService, times(1)).enviarNotificacao(eq(3L), anyString());
    }

    @Test
    @DisplayName("Deve enviar notificação de cancelamento para os administradores ativos")
    void deveEnviarNotificacaoCancelamentoParaAdmins() {
        when(usuarioRepository.findByRole(Role.ADMIN)).thenReturn(List.of(adminQuadra));

        var payload = com.agendamentos.equadras.event.AgendamentoNotificacaoPayload.fromEntity(agendamento);
        listener.onAgendamentoCancelado(new AgendamentoCanceladoEvent(
                payload,
                cliente.getId_usuario(),
                cliente.getEmail_usuario(),
                cliente.getNome_usuario(),
                com.agendamentos.equadras.model.enums.TipoExecutor.CLIENTE
        ));

        verify(notificacaoService, times(1)).enviarNotificacao(eq(2L), anyString());
    }
}
