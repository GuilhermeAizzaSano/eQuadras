package com.agendamentos.equadras.repository;

import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.AbaAgendamento;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.specification.AgendamentoSpecifications;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class AgendamentoContagemRepositoryTest {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private QuadraRepository quadraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private final LocalDateTime agora = LocalDateTime.of(2026, 9, 25, 12, 0);
    private Quadra quadra;
    private Usuario cliente;

    @BeforeEach
    void setUp() {
        Usuario admin = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Admin Contagem")
                .email_usuario("admin.contagem@teste.com")
                .phone_usuario("11900000001")
                .senha_usuario("hash")
                .role(Role.ADMIN)
                .build());
        cliente = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Cliente Contagem")
                .email_usuario("cliente.contagem@teste.com")
                .phone_usuario("11900000002")
                .senha_usuario("hash")
                .role(Role.CLIENT)
                .build());
        quadra = quadraRepository.save(Quadra.builder()
                .nome("Quadra Contagem")
                .tipoEsporte(TipoEsporte.FUTEBOL)
                .valorHora(BigDecimal.valueOf(100))
                .ativa(true)
                .admin(admin)
                .build());
    }

    private void criar(StatusAgendamento status, LocalDateTime inicio) {
        agendamentoRepository.save(Agendamento.builder()
                .usuario(cliente)
                .quadra(quadra)
                .dataHoraInicio(inicio)
                .dataHoraFim(inicio.plusHours(1))
                .valorTotal(BigDecimal.valueOf(100))
                .status(status)
                .criadoEm(agora)
                .build());
    }

    @Test
    @DisplayName("Deve contar total e abas em uma única consulta SQL")
    void deveContarPorAbaEmUmaUnicaConsulta() {
        criar(StatusAgendamento.CONFIRMADO, agora.plusDays(1));
        criar(StatusAgendamento.CONFIRMADO, agora.minusDays(2));
        criar(StatusAgendamento.CANCELADO, agora.plusDays(2));
        agendamentoRepository.flush();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        ContagemPorAba contagem = agendamentoRepository.contarPorAba(
                AgendamentoSpecifications.daQuadra(quadra.getId_quadra()), agora);

        assertEquals(3L, contagem.total());
        assertEquals(1L, contagem.porAba().get(AbaAgendamento.ATIVOS));
        assertEquals(1L, contagem.porAba().get(AbaAgendamento.REALIZADOS));
        assertEquals(1L, contagem.porAba().get(AbaAgendamento.CANCELADOS));
        assertEquals(1L, statistics.getPrepareStatementCount());
    }

    @Test
    @DisplayName("Deve retornar zeros quando não houver agendamentos no escopo")
    void deveRetornarZerosSemAgendamentos() {
        ContagemPorAba contagem = agendamentoRepository.contarPorAba(
                AgendamentoSpecifications.daQuadra(quadra.getId_quadra()), agora);

        assertEquals(0L, contagem.total());
        assertEquals(0L, contagem.porAba().get(AbaAgendamento.ATIVOS));
        assertEquals(0L, contagem.porAba().get(AbaAgendamento.REALIZADOS));
        assertEquals(0L, contagem.porAba().get(AbaAgendamento.CANCELADOS));
    }
}
