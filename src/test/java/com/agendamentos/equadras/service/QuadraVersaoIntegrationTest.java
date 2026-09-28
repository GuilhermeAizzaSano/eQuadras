package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.QuadraCriacaoDTO;
import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.exception.RegraNegocioException;
import com.agendamentos.equadras.model.entity.Quadra;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** B2: edição de quadra com controle de versão otimista. */
@SpringBootTest
class QuadraVersaoIntegrationTest {

    @Autowired private QuadraService quadraService;
    @Autowired private QuadraRepository quadraRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private LogAuditoriaRepository logAuditoriaRepository;

    private Usuario admin;
    private Long quadraId;

    @BeforeEach
    void preparar() {
        admin = usuarioRepository.save(Usuario.builder().nome_usuario("Admin B2").email_usuario("admin.b2@teste.com")
                .senha_usuario("x").phone_usuario("11999990017").role(Role.ADMIN).build());
        quadraId = quadraRepository.save(Quadra.builder().nome("Quadra B2").tipoEsporte(TipoEsporte.FUTSAL)
                .valorHora(BigDecimal.TEN).ativa(true).admin(admin).fotos(new ArrayList<>()).build()).getId_quadra();
    }

    @AfterEach
    void limpar() {
        logAuditoriaRepository.deleteAll();
        quadraRepository.deleteById(quadraId);
        usuarioRepository.delete(admin);
    }

    private QuadraCriacaoDTO dto(String nome, Long versao) {
        return new QuadraCriacaoDTO(nome, TipoEsporte.FUTSAL, BigDecimal.TEN, null, null, null, null,
                null, null, null, null, null, null, null, versao);
    }

    @Test
    @DisplayName("B2: PUT com a versão atual incrementa a versão")
    void versaoAtualIncrementa() {
        Long versao = quadraService.buscarPorId(quadraId).versao();

        QuadraResponseDTO resposta = quadraService.editar(quadraId, dto("Nome novo", versao), admin.getId_usuario());

        assertEquals(versao + 1, resposta.versao());
    }

    @Test
    @DisplayName("B2: PUT com versão antiga retorna CONFLITO_VERSAO")
    void versaoAntigaConflita() {
        Long versao = quadraService.buscarPorId(quadraId).versao();
        quadraService.editar(quadraId, dto("Primeira", versao), admin.getId_usuario());

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> quadraService.editar(quadraId, dto("Segunda", versao), admin.getId_usuario()));
        assertEquals("CONFLITO_VERSAO", ex.getCode());
    }

    @Test
    @DisplayName("B2: PUT sem versão continua funcionando")
    void semVersaoFunciona() {
        assertEquals("Sem versao", quadraService.editar(quadraId, dto("Sem versao", null), admin.getId_usuario()).nome());
    }

    @Test
    @DisplayName("B2: dois PUT simultâneos com a mesma versão resultam em 1 sucesso e 1 conflito")
    void putsSimultaneos() throws Exception {
        Long versao = quadraService.buscarPorId(quadraId).versao();
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<?>> tarefas = new ArrayList<>();
        for (String nome : List.of("Put A", "Put B")) {
            tarefas.add(pool.submit(() -> {
                largada.await();
                return quadraService.editar(quadraId, dto(nome, versao), admin.getId_usuario());
            }));
        }
        largada.countDown();

        int sucessos = 0;
        int conflitos = 0;
        for (Future<?> tarefa : tarefas) {
            try {
                tarefa.get();
                sucessos++;
            } catch (java.util.concurrent.ExecutionException e) {
                boolean conflito = e.getCause() instanceof ObjectOptimisticLockingFailureException
                        || (e.getCause() instanceof RegraNegocioException r && "CONFLITO_VERSAO".equals(r.getCode()));
                if (!conflito) throw e;
                conflitos++;
            }
        }
        pool.shutdown();

        assertEquals(1, sucessos);
        assertEquals(1, conflitos);
    }
}
