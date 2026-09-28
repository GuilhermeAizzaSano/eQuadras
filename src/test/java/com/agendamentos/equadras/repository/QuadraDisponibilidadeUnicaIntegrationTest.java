package com.agendamentos.equadras.repository;

import com.agendamentos.equadras.model.entity.DisponibilidadeDia;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** V13: a mesma quadra não pode ter dois registros de disponibilidade no mesmo dia. */
@SpringBootTest
class QuadraDisponibilidadeUnicaIntegrationTest {

    @Autowired private QuadraRepository quadraRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private Usuario admin;

    @AfterEach
    void limpar() {
        quadraRepository.deleteAll(quadraRepository.findByAdminId(admin.getId_usuario()));
        usuarioRepository.delete(admin);
    }

    @Test
    @DisplayName("V13: dia da semana repetido na mesma quadra viola a unicidade")
    void diaRepetidoViolaUnicidade() {
        admin = usuarioRepository.save(Usuario.builder().nome_usuario("Admin V13").email_usuario("admin.v13@teste.com")
                .senha_usuario("x").phone_usuario("11999990013").role(Role.ADMIN).build());
        Quadra quadra = Quadra.builder().nome("Quadra V13").tipoEsporte(TipoEsporte.FUTSAL)
                .valorHora(BigDecimal.TEN).ativa(true).admin(admin)
                .disponibilidades(new ArrayList<>(List.of(
                        new DisponibilidadeDia(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                        new DisponibilidadeDia(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(16, 0)))))
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> quadraRepository.saveAndFlush(quadra));
    }
}
