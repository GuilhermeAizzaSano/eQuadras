package com.agendamentos.equadras.specification;

import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.AbaAgendamento;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.NotificacaoRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AgendamentoSpecificationsTest {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private QuadraRepository quadraRepository;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    private Usuario usuario1;
    private Usuario usuario2;
    private Quadra quadra;
    private static final java.time.ZoneId ZONE_BRASIL = java.time.ZoneId.of("America/Sao_Paulo");
    private java.time.Clock fixedClock;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        notificacaoRepository.deleteAll();
        agendamentoRepository.deleteAll();
        quadraRepository.deleteAll();
        usuarioRepository.deleteAll();

        java.time.Instant fixedInstant = java.time.Instant.parse("2026-10-01T12:00:00Z");
        fixedClock = java.time.Clock.fixed(fixedInstant, ZONE_BRASIL);
        baseTime = LocalDateTime.now(fixedClock);

        usuario1 = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Usuario Teste 1")
                .email_usuario("user1@especificacao.com")
                .senha_usuario("senha123")
                .phone_usuario("11999990001")
                .role(Role.CLIENT)
                .build());

        usuario2 = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Usuario Teste 2")
                .email_usuario("user2@especificacao.com")
                .senha_usuario("senha123")
                .phone_usuario("11999990002")
                .role(Role.CLIENT)
                .build());

        quadra = quadraRepository.save(Quadra.builder()
                .nome("Quadra Spec")
                .tipoEsporte(TipoEsporte.FUTEBOL)
                .valorHora(BigDecimal.valueOf(100.0))
                .ativa(true)
                .admin(usuario1)
                .build());
    }

    private Agendamento criarAgendamento(Usuario user, StatusAgendamento status, LocalDateTime inicio, LocalDateTime fim) {
        return agendamentoRepository.save(Agendamento.builder()
                .usuario(user)
                .quadra(quadra)
                .status(status)
                .dataHoraInicio(inicio)
                .dataHoraFim(fim)
                .valorTotal(BigDecimal.valueOf(100.0))
                .build());
    }

    @Test
    @DisplayName("Agendamento futuro confirmado entra em ATIVOS")
    void deveRetornarAgendamentoFuturoConfirmadoEmAtivos() {
        Agendamento agendamento = criarAgendamento(
                usuario1,
                StatusAgendamento.CONFIRMADO,
                baseTime.plusHours(1),
                baseTime.plusHours(2)
        );

        Specification<Agendamento> spec = Specification.where(AgendamentoSpecifications.doUsuario(usuario1.getId_usuario()))
                .and(AgendamentoSpecifications.daAba(AbaAgendamento.ATIVOS, baseTime));

        List<Agendamento> resultados = agendamentoRepository.findAll(spec);

        assertEquals(1, resultados.size());
        assertEquals(agendamento.getId_agendamento(), resultados.getFirst().getId_agendamento());
    }

    @Test
    @DisplayName("Agendamento futuro cancelado entra em CANCELADOS e nao em ATIVOS")
    void deveRetornarAgendamentoFuturoCanceladoEmCanceladosENaoEmAtivos() {
        Agendamento cancelado = criarAgendamento(
                usuario1,
                StatusAgendamento.CANCELADO,
                baseTime.plusHours(1),
                baseTime.plusHours(2)
        );

        Specification<Agendamento> specAtivos = Specification.where(AgendamentoSpecifications.doUsuario(usuario1.getId_usuario()))
                .and(AgendamentoSpecifications.daAba(AbaAgendamento.ATIVOS, baseTime));
        Specification<Agendamento> specCancelados = Specification.where(AgendamentoSpecifications.doUsuario(usuario1.getId_usuario()))
                .and(AgendamentoSpecifications.daAba(AbaAgendamento.CANCELADOS, baseTime));

        List<Agendamento> ativos = agendamentoRepository.findAll(specAtivos);
        List<Agendamento> cancelados = agendamentoRepository.findAll(specCancelados);

        assertTrue(ativos.isEmpty(), "Agendamento cancelado nao deve constar em ATIVOS");
        assertEquals(1, cancelados.size());
        assertEquals(cancelado.getId_agendamento(), cancelados.getFirst().getId_agendamento());
    }

    @Test
    @DisplayName("Agendamento passado confirmado entra em REALIZADOS")
    void deveRetornarAgendamentoPassadoConfirmadoEmRealizados() {
        Agendamento realizado = criarAgendamento(
                usuario1,
                StatusAgendamento.CONFIRMADO,
                baseTime.minusHours(2),
                baseTime.minusHours(1)
        );

        Specification<Agendamento> specRealizados = Specification.where(AgendamentoSpecifications.doUsuario(usuario1.getId_usuario()))
                .and(AgendamentoSpecifications.daAba(AbaAgendamento.REALIZADOS, baseTime));

        List<Agendamento> resultados = agendamentoRepository.findAll(specRealizados);

        assertEquals(1, resultados.size());
        assertEquals(realizado.getId_agendamento(), resultados.getFirst().getId_agendamento());
    }

    @Test
    @DisplayName("Agendamento passado cancelado entra em CANCELADOS e nao em REALIZADOS")
    void deveRetornarAgendamentoPassadoCanceladoEmCanceladosENaoEmRealizados() {
        Agendamento canceladoPassado = criarAgendamento(
                usuario1,
                StatusAgendamento.CANCELADO,
                baseTime.minusHours(3),
                baseTime.minusHours(2)
        );

        Specification<Agendamento> specRealizados = Specification.where(AgendamentoSpecifications.doUsuario(usuario1.getId_usuario()))
                .and(AgendamentoSpecifications.daAba(AbaAgendamento.REALIZADOS, baseTime));
        Specification<Agendamento> specCancelados = Specification.where(AgendamentoSpecifications.doUsuario(usuario1.getId_usuario()))
                .and(AgendamentoSpecifications.daAba(AbaAgendamento.CANCELADOS, baseTime));

        List<Agendamento> realizados = agendamentoRepository.findAll(specRealizados);
        List<Agendamento> cancelados = agendamentoRepository.findAll(specCancelados);

        assertTrue(realizados.isEmpty(), "Agendamento passado cancelado nao deve constar em REALIZADOS");
        assertEquals(1, cancelados.size());
        assertEquals(canceladoPassado.getId_agendamento(), cancelados.getFirst().getId_agendamento());
    }

    @Test
    @DisplayName("Agendamentos de outro usuario NUNCA aparecem em nenhuma aba")
    void naoDeveRetornarAgendamentosDeOutroUsuarioEmNenhumaAba() {
        // Criar agendamentos para o usuario2
        criarAgendamento(usuario2, StatusAgendamento.CONFIRMADO, baseTime.plusHours(1), baseTime.plusHours(2));
        criarAgendamento(usuario2, StatusAgendamento.CONFIRMADO, baseTime.minusHours(2), baseTime.minusHours(1));
        criarAgendamento(usuario2, StatusAgendamento.CANCELADO, baseTime.plusHours(3), baseTime.plusHours(4));

        for (AbaAgendamento aba : AbaAgendamento.values()) {
            Specification<Agendamento> spec = Specification.where(AgendamentoSpecifications.doUsuario(usuario1.getId_usuario()))
                    .and(AgendamentoSpecifications.daAba(aba, baseTime));

            List<Agendamento> resultados = agendamentoRepository.findAll(spec);
            assertTrue(resultados.isEmpty(), "Usuario 1 nao deve ver agendamentos de Usuario 2 na aba " + aba);
        }
    }

    @Test
    @DisplayName("Limite exato: Agendamento com dataHoraFim == agora entra em ATIVOS (dataHoraFim >= agora)")
    void deveRetornarEmAtivosQuandoDataHoraFimIgualAgora() {
        Agendamento noLimite = criarAgendamento(
                usuario1,
                StatusAgendamento.CONFIRMADO,
                baseTime.minusHours(1),
                baseTime // exatamente agora
        );

        Specification<Agendamento> specAtivos = Specification.where(AgendamentoSpecifications.doUsuario(usuario1.getId_usuario()))
                .and(AgendamentoSpecifications.daAba(AbaAgendamento.ATIVOS, baseTime));
        Specification<Agendamento> specRealizados = Specification.where(AgendamentoSpecifications.doUsuario(usuario1.getId_usuario()))
                .and(AgendamentoSpecifications.daAba(AbaAgendamento.REALIZADOS, baseTime));

        List<Agendamento> ativos = agendamentoRepository.findAll(specAtivos);
        List<Agendamento> realizados = agendamentoRepository.findAll(specRealizados);

        assertEquals(1, ativos.size(), "dataHoraFim == agora deve constar em ATIVOS");
        assertEquals(noLimite.getId_agendamento(), ativos.getFirst().getId_agendamento());
        assertTrue(realizados.isEmpty(), "dataHoraFim == agora nao deve constar em REALIZADOS");
    }

    @Test
    @DisplayName("Limite exato cancelado: cancelado no instante agora entra em CANCELADOS")
    void deveRetornarEmCanceladosQuandoCanceladoNoInstanteAgora() {
        Agendamento canceladoNoLimite = criarAgendamento(
                usuario1,
                StatusAgendamento.CANCELADO,
                baseTime.minusHours(1),
                baseTime // exatamente agora
        );

        Specification<Agendamento> specCancelados = Specification.where(AgendamentoSpecifications.doUsuario(usuario1.getId_usuario()))
                .and(AgendamentoSpecifications.daAba(AbaAgendamento.CANCELADOS, baseTime));
        Specification<Agendamento> specAtivos = Specification.where(AgendamentoSpecifications.doUsuario(usuario1.getId_usuario()))
                .and(AgendamentoSpecifications.daAba(AbaAgendamento.ATIVOS, baseTime));

        List<Agendamento> cancelados = agendamentoRepository.findAll(specCancelados);
        List<Agendamento> ativos = agendamentoRepository.findAll(specAtivos);

        assertEquals(1, cancelados.size(), "Cancelado no instante agora deve constar em CANCELADOS");
        assertEquals(canceladoNoLimite.getId_agendamento(), cancelados.getFirst().getId_agendamento());
        assertTrue(ativos.isEmpty(), "Cancelado no instante agora nao deve constar em ATIVOS");
    }
}
