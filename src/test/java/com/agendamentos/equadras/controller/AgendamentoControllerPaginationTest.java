package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.NotificacaoRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.security.JwtService;
import jakarta.servlet.http.Cookie;
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
import java.time.LocalDateTime;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AgendamentoControllerPaginationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private QuadraRepository quadraRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private jakarta.persistence.EntityManagerFactory entityManagerFactory;

    private MockMvc mockMvc;
    private Statistics statistics;
    private Usuario usuarioA;
    private Usuario usuarioB;
    private Quadra quadra;
    private String tokenA;
    private String tokenB;
    private String tokenAdmin;
    private String tokenOutroAdmin;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);

        notificacaoRepository.deleteAll();
        agendamentoRepository.deleteAll();
        quadraRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuarioA = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Atleta A")
                .email_usuario("atleta_a_" + System.currentTimeMillis() + "@teste.com")
                .senha_usuario("senha123")
                .phone_usuario("11999990001")
                .role(Role.CLIENT)
                .ativo(true)
                .build());

        usuarioB = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Atleta B")
                .email_usuario("atleta_b_" + System.currentTimeMillis() + "@teste.com")
                .senha_usuario("senha123")
                .phone_usuario("11999990002")
                .role(Role.CLIENT)
                .ativo(true)
                .build());

        Usuario adminQuadra = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Admin Quadra")
                .email_usuario("admin_q_" + System.currentTimeMillis() + "@teste.com")
                .senha_usuario("senha123")
                .phone_usuario("11999990003")
                .role(Role.ADMIN)
                .ativo(true)
                .build());

        Usuario outroAdmin = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Outro Admin")
                .email_usuario("outro_admin_" + System.currentTimeMillis() + "@teste.com")
                .senha_usuario("senha123")
                .phone_usuario("11999990004")
                .role(Role.ADMIN)
                .ativo(true)
                .build());

        quadra = quadraRepository.save(Quadra.builder()
                .nome("Arena Central")
                .tipoEsporte(TipoEsporte.FUTEBOL)
                .valorHora(BigDecimal.valueOf(120.0))
                .ativa(true)
                .admin(adminQuadra)
                .build());

        tokenA = jwtService.gerarToken(usuarioA);
        tokenB = jwtService.gerarToken(usuarioB);
        tokenAdmin = jwtService.gerarToken(adminQuadra);
        tokenOutroAdmin = jwtService.gerarToken(outroAdmin);
        baseTime = LocalDateTime.now();
    }

    private Agendamento criarAgendamento(Usuario user, StatusAgendamento status, LocalDateTime inicio, LocalDateTime fim) {
        return agendamentoRepository.save(Agendamento.builder()
                .usuario(user)
                .quadra(quadra)
                .status(status)
                .dataHoraInicio(inicio)
                .dataHoraFim(fim)
                .valorTotal(BigDecimal.valueOf(120.0))
                .build());
    }

    @Test
    @DisplayName("1. GET /agendamentos?page=0&size=5&aba=ATIVOS deve retornar 200 com estrutura paginada")
    void deveRetornarRespostaPaginadaParaAbaAtivos() throws Exception {
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, baseTime.plusDays(1), baseTime.plusDays(1).plusHours(1));

        var result = mockMvc.perform(get("/agendamentos")
                        .cookie(new Cookie("equadras_session", tokenA))
                        .param("page", "0")
                        .param("size", "5")
                        .param("aba", "ATIVOS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        if (result.getResolvedException() != null) {
            System.err.println("RESOLVED EXCEPTION: " + result.getResolvedException().getMessage());
            result.getResolvedException().printStackTrace();
        }
        org.junit.jupiter.api.Assertions.assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    @DisplayName("2. IDOR: Usuário B não deve visualizar reservas do Usuário A")
    void usuarioBNaoDeveVerReservasDoUsuarioA() throws Exception {
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, baseTime.plusDays(1), baseTime.plusDays(1).plusHours(1));

        mockMvc.perform(get("/agendamentos")
                        .cookie(new Cookie("equadras_session", tokenB))
                        .param("page", "0")
                        .param("size", "5")
                        .param("aba", "ATIVOS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("3. size=10000 deve ser limitado ao máximo de 50 no servidor")
    void sizeGiganteDeveSerLimitadoAoMaximoConfigurado() throws Exception {
        mockMvc.perform(get("/agendamentos")
                        .cookie(new Cookie("equadras_session", tokenA))
                        .param("page", "0")
                        .param("size", "10000")
                        .param("aba", "ATIVOS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(lessThanOrEqualTo(50)));
    }

    @Test
    @DisplayName("4. sort=usuario.senha não permitido na whitelist deve retornar 400 Bad Request com PARAMETRO_INVALIDO")
    void sortNaoPermitidoDeveRetornar400() throws Exception {
        mockMvc.perform(get("/agendamentos")
                        .cookie(new Cookie("equadras_session", tokenA))
                        .param("page", "0")
                        .param("size", "5")
                        .param("aba", "ATIVOS")
                        .param("sort", "usuario.senha")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAMETRO_INVALIDO"));
    }

    @Test
    @DisplayName("5. aba inválida (ex: aba=INVALIDA) deve retornar 400 Bad Request com PARAMETRO_INVALIDO")
    void abaInvalidaDeveRetornar400() throws Exception {
        mockMvc.perform(get("/agendamentos")
                        .cookie(new Cookie("equadras_session", tokenA))
                        .param("page", "0")
                        .param("size", "5")
                        .param("aba", "INVALIDA")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAMETRO_INVALIDO"));
    }

    @Test
    @DisplayName("6. Requisição sem autenticação deve retornar 401 Unauthorized")
    void requisicaoSemAutenticacaoDeveRetornar401() throws Exception {
        mockMvc.perform(get("/agendamentos")
                        .param("page", "0")
                        .param("size", "5")
                        .param("aba", "ATIVOS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("7. GET /agendamentos/contadores deve retornar 200 com map contendo ATIVOS, REALIZADOS e CANCELADOS")
    void deveRetornarContadoresPorAba() throws Exception {
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, baseTime.plusDays(1), baseTime.plusDays(1).plusHours(1));
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, baseTime.minusDays(2), baseTime.minusDays(2).plusHours(1));
        criarAgendamento(usuarioA, StatusAgendamento.CANCELADO, baseTime.plusDays(2), baseTime.plusDays(2).plusHours(1));

        mockMvc.perform(get("/agendamentos/contadores")
                        .cookie(new Cookie("equadras_session", tokenA))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ATIVOS").value(1))
                .andExpect(jsonPath("$.REALIZADOS").value(1))
                .andExpect(jsonPath("$.CANCELADOS").value(1));
    }

    @Test
    @DisplayName("8. Endpoint legado: GET /agendamentos?historico=true deve preservar resposta em array JSON")
    void endpointLegadoDeveRetornarArrayJson() throws Exception {
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, baseTime.plusDays(1), baseTime.plusDays(1).plusHours(1));

        mockMvc.perform(get("/agendamentos")
                        .cookie(new Cookie("equadras_session", tokenA))
                        .param("historico", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("9. GET /agendamentos?page=0&size=5&apenasPendentes=true retorna 200 e dispensa parâmetro aba")
    void deveRetornarApenasPendentesValidosSemAba() throws Exception {
        Agendamento pendenteRecente = agendamentoRepository.save(Agendamento.builder()
                .usuario(usuarioA)
                .quadra(quadra)
                .status(StatusAgendamento.PENDENTE)
                .criadoEm(LocalDateTime.now().minusMinutes(5))
                .dataHoraInicio(baseTime.plusDays(1))
                .dataHoraFim(baseTime.plusDays(1).plusHours(1))
                .valorTotal(BigDecimal.valueOf(120.0))
                .build());

        agendamentoRepository.save(Agendamento.builder()
                .usuario(usuarioA)
                .quadra(quadra)
                .status(StatusAgendamento.PENDENTE)
                .criadoEm(LocalDateTime.now().minusMinutes(25))
                .dataHoraInicio(baseTime.plusDays(2))
                .dataHoraFim(baseTime.plusDays(2).plusHours(1))
                .valorTotal(BigDecimal.valueOf(120.0))
                .build());

        mockMvc.perform(get("/agendamentos")
                        .cookie(new Cookie("equadras_session", tokenA))
                        .param("page", "0")
                        .param("size", "5")
                        .param("apenasPendentes", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id_agendamento").value(pendenteRecente.getId_agendamento()));
    }

    @Test
    @DisplayName("10. GET /agendamentos?page=0&size=5 sem aba e sem apenasPendentes deve retornar 400")
    void requisicaoSemAbaESemApenasPendentesDeveRetornar400() throws Exception {
        mockMvc.perform(get("/agendamentos")
                        .cookie(new Cookie("equadras_session", tokenA))
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("11. N+1 Safety: contagem de queries deve ser idêntica comparando 2 vs 10 registros na paginação por aba")
    void testeN1AusenteEmListarPaginadoComparando2Vs10Itens() throws Exception {
        // 1. Cenário com 2 agendamentos
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, baseTime.plusDays(1), baseTime.plusDays(1).plusHours(1));
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, baseTime.plusDays(2), baseTime.plusDays(2).plusHours(1));

        statistics.clear();
        mockMvc.perform(get("/agendamentos")
                        .cookie(new Cookie("equadras_session", tokenA))
                        .param("page", "0")
                        .param("size", "20")
                        .param("aba", "ATIVOS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        long queriesPara2Itens = statistics.getPrepareStatementCount();

        // 2. Adiciona mais 8 agendamentos (total 10)
        for (int i = 3; i <= 10; i++) {
            criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, baseTime.plusDays(i), baseTime.plusDays(i).plusHours(1));
        }

        statistics.clear();
        mockMvc.perform(get("/agendamentos")
                        .cookie(new Cookie("equadras_session", tokenA))
                        .param("page", "0")
                        .param("size", "20")
                        .param("aba", "ATIVOS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10));

        long queriesPara10Itens = statistics.getPrepareStatementCount();

        // Quantidade de queries deve permanecer constante (1 count + 1 select com join fetch/entity graph)
        assertEquals(queriesPara2Itens, queriesPara10Itens,
                "Número de queries não pode variar com a quantidade de itens retornados (N+1 detectado)");
    }

    @Test
    @DisplayName("12. N+1 Safety: contagem de queries deve ser idêntica comparando 2 vs 10 registros com apenasPendentes=true")
    void testeN1AusenteEmApenasPendentesComparando2Vs10Itens() throws Exception {
        // 1. Cenário com 2 pendentes
        for (int i = 1; i <= 2; i++) {
            agendamentoRepository.save(Agendamento.builder()
                    .usuario(usuarioA)
                    .quadra(quadra)
                    .status(StatusAgendamento.PENDENTE)
                    .criadoEm(LocalDateTime.now().minusMinutes(i))
                    .dataHoraInicio(baseTime.plusDays(i))
                    .dataHoraFim(baseTime.plusDays(i).plusHours(1))
                    .valorTotal(BigDecimal.valueOf(120.0))
                    .build());
        }

        statistics.clear();
        mockMvc.perform(get("/agendamentos")
                        .cookie(new Cookie("equadras_session", tokenA))
                        .param("page", "0")
                        .param("size", "20")
                        .param("apenasPendentes", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        long queriesPara2Itens = statistics.getPrepareStatementCount();

        // 2. Adiciona mais 8 pendentes (total 10)
        for (int i = 3; i <= 10; i++) {
            agendamentoRepository.save(Agendamento.builder()
                    .usuario(usuarioA)
                    .quadra(quadra)
                    .status(StatusAgendamento.PENDENTE)
                    .criadoEm(LocalDateTime.now().minusMinutes(i))
                    .dataHoraInicio(baseTime.plusDays(i))
                    .dataHoraFim(baseTime.plusDays(i).plusHours(1))
                    .valorTotal(BigDecimal.valueOf(120.0))
                    .build());
        }

        statistics.clear();
        mockMvc.perform(get("/agendamentos")
                        .cookie(new Cookie("equadras_session", tokenA))
                        .param("page", "0")
                        .param("size", "20")
                        .param("apenasPendentes", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10));

        long queriesPara10Itens = statistics.getPrepareStatementCount();

        assertEquals(queriesPara2Itens, queriesPara10Itens,
                "Número de queries não pode variar com a quantidade de itens retornados com apenasPendentes=true");
    }

    @Test
    @DisplayName("13. GET /agendamentos/quadra/{quadraId}?page=0&size=5 deve retornar 200 com PageResponse para admin dono")
    void deveRetornarRespostaPaginadaParaHistoricoDeQuadra() throws Exception {
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, baseTime.plusDays(1), baseTime.plusDays(1).plusHours(1));

        mockMvc.perform(get("/agendamentos/quadra/" + quadra.getId_quadra())
                        .cookie(new Cookie("equadras_session", tokenAdmin))
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("14. IDOR: GET /agendamentos/quadra/{quadraId}?page=0&size=5 por outro admin não proprietário deve retornar 403")
    void outroAdminNaoDeveAcessarHistoricoDeQuadraAlheia() throws Exception {
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, baseTime.plusDays(1), baseTime.plusDays(1).plusHours(1));

        mockMvc.perform(get("/agendamentos/quadra/" + quadra.getId_quadra())
                        .cookie(new Cookie("equadras_session", tokenOutroAdmin))
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("15. GET /agendamentos/quadra/{quadraId}/contadores deve retornar 200 com contadores corretos")
    void deveRetornarContadoresDeQuadra() throws Exception {
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, baseTime.plusDays(1), baseTime.plusDays(1).plusHours(1));
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, baseTime.minusDays(2), baseTime.minusDays(2).plusHours(1));
        criarAgendamento(usuarioA, StatusAgendamento.CANCELADO, baseTime.plusDays(2), baseTime.plusDays(2).plusHours(1));

        mockMvc.perform(get("/agendamentos/quadra/" + quadra.getId_quadra() + "/contadores")
                        .cookie(new Cookie("equadras_session", tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TODOS").value(3))
                .andExpect(jsonPath("$.ATIVOS").value(1))
                .andExpect(jsonPath("$.REALIZADOS").value(1))
                .andExpect(jsonPath("$.CANCELADOS").value(1));
    }

    @Test
    @DisplayName("16. Endpoint legado: GET /agendamentos/quadra/{quadraId} sem page deve retornar 200 em array")
    void endpointLegadoQuadraDeveRetornarArrayJson() throws Exception {
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, baseTime.plusDays(1), baseTime.plusDays(1).plusHours(1));

        mockMvc.perform(get("/agendamentos/quadra/" + quadra.getId_quadra())
                        .cookie(new Cookie("equadras_session", tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("17. N+1 Safety: contagem de queries deve ser idêntica comparando 2 vs 10 registros em GET /agendamentos/quadra/{quadraId}?page=0&size=20")
    void testeN1AusenteEmListarPorQuadraPaginadoComparando2Vs10Itens() throws Exception {
        // 1. Cenário com 2 itens
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, baseTime.plusDays(1), baseTime.plusDays(1).plusHours(1));
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, baseTime.plusDays(2), baseTime.plusDays(2).plusHours(1));

        statistics.clear();
        mockMvc.perform(get("/agendamentos/quadra/" + quadra.getId_quadra())
                        .cookie(new Cookie("equadras_session", tokenAdmin))
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        long queriesPara2Itens = statistics.getPrepareStatementCount();

        // 2. Adiciona mais 8 itens (total 10)
        for (int i = 3; i <= 10; i++) {
            criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, baseTime.plusDays(i), baseTime.plusDays(i).plusHours(1));
        }

        statistics.clear();
        mockMvc.perform(get("/agendamentos/quadra/" + quadra.getId_quadra())
                        .cookie(new Cookie("equadras_session", tokenAdmin))
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10));

        long queriesPara10Itens = statistics.getPrepareStatementCount();

        assertEquals(queriesPara2Itens, queriesPara10Itens,
                "Número de queries em listarPorQuadraPaginado não pode variar com a quantidade de itens retornados");
    }

    @Test
    @DisplayName("18. GET /agendamentos/agenda?data=...&page=0&size=5 deve retornar 200 com PageResponse para admin dono")
    void deveRetornarAgendaDoDiaPaginada() throws Exception {
        java.time.LocalDate dataAmanha = baseTime.toLocalDate().plusDays(1);
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, dataAmanha.atTime(10, 0), dataAmanha.atTime(11, 0));

        mockMvc.perform(get("/agendamentos/agenda")
                        .cookie(new Cookie("equadras_session", tokenAdmin))
                        .param("data", dataAmanha.toString())
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("19. IDOR: GET /agendamentos/agenda?data=...&quadraId={quadraId} por outro admin deve retornar 403")
    void outroAdminNaoDeveAcessarAgendaDeQuadraAlheia() throws Exception {
        java.time.LocalDate dataAmanha = baseTime.toLocalDate().plusDays(1);
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, dataAmanha.atTime(10, 0), dataAmanha.atTime(11, 0));

        mockMvc.perform(get("/agendamentos/agenda")
                        .cookie(new Cookie("equadras_session", tokenOutroAdmin))
                        .param("data", dataAmanha.toString())
                        .param("quadraId", quadra.getId_quadra().toString())
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("20. Escopo: GET /agendamentos/agenda?data=... por admin sem quadras deve retornar 0 itens")
    void adminSemQuadrasDeveReceberZeroItensNaAgenda() throws Exception {
        java.time.LocalDate dataAmanha = baseTime.toLocalDate().plusDays(1);
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, dataAmanha.atTime(10, 0), dataAmanha.atTime(11, 0));

        mockMvc.perform(get("/agendamentos/agenda")
                        .cookie(new Cookie("equadras_session", tokenOutroAdmin))
                        .param("data", dataAmanha.toString())
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("21. GET /agendamentos/agenda/contadores deve retornar 200 com contadores por aba")
    void deveRetornarContadoresAgendaDoDia() throws Exception {
        java.time.LocalDate dataAmanha = baseTime.toLocalDate().plusDays(1);
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, dataAmanha.atTime(10, 0), dataAmanha.atTime(11, 0));
        criarAgendamento(usuarioA, StatusAgendamento.CANCELADO, dataAmanha.atTime(14, 0), dataAmanha.atTime(15, 0));

        mockMvc.perform(get("/agendamentos/agenda/contadores")
                        .cookie(new Cookie("equadras_session", tokenAdmin))
                        .param("data", dataAmanha.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ATIVOS").value(1))
                .andExpect(jsonPath("$.CANCELADOS").value(1));
    }

    @Test
    @DisplayName("22. N+1 Safety: contagem de queries deve ser idêntica comparando 2 vs 10 registros em GET /agendamentos/agenda?data=...&page=0&size=20")
    void testeN1AusenteEmListarAgendaDoDiaComparando2Vs10Itens() throws Exception {
        java.time.LocalDate dataAmanha = baseTime.toLocalDate().plusDays(1);

        // 1. Cenário com 2 itens
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, dataAmanha.atTime(8, 0), dataAmanha.atTime(9, 0));
        criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, dataAmanha.atTime(9, 0), dataAmanha.atTime(10, 0));

        statistics.clear();
        mockMvc.perform(get("/agendamentos/agenda")
                        .cookie(new Cookie("equadras_session", tokenAdmin))
                        .param("data", dataAmanha.toString())
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        long queriesPara2Itens = statistics.getPrepareStatementCount();

        // 2. Adiciona mais 8 itens (total 10)
        for (int i = 10; i <= 17; i++) {
            criarAgendamento(usuarioA, StatusAgendamento.CONFIRMADO, dataAmanha.atTime(i, 0), dataAmanha.atTime(i + 1, 0));
        }

        statistics.clear();
        mockMvc.perform(get("/agendamentos/agenda")
                        .cookie(new Cookie("equadras_session", tokenAdmin))
                        .param("data", dataAmanha.toString())
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10));

        long queriesPara10Itens = statistics.getPrepareStatementCount();

        assertEquals(queriesPara2Itens, queriesPara10Itens,
                "Número de queries em listarAgendaDoDia não pode variar com a quantidade de itens retornados");
    }
}
