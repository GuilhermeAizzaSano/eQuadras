package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.model.entity.DisponibilidadeDia;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class QuadraControllerCharacterizationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private QuadraRepository quadraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private com.agendamentos.equadras.security.JwtService jwtService;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private MockMvc mockMvc;
    private Statistics statistics;
    private Usuario admin;
    private Usuario cliente;
    private String tokenClient;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        agendamentoRepository.deleteAll();
        quadraRepository.deleteAll();
        usuarioRepository.deleteAll();

        admin = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Admin Quadras")
                .email_usuario("admin.quadras@equadras.com")
                .senha_usuario("senha123")
                .phone_usuario("11999998888")
                .role(Role.ADMIN)
                .ativo(true)
                .build());

        cliente = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Cliente Geral")
                .email_usuario("cliente.geral@equadras.com")
                .senha_usuario("senha123")
                .phone_usuario("11977776666")
                .role(Role.CLIENT)
                .ativo(true)
                .build());

        tokenClient = jwtService.gerarToken(cliente);
    }

    private Quadra criarQuadra(String nome, boolean ativa) {
        Quadra q = Quadra.builder()
                .nome(nome)
                .tipoEsporte(TipoEsporte.FUTEBOL)
                .valorHora(new BigDecimal("120.00"))
                .ativa(ativa)
                .cep("15000-000")
                .logradouro("Rua das Palmeiras, 100")
                .bairro("Centro")
                .cidade("São José do Rio Preto")
                .estado("SP")
                .latitude(-20.8113)
                .longitude(-49.3758)
                .descricao("Quadra society de grama sintética padrão FIFA")
                .dataLimiteAgendamento(LocalDate.of(2026, 12, 31))
                .fotos(new ArrayList<>(List.of("https://res.cloudinary.com/foto1.jpg")))
                .disponibilidades(new ArrayList<>(List.of(
                        new DisponibilidadeDia(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(22, 0))
                )))
                .admin(admin)
                .build();
        return quadraRepository.save(q);
    }

    @Test
    @DisplayName("Caracterização: GET /quadras (rota pública não paginada) deve retornar array com estrutura exata")
    void deveRetornarEstruturaExataEmGetQuadrasPublicaNaoPaginada() throws Exception {
        Quadra q = criarQuadra("Arena Principal", true);

        mockMvc.perform(get("/quadras")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenClient))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id_quadra").value(q.getId_quadra()))
                .andExpect(jsonPath("$[0].nome").value("Arena Principal"))
                .andExpect(jsonPath("$[0].tipoEsporte").value("FUTEBOL"))
                .andExpect(jsonPath("$[0].valorHora").value(120.00))
                .andExpect(jsonPath("$[0].ativa").value(true))
                .andExpect(jsonPath("$[0].cep").value("15000-000"))
                .andExpect(jsonPath("$[0].logradouro").value("Rua das Palmeiras, 100"))
                .andExpect(jsonPath("$[0].bairro").value("Centro"))
                .andExpect(jsonPath("$[0].cidade").value("São José do Rio Preto"))
                .andExpect(jsonPath("$[0].estado").value("SP"))
                .andExpect(jsonPath("$[0].latitude").value(-20.8113))
                .andExpect(jsonPath("$[0].longitude").value(-49.3758))
                .andExpect(jsonPath("$[0].descricao").value("Quadra society de grama sintética padrão FIFA"))
                .andExpect(jsonPath("$[0].dataLimiteAgendamento").value("2026-12-31"))
                .andExpect(jsonPath("$[0].fotos[0]").value("https://res.cloudinary.com/foto1.jpg"))
                .andExpect(jsonPath("$[0].disponibilidades[0].diaSemana").value("MONDAY"))
                .andExpect(jsonPath("$[0].disponibilidades[0].horaInicio").value("08:00:00"))
                .andExpect(jsonPath("$[0].disponibilidades[0].horaFim").value("22:00:00"));
    }

    @Test
    @DisplayName("Caracterização: GET /quadras?page=0&size=6 (rota pública paginada) deve retornar Page com estrutura exata")
    void deveRetornarEstruturaExataEmGetQuadrasPublicaPaginada() throws Exception {
        Quadra q = criarQuadra("Arena Paginada", true);

        mockMvc.perform(get("/quadras")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenClient))
                        .param("page", "0")
                        .param("size", "6")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id_quadra").value(q.getId_quadra()))
                .andExpect(jsonPath("$.content[0].nome").value("Arena Paginada"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(6));
    }

    @Test
    @DisplayName("N+1 Safety: contagem de queries para listagem de quadras paginadas não pode variar entre 2 vs 10 quadras")
    void testeN1AusenteEmListarQuadrasComparando2Vs10Quadras() throws Exception {
        // 1. Cenário com 2 quadras
        criarQuadra("Quadra Alpha", true);
        criarQuadra("Quadra Beta", true);

        statistics.clear();
        mockMvc.perform(get("/quadras")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenClient))
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        long queriesPara2Quadras = statistics.getPrepareStatementCount();

        // 2. Adiciona mais 8 quadras (total 10)
        for (int i = 3; i <= 10; i++) {
            criarQuadra("Quadra " + i, true);
        }

        statistics.clear();
        mockMvc.perform(get("/quadras")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenClient))
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10));

        long queriesPara10Quadras = statistics.getPrepareStatementCount();

        assertEquals(queriesPara2Quadras, queriesPara10Quadras,
                "Número de queries em listarQuadras não pode variar com a quantidade de quadras retornadas");
    }

    private org.springframework.test.web.servlet.ResultActions buscarPaginado(String parametro, String valor) throws Exception {
        return mockMvc.perform(get("/quadras")
                .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenClient))
                .param("page", "0")
                .param("size", "6")
                .param(parametro, valor)
                .contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Busca paginada: CEP só com dígitos encontra quadra gravada com hífen")
    void buscaPaginadaPorCepSemHifen() throws Exception {
        criarQuadra("Arena CEP", true);
        buscarPaginado("cep", "15000000").andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        buscarPaginado("cep", "15000").andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Busca paginada: cidade sem acento encontra cidade acentuada")
    void buscaPaginadaIgnoraAcentos() throws Exception {
        criarQuadra("Arena Acento", true);
        buscarPaginado("cidade", "sao jose").andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Busca paginada: '%' e '_' são literais, não curingas")
    void buscaPaginadaTrataCuringasComoLiterais() throws Exception {
        criarQuadra("Arena Principal", true);
        buscarPaginado("nome", "%").andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        buscarPaginado("nome", "_").andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Busca paginada: ordenação determinística por nome")
    void buscaPaginadaOrdenaPorNome() throws Exception {
        criarQuadra("Zeta", true);
        criarQuadra("Alfa", true);
        criarQuadra("Beta", true);
        mockMvc.perform(get("/quadras")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenClient))
                        .param("page", "0").param("size", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Alfa"))
                .andExpect(jsonPath("$.content[1].nome").value("Beta"))
                .andExpect(jsonPath("$.content[2].nome").value("Zeta"));
    }
}
