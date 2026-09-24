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

import static org.hamcrest.Matchers.lessThanOrEqualTo;
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

    private MockMvc mockMvc;
    private Usuario usuarioA;
    private Usuario usuarioB;
    private Quadra quadra;
    private String tokenA;
    private String tokenB;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

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

        quadra = quadraRepository.save(Quadra.builder()
                .nome("Arena Central")
                .tipoEsporte(TipoEsporte.FUTEBOL)
                .valorHora(BigDecimal.valueOf(120.0))
                .ativa(true)
                .admin(adminQuadra)
                .build());

        tokenA = jwtService.gerarToken(usuarioA);
        tokenB = jwtService.gerarToken(usuarioB);
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
}
