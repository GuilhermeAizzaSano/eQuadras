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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
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
        Usuario cliente = Usuario.builder()
                .id_usuario(888L)
                .email_usuario("cliente_cookie@teste.com")
                .role(Role.CLIENT)
                .build();
        String token = jwtService.gerarToken(cliente);

        mockMvc.perform(get("/quadras")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /usuarios/logout deve limpar o cookie de sessão com maxAge 0")
    void postLogoutDeveLimparCookie() throws Exception {
        mockMvc.perform(post("/usuarios/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("equadras_session"))
                .andExpect(cookie().maxAge("equadras_session", 0));
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
    @DisplayName("POST /usuarios com token ADMIN deve criar usuário com sucesso (201 Created)")
    void postUsuariosComTokenAdminDeveCriar() throws Exception {
        Usuario admin = Usuario.builder()
                .id_usuario(1L)
                .email_usuario("admin_sec@equadras.com")
                .role(Role.ADMIN)
                .build();
        String tokenAdmin = jwtService.gerarToken(admin);

        mockMvc.perform(post("/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome_usuario\":\"Novo Atleta\",\"email_usuario\":\"atleta_" + System.currentTimeMillis() + "@t.com\",\"senha_usuario\":\"SenhaForte123!\",\"phone_usuario\":\"11999999999\",\"role\":\"CLIENT\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /quadras com token CLIENT deve retornar 403 Forbidden")
    void postQuadrasComClienteDeveRetornar403() throws Exception {
        Usuario cliente = Usuario.builder()
                .id_usuario(999L)
                .email_usuario("cliente@teste.com")
                .role(Role.CLIENT)
                .build();
        String tokenCliente = jwtService.gerarToken(cliente);

        mockMvc.perform(post("/quadras")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenCliente))
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /notificacoes/admin com token ADMIN deve ser permitido")
    void getNotificacoesComAdminDeveSerPermitido() throws Exception {
        Usuario admin = Usuario.builder()
                .id_usuario(1L)
                .email_usuario("admin@teste.com")
                .role(Role.ADMIN)
                .build();
        String tokenAdmin = jwtService.gerarToken(admin);

        mockMvc.perform(get("/notificacoes/admin")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenAdmin))
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/quadras com token CLIENT deve retornar 200 OK (somente leitura permitido)")
    void getApiQuadrasComClienteDeveRetornar200() throws Exception {
        Usuario cliente = Usuario.builder()
                .id_usuario(999L)
                .email_usuario("cliente_api@teste.com")
                .role(Role.CLIENT)
                .build();
        String tokenCliente = jwtService.gerarToken(cliente);

        mockMvc.perform(get("/api/quadras")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/quadras com token CLIENT deve retornar 403 Forbidden (mutação na API bloqueada para cliente)")
    void postApiQuadrasComClienteDeveRetornar403() throws Exception {
        Usuario cliente = Usuario.builder()
                .id_usuario(999L)
                .email_usuario("cliente_api2@teste.com")
                .role(Role.CLIENT)
                .build();
        String tokenCliente = jwtService.gerarToken(cliente);

        mockMvc.perform(post("/api/quadras")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /usuarios/{outroId} com token CLIENT deve retornar 403 Forbidden (mitigação de IDOR)")
    void getUsuariosOutroIdComClienteDeveRetornar403() throws Exception {
        Usuario clienteLogado = Usuario.builder()
                .id_usuario(555L)
                .email_usuario("meu_usuario@teste.com")
                .role(Role.CLIENT)
                .build();
        String tokenCliente = jwtService.gerarToken(clienteLogado);

        mockMvc.perform(get("/usuarios/999")
                        .cookie(new jakarta.servlet.http.Cookie("equadras_session", tokenCliente))
                        .header("Authorization", "Bearer " + tokenCliente))
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
}