package com.agendamentos.equadras.service;

import com.agendamentos.equadras.model.entity.Notificacao;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.NotificacaoRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class NotificacaoLeituraIntegrationTest {

    @Autowired private NotificacaoService notificacaoService;
    @Autowired private NotificacaoRepository notificacaoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @AfterEach
    void limpar() {
        notificacaoRepository.deleteAll();
        usuarioRepository.findByEmail_usuario("dono.b4@teste.com").ifPresent(usuarioRepository::delete);
        usuarioRepository.findByEmail_usuario("outro.b4@teste.com").ifPresent(usuarioRepository::delete);
    }

    private Usuario admin(String email) {
        return usuarioRepository.save(Usuario.builder().nome_usuario("Adm").email_usuario(email)
                .senha_usuario("x").phone_usuario("11999990000").role(Role.ADMIN).build());
    }

    @Test
    @DisplayName("B4: marca a própria notificação com 1 statement e bloqueia a de outro admin")
    void marcaComUmStatementEBloqueiaIdor() {
        Usuario dono = admin("dono.b4@teste.com");
        Usuario outro = admin("outro.b4@teste.com");
        Notificacao n = notificacaoRepository.save(new Notificacao(dono, "m"));

        assertThrows(IllegalArgumentException.class, () -> notificacaoService.marcarComoLidaSeDoUsuario(n.getId(), outro.getId_usuario()));
        assertFalse(notificacaoRepository.findById(n.getId()).orElseThrow().isLida());

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        notificacaoService.marcarComoLidaSeDoUsuario(n.getId(), dono.getId_usuario());
        assertEquals(1, stats.getPrepareStatementCount());

        assertTrue(notificacaoRepository.findById(n.getId()).orElseThrow().isLida());
    }
}
