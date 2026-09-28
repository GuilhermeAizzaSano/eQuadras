package com.agendamentos.equadras.performance;

import com.agendamentos.equadras.dto.request.DisponibilidadeDiaDTO;
import com.agendamentos.equadras.dto.request.QuadraCriacaoDTO;
import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.LogAuditoriaRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.service.QuadraService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** M2: PUT /quadras só regrava disponibilidades e fotos quando elas mudam. */
@SpringBootTest
class QuadraEdicaoColecoesIntegrationTest {

    @Autowired private QuadraService quadraService;
    @Autowired private QuadraRepository quadraRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private LogAuditoriaRepository logAuditoriaRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private Usuario admin;
    private Long quadraId;

    @BeforeEach
    void preparar() {
        admin = usuarioRepository.save(Usuario.builder().nome_usuario("Admin M2").email_usuario("admin.m2@teste.com")
                .senha_usuario("x").phone_usuario("11999990003").role(Role.ADMIN).build());
        Quadra quadra = quadraRepository.save(Quadra.builder().nome("Quadra M2").tipoEsporte(TipoEsporte.FUTSAL)
                .valorHora(BigDecimal.TEN).ativa(true).admin(admin)
                .fotos(new ArrayList<>(List.of("/uploads/a.jpg", "/uploads/b.jpg"))).build());
        quadraId = quadra.getId_quadra();
    }

    @AfterEach
    void limpar() {
        logAuditoriaRepository.deleteAll();
        quadraRepository.deleteById(quadraId);
        usuarioRepository.delete(admin);
    }

    private QuadraCriacaoDTO dtoAtual(List<DisponibilidadeDiaDTO> disponibilidades) {
        QuadraResponseDTO atual = quadraService.buscarPorId(quadraId);
        return new QuadraCriacaoDTO(atual.nome(), atual.tipoEsporte(), atual.valorHora(), null, null, null, null,
                null, null, null, null, null, atual.fotos(), disponibilidades);
    }

    @Test
    @DisplayName("M2: PUT com coleções idênticas não recria nem altera coleções")
    void putSemMudancaNaoRegravaColecoes() {
        List<DisponibilidadeDiaDTO> mesmas = quadraService.buscarPorId(quadraId).disponibilidades();
        QuadraCriacaoDTO dto = dtoAtual(mesmas);

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        quadraService.editar(quadraId, dto, admin.getId_usuario());

        assertEquals(0, stats.getCollectionRecreateCount(), "recreate");
        assertEquals(0, stats.getCollectionUpdateCount(), "update");
    }

    @Test
    @DisplayName("M2: PUT que altera uma disponibilidade persiste a mudança")
    void putComMudancaPersiste() {
        List<DisponibilidadeDiaDTO> novas = new ArrayList<>(quadraService.buscarPorId(quadraId).disponibilidades());
        novas.removeIf(d -> d.diaSemana() == DayOfWeek.MONDAY);
        novas.add(new DisponibilidadeDiaDTO(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));

        quadraService.editar(quadraId, dtoAtual(novas), admin.getId_usuario());

        assertTrue(quadraService.buscarPorId(quadraId).disponibilidades()
                .contains(new DisponibilidadeDiaDTO(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0))));
    }
}
