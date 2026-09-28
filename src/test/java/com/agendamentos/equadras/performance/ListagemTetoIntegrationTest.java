package com.agendamentos.equadras.performance;

import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.dto.response.UsuarioResponseDTO;
import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.service.AgendamentoService;
import com.agendamentos.equadras.service.QuadraBuscaService;
import com.agendamentos.equadras.service.UsuarioService;
import com.agendamentos.equadras.shared.pagination.LimitesListagem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** M11: listagens sem paginação retornam no máximo 200 itens, na ordem especificada. */
@SpringBootTest
class ListagemTetoIntegrationTest {

    private static final int ACIMA_DO_TETO = LimitesListagem.MAXIMO_SEM_PAGINACAO + 1;

    @Autowired private AgendamentoService agendamentoService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private QuadraBuscaService quadraBuscaService;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private QuadraRepository quadraRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private final List<Long> usuariosCriados = new ArrayList<>();
    private Usuario cliente;

    @BeforeEach
    void preparar() {
        cliente = salvarUsuario("cliente.teto@teste.com", Role.CLIENT);
        Usuario admin = salvarUsuario("admin.teto@teste.com", Role.ADMIN);
        for (int i = 0; i < ACIMA_DO_TETO; i++) {
            salvarUsuario("u" + i + ".teto@teste.com", Role.CLIENT);
        }

        List<Quadra> quadras = new ArrayList<>();
        for (int i = 0; i < ACIMA_DO_TETO; i++) {
            quadras.add(Quadra.builder().nome("Quadra Teto " + i).tipoEsporte(TipoEsporte.FUTSAL)
                    .valorHora(BigDecimal.TEN).ativa(true).admin(admin).build());
        }
        quadraRepository.saveAll(quadras);

        LocalDateTime base = LocalDateTime.of(2020, 1, 1, 10, 0);
        List<Agendamento> agendamentos = new ArrayList<>();
        for (int i = 0; i < ACIMA_DO_TETO; i++) {
            agendamentos.add(Agendamento.builder().usuario(cliente).quadra(quadras.get(0))
                    .dataHoraInicio(base.plusDays(i)).dataHoraFim(base.plusDays(i).plusHours(1))
                    .valorTotal(BigDecimal.TEN).status(StatusAgendamento.CONFIRMADO).build());
        }
        agendamentoRepository.saveAll(agendamentos);
    }

    private Usuario salvarUsuario(String email, Role role) {
        Usuario u = usuarioRepository.save(Usuario.builder().nome_usuario("Teto").email_usuario(email)
                .senha_usuario("x").phone_usuario("11999990000").role(role).build());
        usuariosCriados.add(u.getId_usuario());
        return u;
    }

    @AfterEach
    void limpar() {
        agendamentoRepository.deleteAll();
        quadraRepository.deleteAll(quadraRepository.findAll().stream()
                .filter(q -> q.getNome().startsWith("Quadra Teto ")).toList());
        usuarioRepository.deleteAllById(usuariosCriados);
        usuariosCriados.clear();
    }

    @Test
    @DisplayName("Histórico de agendamentos retorna as 200 mais recentes")
    void historicoAgendamentosLimitado() {
        List<AgendamentoResponseDTO> lista = agendamentoService.listarTodos(cliente.getId_usuario(), true);

        assertEquals(LimitesListagem.MAXIMO_SEM_PAGINACAO, lista.size());
        LocalDateTime maisRecente = LocalDateTime.of(2020, 1, 1, 10, 0).plusDays(ACIMA_DO_TETO - 1);
        assertEquals(maisRecente, lista.get(0).dataHoraInicio());
    }

    @Test
    @DisplayName("Listagem de usuários retorna os 200 de maior id, em ordem decrescente")
    void usuariosLimitados() {
        List<UsuarioResponseDTO> lista = usuarioService.listarTodos();

        assertEquals(LimitesListagem.MAXIMO_SEM_PAGINACAO, lista.size());
        Long maiorId = usuarioRepository.findAll().stream().map(Usuario::getId_usuario).max(Comparator.naturalOrder()).orElseThrow();
        assertEquals(maiorId, lista.get(0).id_usuario());
    }

    @Test
    @DisplayName("Listagem de quadras sem paginação retorna no máximo 200")
    void quadrasLimitadas() {
        assertEquals(LimitesListagem.MAXIMO_SEM_PAGINACAO,
                quadraBuscaService.listarResumido(null, null, null, null, null, null, null, null, null).size());
        assertEquals(LimitesListagem.MAXIMO_SEM_PAGINACAO,
                quadraBuscaService.listar(null, null, null, null, (String) null, null, null, null, null).size());
    }
}
