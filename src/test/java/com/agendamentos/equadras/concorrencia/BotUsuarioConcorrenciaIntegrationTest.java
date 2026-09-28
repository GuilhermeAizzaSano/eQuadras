package com.agendamentos.equadras.concorrencia;

import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class BotUsuarioConcorrenciaIntegrationTest {

    private static final String TELEFONE = "11977776666";

    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarioRepository;

    @AfterEach
    void limpar() {
        usuarioRepository.findAll().stream()
                .filter(u -> TELEFONE.equals(u.getPhone_usuario()))
                .forEach(usuarioRepository::delete);
    }

    @Test
    @DisplayName("M8: 5 criações simultâneas do mesmo telefone retornam o mesmo usuário, sem exceção")
    void criacaoConcorrenteRetornaMesmoUsuario() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Usuario>> tarefas = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tarefas.add(executor.submit(() -> {
                largada.await();
                return usuarioService.obterOuCriarUsuarioBot("Cliente Bot", TELEFONE);
            }));
        }
        largada.countDown();
        List<Long> ids = new ArrayList<>();
        for (Future<Usuario> f : tarefas) {
            ids.add(f.get(20, TimeUnit.SECONDS).getId_usuario());
        }
        executor.shutdown();

        assertEquals(1, new HashSet<>(ids).size());
        assertEquals(1, usuarioRepository.findAll().stream().filter(u -> TELEFONE.equals(u.getPhone_usuario())).count());
    }
}
