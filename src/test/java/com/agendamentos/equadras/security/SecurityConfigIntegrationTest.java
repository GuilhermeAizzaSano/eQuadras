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
    @DisplayName("GET /quadras deve ser acessível publicamente sem token")
    void getQuadrasDeveSerPublico() throws Exception {
        mockMvc.perform(get("/quadras"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /usuarios deve retornar 401 ou 403 sem token")
    void getUsuariosSemTokenDeveSerNegado() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isForbidden());
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
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
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