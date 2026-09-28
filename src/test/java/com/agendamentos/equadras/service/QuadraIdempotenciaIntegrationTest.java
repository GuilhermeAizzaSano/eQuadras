package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.QuadraCriacaoDTO;
import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.LogAuditoriaRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** B1: POST /quadras com a mesma Idempotency-Key cria uma quadra só. */
@SpringBootTest
class QuadraIdempotenciaIntegrationTest {

    @Autowired private QuadraIdempotenciaService idempotenciaService;
    @Autowired private QuadraRepository quadraRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private LogAuditoriaRepository logAuditoriaRepository;

    private Usuario admin;
    private Usuario outroAdmin;

    @BeforeEach
    void preparar() {
        admin = usuarioRepository.save(Usuario.builder().nome_usuario("Admin B1").email_usuario("admin.b1@teste.com")
                .senha_usuario("x").phone_usuario("11999990018").role(Role.ADMIN).build());
        outroAdmin = usuarioRepository.save(Usuario.builder().nome_usuario("Admin B1 dois").email_usuario("admin.b1b@teste.com")
                .senha_usuario("x").phone_usuario("11999990019").role(Role.ADMIN).build());
    }

    @AfterEach
    void limpar() {
        logAuditoriaRepository.deleteAll();
        quadraRepository.deleteAll(quadraRepository.findByAdminId(admin.getId_usuario()));
        quadraRepository.deleteAll(quadraRepository.findByAdminId(outroAdmin.getId_usuario()));
        usuarioRepository.delete(admin);
        usuarioRepository.delete(outroAdmin);
    }

    private QuadraCriacaoDTO dto() {
        return new QuadraCriacaoDTO("Quadra B1", TipoEsporte.FUTSAL, BigDecimal.TEN, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("B1: 5 envios simultâneos com a mesma chave criam 1 quadra")
    void mesmaChaveConcorrenteCriaUmaQuadra() throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(5);
        List<Future<QuadraResponseDTO>> tarefas = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tarefas.add(pool.submit(() -> {
                largada.await();
                return idempotenciaService.cadastrar(dto(), admin.getId_usuario(), "chave-b1");
            }));
        }
        largada.countDown();
        List<Long> ids = new ArrayList<>();
        for (Future<QuadraResponseDTO> tarefa : tarefas) {
            ids.add(tarefa.get().id_quadra());
        }
        pool.shutdown();

        assertEquals(1, quadraRepository.findByAdminId(admin.getId_usuario()).size());
        assertEquals(1, ids.stream().distinct().count());
    }

    @Test
    @DisplayName("B1: chaves diferentes criam quadras diferentes")
    void chavesDiferentes() {
        Long a = idempotenciaService.cadastrar(dto(), admin.getId_usuario(), "chave-a").id_quadra();
        Long b = idempotenciaService.cadastrar(dto(), admin.getId_usuario(), "chave-b").id_quadra();
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("B1: a mesma chave de admins diferentes não se mistura")
    void mesmaChaveAdminsDiferentes() {
        Long a = idempotenciaService.cadastrar(dto(), admin.getId_usuario(), "chave-x").id_quadra();
        Long b = idempotenciaService.cadastrar(dto(), outroAdmin.getId_usuario(), "chave-x").id_quadra();
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("B1: sem chave mantém o comportamento atual")
    void semChave() {
        idempotenciaService.cadastrar(dto(), admin.getId_usuario(), null);
        idempotenciaService.cadastrar(dto(), admin.getId_usuario(), " ");
        assertEquals(2, quadraRepository.findByAdminId(admin.getId_usuario()).size());
    }
}
