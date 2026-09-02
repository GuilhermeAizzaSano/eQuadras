package com.agendamentos.equadras.concorrencia;

import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.AgendamentoRepository;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class AgendamentoConcorrenciaIntegrationTest {

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private QuadraRepository quadraRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    private Usuario usuario1;
    private Usuario usuario2;
    private Quadra quadra;

    @BeforeEach
    void setUp() {
        agendamentoRepository.deleteAll();
        quadraRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuario1 = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Usuario 1")
                .email_usuario("user1@teste.com")
                .senha_usuario("senha123")
                .phone_usuario("11999990001")
                .role(Role.CLIENT)
                .build());

        usuario2 = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Usuario 2")
                .email_usuario("user2@teste.com")
                .senha_usuario("senha123")
                .phone_usuario("11999990002")
                .role(Role.CLIENT)
                .build());

        quadra = quadraRepository.save(Quadra.builder()
                .nome("Quadra Lock Concorrente")
                .tipoEsporte(TipoEsporte.FUTEBOL)
                .valorHora(BigDecimal.valueOf(150.0))
                .ativa(true)
                .admin(usuario1)
                .build());
    }

    @Test
    @DisplayName("Deve permitir apenas 1 agendamento bem-sucedido para o mesmo horário e quadra sob concorrência")
    void deveSerializarAgendamentosConcorrentes() throws InterruptedException {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        LocalDateTime inicio = LocalDateTime.now().plusDays(2).withHour(18).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fim = inicio.plusHours(1);

        AgendamentoCriacaoDTO dto = new AgendamentoCriacaoDTO(null, quadra.getId_quadra(), inicio, fim);

        AtomicInteger sucessos = new AtomicInteger(0);
        AtomicInteger falhas = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                agendamentoService.agendar(dto, usuario1.getId_usuario());
                sucessos.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
                falhas.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                agendamentoService.agendar(dto, usuario2.getId_usuario());
                sucessos.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
                falhas.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executor.shutdown();

        assertEquals(1, sucessos.get(), "Apenas 1 agendamento concorrente deve ter sucesso");
        assertEquals(1, falhas.get(), "O segundo agendamento no mesmo horário deve falhar");
        assertEquals(1, agendamentoRepository.count(), "Apenas 1 registro deve constar no banco de dados");
    }
}