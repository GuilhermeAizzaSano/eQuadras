package com.agendamentos.equadras.performance;

import com.agendamentos.equadras.dto.response.GradeHorariosResponseDTO;
import com.agendamentos.equadras.model.entity.BloqueioHorario;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.BloqueioHorarioRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.service.GradeHorariosService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** M1: grade flexível sem data resolve os 14 dias com consultas por intervalo, não por dia. */
@SpringBootTest
class GradeFlexivelQueryCountIntegrationTest {

    @Autowired private GradeHorariosService gradeHorariosService;
    @Autowired private QuadraRepository quadraRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BloqueioHorarioRepository bloqueioHorarioRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private Clock clock;

    private Usuario admin;

    @AfterEach
    void limpar() {
        bloqueioHorarioRepository.deleteAll();
        quadraRepository.deleteAll();
        usuarioRepository.delete(admin);
    }

    @Test
    @DisplayName("M1: 13 dias bloqueados e o 14º livre usam no máximo 4 statements")
    void gradeFlexivelUsaConsultasPorIntervalo() {
        admin = usuarioRepository.save(Usuario.builder().nome_usuario("Admin M1").email_usuario("admin.m1@teste.com")
                .senha_usuario("x").phone_usuario("11999990004").role(Role.ADMIN).build());
        Quadra quadra = quadraRepository.save(Quadra.builder().nome("Quadra M1").tipoEsporte(TipoEsporte.FUTSAL)
                .valorHora(BigDecimal.TEN).ativa(true).admin(admin).build());

        LocalDate hoje = LocalDate.now(clock);
        List<BloqueioHorario> bloqueios = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            bloqueios.add(new BloqueioHorario(quadra, hoje.plusDays(i), null, null, "Bloqueio M1"));
        }
        bloqueioHorarioRepository.saveAll(bloqueios);

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        List<GradeHorariosResponseDTO> grade = gradeHorariosService.consultarGradeHorariosFlexivel(null, quadra.getId_quadra(), null, null, true);

        assertTrue(stats.getPrepareStatementCount() <= 4, "statements: " + stats.getPrepareStatementCount());
        assertEquals(1, grade.size());
        assertEquals(hoje.plusDays(13), grade.get(0).data());
    }
}
