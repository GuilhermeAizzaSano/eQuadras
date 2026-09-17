package com.agendamentos.equadras.security;

import com.agendamentos.equadras.dto.response.ApiKeyCriadaDTO;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class ApiKeySecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoSpyBean
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ApiKeyRateLimiter apiKeyRateLimiter;

    private MockMvc mockMvc;
    private Usuario usuarioAdmin;
    private String rawApiKeyAdmin;
    private String sessionCookieAdmin;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Reseta o rate limiter entre testes para evitar contaminação de estado (falhas acumuladas de testes anteriores)
        apiKeyRateLimiter.reset();

        // Cria ou atualiza usuário admin no banco
        usuarioAdmin = usuarioRepository.findByEmail_usuario("admin_integ@equadras.com")
                .orElseGet(() -> usuarioRepository.save(
                        Usuario.builder()
                                .nome_usuario("Admin Integracao")
                                .email_usuario("admin_integ@equadras.com")
                                .senha_usuario("$2a$10$N.ZOmG4i2OUPOgD8xDW3qux6w.2B8.0ZkJqPuhf45sC")
                                .phone_usuario("11988887777")
                                .role(Role.ADMIN)
                                .ativo(true)
                                .build()
                ));

        // Gera API-KEY real para o admin
        ApiKeyCriadaDTO apiKeyDto = apiKeyService.gerarOuRegenerar(usuarioAdmin.getId_usuario(), "127.0.0.1", "MockMvc");
        rawApiKeyAdmin = apiKeyDto.apiKey();

        // Gera token JWT de sessão web
        sessionCookieAdmin = jwtService.gerarToken(usuarioAdmin);
    }

    @Test
    @DisplayName("API-KEY válida deve acessar rota de negócio permitida (GET /quadras)")
    void apiKeyValidaDeveAcessarRotaDeNegocio() throws Exception {
        mockMvc.perform(get("/quadras")
                        .header("X-API-KEY", rawApiKeyAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("API-KEY via Authorization Bearer eq_... deve acessar rota de negócio permitida")
    void apiKeyViaBearerDeveAcessarRotaDeNegocio() throws Exception {
        mockMvc.perform(get("/quadras")
                        .header("Authorization", "Bearer " + rawApiKeyAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("API-KEY em rota de conta (GET /usuarios/me) deve retornar 403 Forbidden por isolamento de escopo")
    void apiKeyEmRotaDeContaDeveRetornar403() throws Exception {
        mockMvc.perform(get("/usuarios/me")
                        .header("X-API-KEY", rawApiKeyAdmin))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Acesso Proibido"));
    }

    @Test
    @DisplayName("API-KEY em rota de gerenciamento de chaves (POST /usuarios/api-key/regenerar) deve retornar 403 Forbidden")
    void apiKeyNaoPodeGerenciarPropriaConta() throws Exception {
        mockMvc.perform(post("/usuarios/api-key/regenerar")
                        .header("X-API-KEY", rawApiKeyAdmin))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("API-KEY inválida deve retornar 401 Unauthorized imediatamente")
    void apiKeyInvalidaDeveRetornar401() throws Exception {
        // Formato válido (eq_ + 43 caracteres), porém inexistente no banco
        mockMvc.perform(get("/quadras")
                        .header("X-API-KEY", "eq_" + "A".repeat(43)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Precedência Estrita: Requisição com X-API-KEY inválido E Cookie de sessão válido deve retornar 401 sem fallback")
    void precedenciaEstritaApiKeySobreCookie() throws Exception {
        mockMvc.perform(get("/quadras")
                        .header("X-API-KEY", "eq_chaveinvalida")
                        .cookie(new Cookie("equadras_session", sessionCookieAdmin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Invalidação Imediata: Ao regenerar a chave, chave anterior retorna 401 no mesmo instante")
    void invalidacaoImediataAoRegenerar() throws Exception {
        // Valida que a chave inicial funciona
        mockMvc.perform(get("/quadras")
                        .header("X-API-KEY", rawApiKeyAdmin))
                .andExpect(status().isOk());

        // Regenera a chave (invalidação atômica no banco)
        ApiKeyCriadaDTO novaChaveDto = apiKeyService.gerarOuRegenerar(usuarioAdmin.getId_usuario(), "127.0.0.1", "MockMvc");
        String novaApiKey = novaChaveDto.apiKey();

        // Chave antiga deve falhar com 401 imediatamente
        mockMvc.perform(get("/quadras")
                        .header("X-API-KEY", rawApiKeyAdmin))
                .andExpect(status().isUnauthorized());

        // Nova chave deve funcionar com 200
        mockMvc.perform(get("/quadras")
                        .header("X-API-KEY", novaApiKey))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("API-KEY de usuário inativo deve retornar 401 Unauthorized")
    void apiKeyUsuarioInativoDeveRetornar401() throws Exception {
        usuarioAdmin.setAtivo(false);
        usuarioRepository.save(usuarioAdmin);

        mockMvc.perform(get("/quadras")
                        .header("X-API-KEY", rawApiKeyAdmin))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Sessão Web via Cookie acessa rota de conta (GET /usuarios/me) com 200 OK")
    void sessaoWebAcessaRotaDeContaComSucesso() throws Exception {
        mockMvc.perform(get("/usuarios/me")
                        .cookie(new Cookie("equadras_session", sessionCookieAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email_usuario").value("admin_integ@equadras.com"));
    }

    @Test
    @DisplayName("Mutação via Sessão Web sem header X-Client deve retornar 403 Forbidden (proteção CSRF)")
    void mutacaoSessaoSemHeaderXClientDeveRetornar403() throws Exception {
        mockMvc.perform(post("/usuarios/api-key/regenerar")
                        .cookie(new Cookie("equadras_session", sessionCookieAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso Proibido"))
                .andExpect(jsonPath("$.detail").value("Cabeçalho X-Client obrigatório ausente ou inválido para requisições com mutação de estado."));
    }

    @Test
    @DisplayName("Mutação via Sessão Web com header X-Client: frontend deve retornar 200 OK")
    void mutacaoSessaoComHeaderXClientDevePassar() throws Exception {
        mockMvc.perform(post("/usuarios/api-key/regenerar")
                        .cookie(new Cookie("equadras_session", sessionCookieAdmin))
                        .header("X-Client", "frontend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey").exists())
                .andExpect(jsonPath("$.last4").exists());
    }

    @Test
    @DisplayName("Falha ao registrar último uso (throttling) não derruba requisição com API-KEY válida (200 OK)")
    void falhaAoRegistrarUltimoUsoNaoDerrubaRequisicao() throws Exception {
        Mockito.doThrow(new RuntimeException("Falha simulada de throttling"))
                .when(usuarioRepository)
                .atualizarUltimoUsoComThrottling(Mockito.eq(usuarioAdmin.getId_usuario()), Mockito.any(), Mockito.any());

        mockMvc.perform(get("/quadras")
                        .header("X-API-KEY", rawApiKeyAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Conflito entre X-API-KEY válida e Authorization Bearer eq_... divergente deve retornar 401 e contar falha no rate limiter")
    void conflitoApiKeyEAuthorizationDivergentesDeveRetornar401() throws Exception {
        mockMvc.perform(get("/quadras")
                        .header("X-API-KEY", rawApiKeyAdmin)
                        .header("Authorization", "Bearer eq_" + "B".repeat(43)))
                .andExpect(status().isUnauthorized());

        // A falha de conflito já conta 1 tentativa; mais 19 falhas do mesmo IP devem estourar o limite (20) e retornar 429
        for (int i = 0; i < 19; i++) {
            mockMvc.perform(get("/quadras").header("X-API-KEY", "eq_" + "D".repeat(43)))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(get("/quadras").header("X-API-KEY", "eq_" + "D".repeat(43)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("21 falhas consecutivas de API-KEY para o mesmo IP devem retornar 429 com Retry-After")
    void rateLimitPorIpAposFalhasConsecutivas() throws Exception {
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/quadras").header("X-API-KEY", "eq_" + "C".repeat(43)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(get("/quadras").header("X-API-KEY", "eq_" + "C".repeat(43)))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists("Retry-After"));
    }

    @Test
    @DisplayName("API-KEY com ROLE_CLIENT tentando mutar rota restrita a ADMIN (POST /quadras) deve retornar 403")
    void apiKeyClientTentandoMutarRotaAdminDeveRetornar403() throws Exception {
        Usuario cliente = usuarioRepository.findByEmail_usuario("cliente_integ@equadras.com")
                .orElseGet(() -> usuarioRepository.save(
                        Usuario.builder()
                                .nome_usuario("Cliente Integracao")
                                .email_usuario("cliente_integ@equadras.com")
                                .senha_usuario("$2a$10$N.ZOmG4i2OUPOgD8xDW3qux6w.2B8.0ZkJqPuhf45sC")
                                .phone_usuario("11977776666")
                                .role(Role.CLIENT)
                                .ativo(true)
                                .build()
                ));
        ApiKeyCriadaDTO apiKeyCliente = apiKeyService.gerarOuRegenerar(cliente.getId_usuario(), "127.0.0.1", "MockMvc");

        mockMvc.perform(post("/quadras")
                        .header("X-API-KEY", apiKeyCliente.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
