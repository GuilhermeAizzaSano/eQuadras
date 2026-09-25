package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.dto.response.QuadraResumoResponseDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class QuadraBuscaColecoesIntegrationTest {

    private static final String DISPONIBILIDADES = Quadra.class.getName() + ".disponibilidades";

    @Autowired
    private QuadraBuscaService quadraBuscaService;

    @Autowired
    private QuadraFotoService quadraFotoService;

    @Autowired
    private QuadraRepository quadraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        Usuario admin = usuarioRepository.save(Usuario.builder()
                .nome_usuario("colecoes.admin").email_usuario("colecoes.admin@teste.com")
                .phone_usuario("11955550001").senha_usuario("hash").role(Role.ADMIN).build());
        for (String nome : List.of("Colecao Alfa", "Colecao Beta")) {
            quadraRepository.save(Quadra.builder().nome(nome).tipoEsporte(TipoEsporte.FUTEBOL)
                    .valorHora(BigDecimal.TEN).ativa(true).admin(admin)
                    .fotos(new ArrayList<>(List.of("http://foto/" + nome))).build());
        }
        entityManager.flush();
        entityManager.clear();

        statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @Test
    @DisplayName("Resumo do bot não deve carregar disponibilidades")
    void resumoNaoDeveCarregarDisponibilidades() {
        List<QuadraResumoResponseDTO> resumo = quadraBuscaService.listarResumido(null, null, null, null, null, "colecao", null, null, null);

        assertEquals(2, resumo.size());
        assertEquals(0, statistics.getCollectionStatistics(DISPONIBILIDADES).getLoadCount());
    }

    @Test
    @DisplayName("Consulta de fotos não deve carregar disponibilidades")
    void consultaDeFotosNaoDeveCarregarDisponibilidades() {
        Object resposta = quadraFotoService.consultarFotos(null, "colecao", null, null, null);

        assertEquals(2, ((List<?>) resposta).size());
        assertEquals(0, statistics.getCollectionStatistics(DISPONIBILIDADES).getLoadCount());
    }

    @Test
    @DisplayName("Listagem completa continua devolvendo disponibilidades")
    void listagemCompletaDeveManterDisponibilidades() {
        List<QuadraResponseDTO> lista = quadraBuscaService.listar(null, null, null, null, (String) null, "colecao", null, null, null);

        assertEquals(2, lista.size());
        lista.forEach(q -> assertFalse(q.disponibilidades().isEmpty(), "DTO completo precisa das disponibilidades"));
        assertTrue(statistics.getCollectionStatistics(DISPONIBILIDADES).getLoadCount() > 0);
    }
}
