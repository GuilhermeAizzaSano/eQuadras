package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.security.JwtService;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class UsuarioListagemPaginadaTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private Cookie sessaoMaster;
    private Cookie sessaoAdminComum;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        Usuario master = usuarioRepository.findByEmail_usuario("gui@gmail.com")
                .orElseGet(() -> usuarioRepository.save(Usuario.builder().nome_usuario("Master")
                        .email_usuario("gui@gmail.com").phone_usuario("11944440000").senha_usuario("hash").role(Role.ADMIN).build()));
        Usuario comum = usuarioRepository.save(Usuario.builder().nome_usuario("Comum")
                .email_usuario("comum.pag@teste.com").phone_usuario("11944440001").senha_usuario("hash").role(Role.ADMIN).build());
        for (int i = 0; i < 3; i++) {
            usuarioRepository.save(Usuario.builder().nome_usuario("Cli " + i)
                    .email_usuario("cli.pag" + i + "@teste.com").phone_usuario("1194444010" + i).senha_usuario("hash").role(Role.CLIENT).build());
        }
        sessaoMaster = new Cookie("equadras_session", jwtService.gerarToken(master));
        sessaoAdminComum = new Cookie("equadras_session", jwtService.gerarToken(comum));
    }

    @Test
    @DisplayName("Com page: devolve página limitada no SQL")
    void devePaginarQuandoInformarPage() throws Exception {
        mockMvc.perform(get("/usuarios").cookie(sessaoMaster).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("Com page: tamanho acima de 50 é limitado")
    void deveLimitarTamanhoDaPagina() throws Exception {
        mockMvc.perform(get("/usuarios").cookie(sessaoMaster).param("page", "0").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    @DisplayName("Sem page: mantém a lista completa (contrato legado)")
    void deveManterListaSemPage() throws Exception {
        mockMvc.perform(get("/usuarios").cookie(sessaoMaster))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Paginado continua restrito ao master")
    void deveNegarPaginadoParaAdminComum() throws Exception {
        mockMvc.perform(get("/usuarios").cookie(sessaoAdminComum).param("page", "0"))
                .andExpect(status().isForbidden());
    }
}
