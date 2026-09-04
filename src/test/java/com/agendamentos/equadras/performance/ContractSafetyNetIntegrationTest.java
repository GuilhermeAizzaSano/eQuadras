package com.agendamentos.equadras.performance;

import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.security.JwtService;
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
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class ContractSafetyNetIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private QuadraRepository quadraRepository;

    @Autowired
    private com.agendamentos.equadras.repository.AgendamentoRepository agendamentoRepository;

    private MockMvc mockMvc;
    private String adminToken;
    private Long quadraId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        agendamentoRepository.deleteAll();
        quadraRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario admin = usuarioRepository.save(Usuario.builder()
                .nome_usuario("Admin Contrato")
                .email_usuario("admin_contract_" + System.currentTimeMillis() + "@equadras.com")
                .senha_usuario("hash123")
                .phone_usuario("11999990000")
                .role(Role.ADMIN)
                .build());

        adminToken = jwtService.gerarToken(admin);

        Quadra quadra = quadraRepository.save(Quadra.builder()
                .nome("Arena Contrato")
                .tipoEsporte(TipoEsporte.FUTEBOL)
                .valorHora(new BigDecimal("120.00"))
                .ativa(true)
                .cep("01001-000")
                .logradouro("Rua Teste Contrato")
                .bairro("Centro")
                .cidade("São Paulo")
                .estado("SP")
                .latitude(-23.550520)
                .longitude(-46.633308)
                .descricao("Quadra para teste de contrato JSON")
                .admin(admin)
                .fotos(new java.util.ArrayList<>(List.of("/uploads/foto1.jpg", "/uploads/foto2.jpg")))
                .build());

        quadraId = quadra.getId_quadra();
    }

    @Test
    @DisplayName("Garante contrato JSON de GET /quadras")
    void devePreservarContratoGetQuadras() throws Exception {
        mockMvc.perform(get("/quadras").header("X-Client", "frontend").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].id_quadra").exists())
                .andExpect(jsonPath("$[0].nome").exists())
                .andExpect(jsonPath("$[0].tipoEsporte").exists())
                .andExpect(jsonPath("$[0].valorHora").exists())
                .andExpect(jsonPath("$[0].ativa").exists())
                .andExpect(jsonPath("$[0].cep").exists())
                .andExpect(jsonPath("$[0].logradouro").exists())
                .andExpect(jsonPath("$[0].bairro").exists())
                .andExpect(jsonPath("$[0].cidade").exists())
                .andExpect(jsonPath("$[0].estado").exists())
                .andExpect(jsonPath("$[0].fotos").isArray());
    }

    @Test
    @DisplayName("Garante contrato JSON de GET /agendamentos")
    void devePreservarContratoGetAgendamentos() throws Exception {
        mockMvc.perform(get("/agendamentos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Garante contrato JSON de GET /agendamentos/quadra/{id}/horarios-disponiveis")
    void devePreservarContratoHorariosDisponiveis() throws Exception {
        mockMvc.perform(get("/agendamentos/quadra/" + quadraId + "/horarios-disponiveis")
                        .param("data", LocalDate.now().plusDays(1).toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].inicio").exists())
                .andExpect(jsonPath("$[0].fim").exists())
                .andExpect(jsonPath("$[0].disponivel").exists())
                .andExpect(jsonPath("$[0].motivo").exists());
    }
}