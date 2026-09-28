package com.agendamentos.equadras.concorrencia;

import com.agendamentos.equadras.dto.request.QuadraCriacaoDTO;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.CategoriaAuditoria;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.LogAuditoriaRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.service.QuadraService;
import org.junit.jupiter.api.AfterEach;
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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reproduz o achado C1 do DB_AUDIT: com pool menor que o número de escritas simultâneas,
 * a auditoria em REQUIRES_NEW após o commit exigia uma segunda conexão e esgotava o pool.
 */
@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.datasource.hikari.connection-timeout=3000"
})
class AuditoriaPoolConcorrenciaIntegrationTest {

    private static final int ESCRITAS = 6;

    @Autowired private QuadraService quadraService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private QuadraRepository quadraRepository;
    @Autowired private LogAuditoriaRepository logAuditoriaRepository;

    @AfterEach
    void limpar() {
        logAuditoriaRepository.deleteAll();
        quadraRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("Escritas simultâneas acima do tamanho do pool não esgotam conexões e geram 1 log cada")
    void escritasSimultaneasNaoEsgotamPoolEAuditamTodas() throws Exception {
        Usuario admin = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Admin Pool")
                .email_usuario("admin.pool@teste.com")
                .senha_usuario("x")
                .phone_usuario("11999990099")
                .role(Role.ADMIN)
                .build());

        ExecutorService executor = Executors.newFixedThreadPool(ESCRITAS);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<?>> tarefas = new ArrayList<>();
        for (int i = 0; i < ESCRITAS; i++) {
            int n = i;
            tarefas.add(executor.submit(() -> {
                largada.await();
                quadraService.cadastrar(new QuadraCriacaoDTO(
                        "Quadra Pool " + n, TipoEsporte.FUTSAL, BigDecimal.TEN,
                        null, null, null, null, null, null, null, null, null, null, null), admin.getId_usuario());
                return null;
            }));
        }
        largada.countDown();
        for (Future<?> tarefa : tarefas) {
            tarefa.get(20, TimeUnit.SECONDS); // propaga qualquer falha de conexão
        }
        executor.shutdown();

        long logs = logAuditoriaRepository.findAll().stream()
                .filter(l -> l.getCategoria() == CategoriaAuditoria.QUADRA && "CRIAR".equals(l.getAcao()))
                .count();
        assertEquals(ESCRITAS, logs);
    }
}
