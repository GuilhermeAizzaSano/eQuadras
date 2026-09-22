package com.agendamentos.equadras.listener;

import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Notificacao;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.NotificacaoRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.service.AgendamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AgendamentoNotificacaoIntegrationTest {

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private QuadraRepository quadraRepository;

    private Usuario admin;
    private Usuario cliente;
    private Quadra quadra;
    private Agendamento agendamento;

    @BeforeEach
    void setUp() {
        notificacaoRepository.deleteAll();
        agendamentoRepository.deleteAll();
        quadraRepository.deleteAll();
        usuarioRepository.deleteAll();

        admin = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Admin Teste")
                .email_usuario("admin@teste.com")
                .senha_usuario("senha123")
                .phone_usuario("11999990001")
                .role(Role.ADMIN)
                .build());

        cliente = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Cliente Teste")
                .email_usuario("cliente@teste.com")
                .senha_usuario("senha123")
                .phone_usuario("11999990002")
                .role(Role.CLIENT)
                .build());

        quadra = quadraRepository.save(new Quadra(
                null,
                "Quadra Show",
                TipoEsporte.FUTEBOL,
                BigDecimal.valueOf(100.0),
                true,
                "01001-000",
                "Rua A",
                "Bairro B",
                "Cidade C",
                "SP",
                -23.55,
                -46.63,
                "Desc",
                null,
                new java.util.ArrayList<>(),
                new java.util.ArrayList<>(),
                admin
        ));

        agendamento = agendamentoRepository.save(Agendamento.builder()
                .usuario(cliente)
                .quadra(quadra)
                .dataHoraInicio(LocalDateTime.now().plusDays(2).withHour(10).withMinute(0))
                .dataHoraFim(LocalDateTime.now().plusDays(2).withHour(11).withMinute(0))
                .valorTotal(BigDecimal.valueOf(100.0))
                .status(StatusAgendamento.PENDENTE)
                .build());
    }

    @Test
    @DisplayName("Ao confirmar pagamento, notificação deve ser persistida para o admin")
    void devePersistirNotificacaoAoConfirmarPagamento() {
        agendamentoService.confirmarPagamento(agendamento.getId_agendamento(), cliente.getId_usuario());

        List<Notificacao> notificacoes = notificacaoRepository.findAll();
        assertFalse(notificacoes.isEmpty(), "Notificações deveriam ter sido persistidas no banco!");
        assertEquals(admin.getId_usuario(), notificacoes.get(0).getAdmin().getId_usuario());
        assertTrue(notificacoes.get(0).getMensagem().contains("Pagamento Pix confirmado!"));
    }

    @Test
    @DisplayName("Ao cancelar agendamento, notificação deve ser persistida para o admin")
    void devePersistirNotificacaoAoCancelarAgendamento() {
        agendamentoService.cancelar(agendamento.getId_agendamento(), cliente.getId_usuario());

        List<Notificacao> notificacoes = notificacaoRepository.findAll();
        assertFalse(notificacoes.isEmpty(), "Notificação de cancelamento deveria ter sido persistida!");
        assertEquals(admin.getId_usuario(), notificacoes.get(0).getAdmin().getId_usuario());
        assertTrue(notificacoes.get(0).getMensagem().contains("Agendamento Cancelado!"));
    }
}
