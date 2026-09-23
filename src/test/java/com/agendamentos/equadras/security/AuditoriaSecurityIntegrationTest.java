package com.agendamentos.equadras.security;

import com.agendamentos.equadras.dto.response.ApiKeyCriadaDTO;
import com.agendamentos.equadras.model.entity.LogAuditoria;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.CategoriaAuditoria;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoExecutor;
import com.agendamentos.equadras.repository.LogAuditoriaRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.service.AuditoriaService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class AuditoriaSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    private Usuario usuarioMasterAdmin;
    private Usuario usuarioAdminQuadra;
    private Usuario usuarioCliente;

    private String cookieMasterAdmin;
    private String cookieAdminQuadra;
    private String cookieCliente;
    private String rawApiKeyMaster;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        usuarioMasterAdmin = usuarioRepository.findByEmail_usuario("gui@gmail.com")
                .orElseGet(() -> usuarioRepository.save(
                        Usuario.builder()
                                .nome_usuario("Guilherme Master")
                                .email_usuario("gui@gmail.com")
                                .senha_usuario("$2a$10$N.ZOmG4i2OUPOgD8xDW3qux6w.2B8.0ZkJqPuhf45sC")
                                .phone_usuario("11999990001")
                                .role(Role.ADMIN)
                                .ativo(true)
                                .build()
                ));

        usuarioAdminQuadra = usuarioRepository.findByEmail_usuario("admin_quadra_aud@equadras.com")
                .orElseGet(() -> usuarioRepository.save(
                        Usuario.builder()
                                .nome_usuario("Admin Quadra Normal")
                                .email_usuario("admin_quadra_aud@equadras.com")
                                .senha_usuario("$2a$10$N.ZOmG4i2OUPOgD8xDW3qux6w.2B8.0ZkJqPuhf45sC")
                                .phone_usuario("11999990002")
                                .role(Role.ADMIN)
                                .ativo(true)
                                .build()
                ));

        usuarioCliente = usuarioRepository.findByEmail_usuario("cliente_aud@equadras.com")
                .orElseGet(() -> usuarioRepository.save(
                        Usuario.builder()
                                .nome_usuario("Cliente Comum")
                                .email_usuario("cliente_aud@equadras.com")
                                .senha_usuario("$2a$10$N.ZOmG4i2OUPOgD8xDW3qux6w.2B8.0ZkJqPuhf45sC")
                                .phone_usuario("11999990003")
                                .role(Role.CLIENT)
                                .ativo(true)
                                .build()
                ));

        cookieMasterAdmin = jwtService.gerarToken(usuarioMasterAdmin);
        cookieAdminQuadra = jwtService.gerarToken(usuarioAdminQuadra);
        cookieCliente = jwtService.gerarToken(usuarioCliente);

        ApiKeyCriadaDTO apiKeyDto = apiKeyService.gerarOuRegenerar(usuarioMasterAdmin.getId_usuario(), "127.0.0.1", "MockMvc");
        rawApiKeyMaster = apiKeyDto.apiKey();
    }

    @Test
    @DisplayName("Acesso anônimo a /admin/auditoria deve retornar 401 Unauthorized")
    void acessoAnonimoDeveRetornar401() throws Exception {
        mockMvc.perform(get("/admin/auditoria"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cliente comum tentando acessar /admin/auditoria deve retornar 403 Forbidden")
    void clienteComumDeveRetornar403() throws Exception {
        mockMvc.perform(get("/admin/auditoria")
                        .cookie(new Cookie("equadras_session", cookieCliente)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin de quadra comum tentando acessar /admin/auditoria deve retornar 403 Forbidden")
    void adminQuadraComumDeveRetornar403() throws Exception {
        mockMvc.perform(get("/admin/auditoria")
                        .cookie(new Cookie("equadras_session", cookieAdminQuadra)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Master Admin com API-KEY válida acessando /admin/auditoria deve retornar 200 OK")
    void masterAdminComApiKeyDeveRetornar200() throws Exception {
        mockMvc.perform(get("/admin/auditoria")
                        .header("X-API-KEY", rawApiKeyMaster))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Master Admin com Sessão Web acessando /admin/auditoria deve retornar 200 OK")
    void masterAdminDeveRetornar200() throws Exception {
        mockMvc.perform(get("/admin/auditoria")
                        .cookie(new Cookie("equadras_session", cookieMasterAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Master Admin acessando estatísticas de auditoria deve retornar 200 OK")
    void masterAdminEstatisticasDeveRetornar200() throws Exception {
        mockMvc.perform(get("/admin/auditoria/estatisticas")
                        .cookie(new Cookie("equadras_session", cookieMasterAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAcoesHoje").isNumber());
    }

    @Test
    @DisplayName("Admin de quadra acessando estatísticas deve retornar 403 Forbidden")
    void adminQuadraEstatisticasDeveRetornar403() throws Exception {
        mockMvc.perform(get("/admin/auditoria/estatisticas")
                        .cookie(new Cookie("equadras_session", cookieAdminQuadra)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Registrar ação de auditoria deve persistir no banco e ser recuperada na listagem")
    void registrarAcaoAuditoriaDevePersistir() {
        auditoriaService.registrarAcao(
                usuarioMasterAdmin,
                CategoriaAuditoria.QUADRA,
                "CRIAR",
                "QUADRA",
                "999",
                "Criação de quadra de teste auditoria",
                "192.168.1.100",
                "JUnitTestAgent"
        );

        var logs = logAuditoriaRepository.findAll();
        boolean encontrou = logs.stream().anyMatch(l ->
                "CRIAR".equals(l.getAcao())
                        && "999".equals(l.getRecursoId())
                        && TipoExecutor.MASTER_ADMIN == l.getTipoExecutor()
        );

        assertTrue(encontrou, "Log de auditoria registrado deve estar presente no repositório.");
    }
}
