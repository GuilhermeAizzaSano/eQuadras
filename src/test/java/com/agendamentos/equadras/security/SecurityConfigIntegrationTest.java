package com.agendamentos.equadras.security;

import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class SecurityConfigIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private com.agendamentos.equadras.repository.UsuarioRepository usuarioRepository;

    private MockMvc mockMvc;
    private Usuario admin;
    private Usuario cliente;
    private String tokenAdmin;
    private String tokenCliente;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        admin = usuarioRepository.findByEmail_usuario("admin_sec@equadras.com")
                .orElseGet(() -> usuarioRepository.save(
                        Usuario.builder()
                                .nome_usuario("Admin Sec")
                                .email_usuario("admin_sec@equadras.com")
                                .senha_usuario("senha123")
                                .phone_usuario("11999990001")
                                .role(Role.ADMIN)
                                .ativo(true)
                                .build()
                ));

        cliente = usuarioRepository.findByEmail_usuario("cliente_sec@equadras.com")
                .orElseGet(() -> usuarioRepository.save(
                        Usuario.builder()
                                .nome_usuario("Cliente Sec")
                                .email_usuario("cliente_sec@equadras.com")
                                .senha_usuario("senha123")
                                .phone_usuario("11999990002")
                                .role(Role.CLIENT)
                                .ativo(true)
                                .build()
                ));

        tokenAdmin = jwtService.gerarToken(admin);
        tokenCliente = jwtService.gerarToken(cliente);
    }

    @Test
    @DisplayName("GET /quadras sem cookie deve retornar 401 Unauthorized (requisição forçada recusada)")
    void getQuadrasSemCookieDeveRetornar401() throws Exception {
        mockMvc.perform(get("/quadras"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /quadras com cookie de sessão válido deve retornar 200 OK")
    void getQuadrasComCookieDeveRetornar200() throws Exception {
        mockMvc.perform(get("/quadras")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenCliente)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /usuarios/logout com X-Client deve limpar o cookie e invalidar o token para acessos subsequentes")
    void postLogoutDeveLimparCookieEInvalidarToken() throws Exception {
        // 1. Acesso inicial válido com o token
        mockMvc.perform(get("/quadras")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenCliente)))
                .andExpect(status().isOk());

        // 2. Logout com o cookie
        mockMvc.perform(post("/usuarios/logout")
                        .header("X-Client", "frontend")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenCliente)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("equadras_session"))
                .andExpect(cookie().maxAge("equadras_session", 0));

        // 3. Tentar reutilizar o token anterior via cookie após logout -> deve retornar 401 Unauthorized
        mockMvc.perform(get("/quadras")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenCliente)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /usuarios sem cookie ou token deve retornar 401 Unauthorized")
    void getUsuariosSemTokenDeveSerNegado() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /usuarios sem token ADMIN deve retornar 403 Forbidden (auto-cadastro desabilitado)")
    void postUsuariosSemTokenAdminDeveSerBloqueado() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome_usuario\":\"Teste\",\"email_usuario\":\"test_public_" + System.currentTimeMillis() + "@t.com\",\"senha_usuario\":\"SenhaForte123!\",\"phone_usuario\":\"11999999999\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /usuarios com cookie de sessão ADMIN e X-Client deve criar usuário com sucesso (201 Created)")
    void postUsuariosComTokenAdminDeveCriar() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenAdmin))
                        .header("X-Client", "frontend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome_usuario\":\"Novo Atleta\",\"email_usuario\":\"atleta_" + System.currentTimeMillis() + "@t.com\",\"senha_usuario\":\"SenhaForte123!\",\"phone_usuario\":\"11999999999\",\"role\":\"CLIENT\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /quadras com cookie CLIENT deve retornar 403 Forbidden")
    void postQuadrasComClienteDeveRetornar403() throws Exception {
        mockMvc.perform(post("/quadras")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenCliente))
                        .header("X-Client", "frontend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /notificacoes/admin com cookie ADMIN deve ser permitido")
    void getNotificacoesComAdminDeveSerPermitido() throws Exception {
        mockMvc.perform(get("/notificacoes/admin")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenAdmin)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /notificacoes/todas com cookie ADMIN deve retornar 204 No Content")
    void deleteNotificacoesTodasComAdminDeveRetornar204() throws Exception {
        mockMvc.perform(delete("/notificacoes/todas")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenAdmin))
                        .header("X-Client", "frontend"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /notificacoes/todas com cookie CLIENT deve retornar 403 Forbidden")
    void deleteNotificacoesTodasComClientDeveRetornar403() throws Exception {
        mockMvc.perform(delete("/notificacoes/todas")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenCliente))
                        .header("X-Client", "frontend"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/quadras com cookie CLIENT deve retornar 200 OK (somente leitura permitido)")
    void getApiQuadrasComClienteDeveRetornar200() throws Exception {
        mockMvc.perform(get("/api/quadras")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenCliente)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/quadras com cookie CLIENT deve retornar 403 Forbidden (mutação na API bloqueada para cliente)")
    void postApiQuadrasComClienteDeveRetornar403() throws Exception {
        mockMvc.perform(post("/api/quadras")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenCliente))
                        .header("X-Client", "frontend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /usuarios/{outroId} com cookie CLIENT deve retornar 403 Forbidden (mitigação de IDOR)")
    void getUsuariosOutroIdComClienteDeveRetornar403() throws Exception {
        mockMvc.perform(get("/usuarios/" + admin.getId_usuario())
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenCliente)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Gerar tokens para testes de performance")
    void exportPerfTokens() {
        Usuario admin = Usuario.builder()
                .id_usuario(1L)
                .email_usuario("admin@equadras.com")
                .role(Role.ADMIN)
                .build();
        String tokenAdmin = jwtService.gerarToken(admin);

        Usuario cliente = Usuario.builder()
                .id_usuario(2L)
                .email_usuario("cliente2@teste.com")
                .role(Role.CLIENT)
                .build();
        String tokenClient = jwtService.gerarToken(cliente);

        assertNotNull(tokenAdmin);
        assertNotNull(tokenClient);
        assertFalse(tokenAdmin.isBlank());
        assertFalse(tokenClient.isBlank());
    }

    @Test
    @DisplayName("POST /usuarios/login com 5 falhas consecutivas para o mesmo e-mail deve retornar 429 Too Many Requests na 6ª tentativa")
    void loginComFalhasConsecutivasDeveBloquearPorRateLimitDeEmail() throws Exception {
        String emailVitima = "vitima_brute_force_" + System.currentTimeMillis() + "@equadras.com";

        // 5 tentativas erradas (retornam 400 Bad Request por credenciais incorretas)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/usuarios/login")
                            .header("X-Client", "frontend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email_usuario\":\"" + emailVitima + "\",\"senha_usuario\":\"senhaErrada" + i + "\"}"))
                    .andExpect(status().isBadRequest());
        }

        // 6ª tentativa deve ser bloqueada antes mesmo de verificar senha -> 429 Too Many Requests
        mockMvc.perform(post("/usuarios/login")
                        .header("X-Client", "frontend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email_usuario\":\"" + emailVitima + "\",\"senha_usuario\":\"qualquer\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists("Retry-After"));
    }
}