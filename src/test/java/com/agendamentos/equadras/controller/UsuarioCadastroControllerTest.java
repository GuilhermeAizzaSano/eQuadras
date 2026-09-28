package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.security.ApiKeyCache;
import com.agendamentos.equadras.security.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class UsuarioCadastroControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApiKeyService apiKeyService;
    @Autowired private ApiKeyCache apiKeyCache;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        apiKeyCache.invalidarTudo();
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENT", "ADMIN"})
    @DisplayName("ADMIN comum recebe 403 ao cadastrar usuário de qualquer role e nada é gravado")
    void adminComumNaoCadastra(String role) throws Exception {
        Usuario admin = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Admin Comum").email_usuario("comum.cadastro@teste.com").senha_usuario("x")
                .phone_usuario("11999990001").role(Role.ADMIN).build());
        String chave = apiKeyService.gerarOuRegenerar(admin.getId_usuario(), "127.0.0.1", "teste").apiKey();

        mockMvc.perform(post("/usuarios")
                        .header("X-API-KEY", chave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome_usuario":"Novo","email_usuario":"novo.cadastro@teste.com","senha_usuario":"senha123",
                                 "phone_usuario":"11988887777","role":"%s"}""".formatted(role)))
                .andExpect(status().isForbidden());

        assertTrue(usuarioRepository.findByEmail_usuario("novo.cadastro@teste.com").isEmpty());
    }
}
