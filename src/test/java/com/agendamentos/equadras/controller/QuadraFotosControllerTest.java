package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.QuadraRepository;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Caracterização do contrato legado de consulta de fotos (bot/integrações): o JSON não pode mudar
@SpringBootTest
@Transactional
class QuadraFotosControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private QuadraRepository quadraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private Cookie sessao;
    private Quadra fotoUnica;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        Usuario admin = usuarioRepository.save(Usuario.builder()
                .nome_usuario("fotos.admin").email_usuario("fotos.admin@teste.com")
                .phone_usuario("11933330001").senha_usuario("hash").role(Role.ADMIN).build());
        sessao = new Cookie("equadras_session", jwtService.gerarToken(admin));

        fotoUnica = quadraRepository.save(Quadra.builder().nome("Zeta Fotos Unica").tipoEsporte(TipoEsporte.VOLEI)
                .valorHora(BigDecimal.TEN).ativa(true).admin(admin).bairro("Centro")
                .fotos(new ArrayList<>(List.of("http://foto/z1", "http://foto/z2"))).build());
        quadraRepository.save(Quadra.builder().nome("Omega Fotos A").tipoEsporte(TipoEsporte.TENIS)
                .valorHora(BigDecimal.TEN).ativa(true).admin(admin).cidade("Campinas").bairro("Taquaral")
                .fotos(new ArrayList<>(List.of("http://foto/o1"))).build());
        quadraRepository.save(Quadra.builder().nome("Omega Fotos B").tipoEsporte(TipoEsporte.TENIS)
                .valorHora(BigDecimal.TEN).ativa(true).admin(admin).cidade("Campinas").bairro("Taquaral")
                .fotos(new ArrayList<>()).build());
    }

    @Test
    @DisplayName("Por id: objeto único com campos normalizados (cidade nula vira vazio)")
    void porIdDeveRetornarObjeto() throws Exception {
        mockMvc.perform(get("/quadras/{id}/fotos", fotoUnica.getId_quadra()).cookie(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_quadra").value(fotoUnica.getId_quadra()))
                .andExpect(jsonPath("$.nome").value("Zeta Fotos Unica"))
                .andExpect(jsonPath("$.tipoEsporte").value("VOLEI"))
                .andExpect(jsonPath("$.cidade").value(""))
                .andExpect(jsonPath("$.bairro").value("Centro"))
                .andExpect(jsonPath("$.fotos.length()").value(2));
    }

    @Test
    @DisplayName("Filtro com 1 resultado: objeto único (não lista)")
    void filtroComUmResultadoDeveRetornarObjeto() throws Exception {
        mockMvc.perform(get("/quadras/fotos").param("nomeQuadra", "zeta fotos").cookie(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Zeta Fotos Unica"));
    }

    @Test
    @DisplayName("Filtro com vários resultados: lista")
    void filtroComVariosResultadosDeveRetornarLista() throws Exception {
        mockMvc.perform(get("/quadras/fotos").param("nome", "omega fotos").param("esporte", "tenis").cookie(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fotos").isArray());
    }

    @Test
    @DisplayName("Filtro sem resultado: objeto com fotos vazio")
    void filtroSemResultadoDeveRetornarFotosVazio() throws Exception {
        mockMvc.perform(get("/quadras/fotos").param("nome", "nao-existe-xyz").cookie(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotos.length()").value(0))
                .andExpect(jsonPath("$.nome").doesNotExist());
    }

    @Test
    @DisplayName("Sem filtro: lista de todas as ativas")
    void semFiltroDeveRetornarLista() throws Exception {
        mockMvc.perform(get("/quadras/fotos").cookie(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Id inexistente: 400 com a mensagem atual")
    void idInexistenteDeveRetornar400() throws Exception {
        mockMvc.perform(get("/quadras/{id}/fotos", 987654321L).cookie(sessao))
                .andExpect(status().isBadRequest());
    }
}
