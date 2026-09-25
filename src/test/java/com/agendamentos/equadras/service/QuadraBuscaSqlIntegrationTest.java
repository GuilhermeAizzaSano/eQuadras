package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.model.entity.DisponibilidadeDia;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class QuadraBuscaSqlIntegrationTest {

    @Autowired
    private QuadraBuscaService quadraBuscaService;

    @Autowired
    private QuadraRepository quadraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private Usuario adminDono;
    private Usuario outroAdmin;
    private Usuario master;
    private Usuario cliente;
    // O H2 é compartilhado entre classes de teste; as asserções consideram só as quadras semeadas aqui
    private final Set<String> semeadas = new java.util.HashSet<>();

    @BeforeEach
    void setUp() {
        adminDono = salvarUsuario("dono.busca@teste.com", "11911110001", Role.ADMIN);
        outroAdmin = salvarUsuario("outro.busca@teste.com", "11911110002", Role.ADMIN);
        master = usuarioRepository.findByEmail_usuario("gui@gmail.com")
                .orElseGet(() -> salvarUsuario("gui@gmail.com", "11911110003", Role.ADMIN));
        cliente = salvarUsuario("cliente.busca@teste.com", "11911110004", Role.CLIENT);

        salvarQuadra("Sunset Arena", TipoEsporte.FUTSAL, "Av. dos Aviadores", "Centro", "São Paulo", "01001-000", true, adminDono, -23.5500, -46.6300);
        salvarQuadra("Quadra Jacó", TipoEsporte.FUTEBOL, "Rua das Palmeiras", "Jacaré", "Campinas", "13000-100", true, adminDono);
        salvarQuadra("Quadra Inativa", TipoEsporte.BEACH_TENNIS, "Rua Fechada", "Centro", "São Paulo", "01002-000", false, adminDono, -23.5510, -46.6310);
        salvarQuadra("Arena Norte", TipoEsporte.BASQUETE, "Rua Norte", "Ponta da Praia", "Santos", "11000-000", true, outroAdmin, -23.5520, -46.6320);
        limparContexto();
    }

    private Usuario salvarUsuario(String email, String telefone, Role role) {
        return usuarioRepository.save(Usuario.builder()
                .nome_usuario(email)
                .email_usuario(email)
                .phone_usuario(telefone)
                .senha_usuario("hash")
                .role(role)
                .build());
    }

    private void salvarQuadra(String nome, TipoEsporte esporte, String logradouro, String bairro,
                              String cidade, String cep, boolean ativa, Usuario admin) {
        salvarQuadra(nome, esporte, logradouro, bairro, cidade, cep, ativa, admin, null, null);
    }

    private void salvarQuadra(String nome, TipoEsporte esporte, String logradouro, String bairro,
                              String cidade, String cep, boolean ativa, Usuario admin, Double latitude, Double longitude) {
        semeadas.add(nome);
        List<DisponibilidadeDia> disponibilidades = new ArrayList<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            disponibilidades.add(new DisponibilidadeDia(dia, LocalTime.of(8, 0), LocalTime.of(22, 0)));
        }
        Quadra salva = quadraRepository.save(Quadra.builder()
                .nome(nome)
                .tipoEsporte(esporte)
                .valorHora(BigDecimal.valueOf(100))
                .logradouro(logradouro)
                .bairro(bairro)
                .cidade(cidade)
                .cep(cep)
                .latitude(latitude)
                .longitude(longitude)
                .ativa(ativa)
                .admin(admin)
                .fotos(new ArrayList<>(List.of("http://foto/" + nome)))
                .disponibilidades(disponibilidades)
                .build());
        // @PrePersist sempre cadastra a quadra ativa; a inativação é uma alteração posterior
        if (!ativa) {
            salva.setAtiva(false);
            quadraRepository.save(salva);
        }
    }

    private void limparContexto() {
        entityManager.flush();
        entityManager.clear();
    }

    private Set<String> nomes(Long usuarioId, String esporte, String nome, String endereco,
                              String cidade, String bairro, String cep) {
        return quadraBuscaService.filtrarQuadrasEntidades(usuarioId, null, null, null, esporte, nome, endereco, cidade, bairro, cep)
                .stream()
                .map(Quadra::getNome)
                .filter(semeadas::contains)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Deve filtrar por nome ignorando acentos e caixa")
    void deveFiltrarPorNome() {
        assertEquals(Set.of("Sunset Arena"), nomes(null, null, "sunset", null, null, null, null));
        assertEquals(Set.of("Quadra Jacó"), nomes(null, null, "JACO", null, null, null, null));
    }

    @Test
    @DisplayName("Deve filtrar por endereço no logradouro ou no bairro")
    void deveFiltrarPorEndereco() {
        assertEquals(Set.of("Sunset Arena"), nomes(null, null, null, "aviadores", null, null, null));
        assertEquals(Set.of("Quadra Jacó"), nomes(null, null, null, "jacare", null, null, null));
    }

    @Test
    @DisplayName("Deve combinar filtros de nome e endereço simultaneamente")
    void deveCombinarFiltroNomeEEndereco() {
        assertEquals(Set.of("Arena Norte"), nomes(null, null, "arena", "ponta", null, null, null));
        assertTrue(nomes(null, null, "sunset", "ponta", null, null, null).isEmpty());
    }

    @Test
    @DisplayName("Deve filtrar por cidade, bairro e CEP com máscara")
    void deveFiltrarPorCidadeBairroECep() {
        assertEquals(Set.of("Sunset Arena"), nomes(null, null, null, null, "sao paulo", null, null));
        assertEquals(Set.of("Arena Norte"), nomes(null, null, null, null, null, "ponta", null));
        assertEquals(Set.of("Quadra Jacó"), nomes(null, null, null, null, null, null, "13000100"));
    }

    @Test
    @DisplayName("Deve interpretar o esporte e retornar vazio para esporte desconhecido")
    void deveFiltrarPorEsporte() {
        assertEquals(Set.of("Sunset Arena"), nomes(null, "futebol de salão", null, null, null, null, null));
        assertTrue(nomes(null, "xadrez", null, null, null, null, null).isEmpty());
    }

    @Test
    @DisplayName("Deve aplicar o escopo por perfil: cliente vê ativas, admin vê as suas e master vê todas")
    void deveAplicarEscopoPorPerfil() {
        assertEquals(Set.of("Sunset Arena", "Quadra Jacó", "Arena Norte"),
                nomes(cliente.getId_usuario(), null, null, null, null, null, null));
        assertEquals(Set.of("Sunset Arena", "Quadra Jacó", "Quadra Inativa"),
                nomes(adminDono.getId_usuario(), null, null, null, null, null, null));
        assertEquals(Set.of("Sunset Arena", "Quadra Jacó", "Quadra Inativa", "Arena Norte"),
                nomes(master.getId_usuario(), null, null, null, null, null, null));
    }

    @Test
    @DisplayName("Número de queries da busca não deve crescer com a quantidade de quadras")
    void naoDeveVariarQueriesComQuantidadeDeQuadras() {
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        statistics.clear();
        assertEquals(3, nomes(null, null, null, null, null, null, null).size());
        long queriesCom3 = statistics.getPrepareStatementCount();

        for (int i = 0; i < 7; i++) {
            salvarQuadra("Extra " + i, TipoEsporte.FUTEBOL, "Rua Extra", "Bairro Extra", "Extra", "22000-00" + i, true, adminDono);
        }
        limparContexto();

        statistics.clear();
        assertEquals(10, nomes(null, null, null, null, null, null, null).size());
        long queriesCom10 = statistics.getPrepareStatementCount();

        assertEquals(queriesCom3, queriesCom10,
                "A busca de quadras não pode executar uma consulta por quadra (N+1)");
    }

    private List<String> nomesPorProximidade(Long usuarioId, String nome) {
        return quadraBuscaService.filtrarQuadrasEntidades(usuarioId, -23.5500, -46.6300, 5.0, null, nome, null, null, null, null)
                .stream()
                .map(Quadra::getNome)
                .filter(semeadas::contains)
                .toList();
    }

    @Test
    @DisplayName("Deve buscar por proximidade em ordem de distância, só ativas, aplicando filtros e escopo")
    void deveBuscarPorProximidadeComFiltros() {
        assertEquals(List.of("Sunset Arena", "Arena Norte"), nomesPorProximidade(null, null));
        assertEquals(List.of("Arena Norte"), nomesPorProximidade(null, "norte"));
        assertEquals(List.of("Sunset Arena"), nomesPorProximidade(adminDono.getId_usuario(), null));
    }

    @Test
    @DisplayName("Número de queries da busca por proximidade não deve crescer com a quantidade de quadras")
    void naoDeveVariarQueriesNaBuscaPorProximidade() {
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        statistics.clear();
        assertEquals(2, nomesPorProximidade(null, null).size());
        long queriesCom2 = statistics.getPrepareStatementCount();

        for (int i = 0; i < 8; i++) {
            salvarQuadra("Perto " + i, TipoEsporte.FUTEBOL, "Rua Perto", "Centro", "São Paulo", "01003-00" + i, true, adminDono, -23.5530 - i * 0.0001, -46.6330);
        }
        limparContexto();

        statistics.clear();
        assertEquals(10, nomesPorProximidade(null, null).size());
        long queriesCom10 = statistics.getPrepareStatementCount();

        assertEquals(queriesCom2, queriesCom10,
                "A busca por proximidade não pode executar uma consulta por quadra (N+1)");
    }

    private List<String> nomesDaPagina(Page<QuadraResponseDTO> pagina) {
        return pagina.getContent().stream()
                .map(QuadraResponseDTO::nome)
                .filter(semeadas::contains)
                .toList();
    }

    @Test
    @DisplayName("Deve paginar a busca por proximidade no SQL mantendo a ordem por distância")
    void devePaginarBuscaPorProximidadeNoSql() {
        salvarQuadra("Arena Sul", TipoEsporte.FUTEBOL, "Rua Sul", "Centro", "São Paulo", "01004-000", true, outroAdmin, -23.5550, -46.6330);
        limparContexto();

        Page<QuadraResponseDTO> doDono = quadraBuscaService.listar(adminDono.getId_usuario(), -23.5500, -46.6300, 5.0,
                null, null, null, null, null, null, PageRequest.of(0, 1));
        assertEquals(List.of("Sunset Arena"), nomesDaPagina(doDono));
        assertEquals(1, doDono.getTotalElements());

        // O H2 é compartilhado: outras classes podem deixar quadras no raio. Percorre todas as páginas
        // e confere a ordem só das semeadas; o total deve bater com a mesma busca sem paginação.
        int totalSemPaginacao = quadraBuscaService.filtrarQuadrasEntidades(cliente.getId_usuario(), -23.5500, -46.6300, 5.0,
                null, "arena", null, null, null, null).size();
        Page<QuadraResponseDTO> pagina = quadraBuscaService.listar(cliente.getId_usuario(), -23.5500, -46.6300, 5.0,
                null, "arena", null, null, null, null, PageRequest.of(0, 2));
        List<String> semeadasEmOrdem = new ArrayList<>(nomesDaPagina(pagina));
        for (int p = 1; p < pagina.getTotalPages(); p++) {
            semeadasEmOrdem.addAll(nomesDaPagina(quadraBuscaService.listar(cliente.getId_usuario(), -23.5500, -46.6300, 5.0,
                    null, "arena", null, null, null, null, PageRequest.of(p, 2))));
        }

        assertEquals(List.of("Sunset Arena", "Arena Norte", "Arena Sul"), semeadasEmOrdem);
        assertEquals(totalSemPaginacao, pagina.getTotalElements());
        assertEquals((totalSemPaginacao + 1) / 2, pagina.getTotalPages());
        assertEquals(2, pagina.getContent().size());
    }

    @Test
    @DisplayName("Não deve retornar quadra inativa na busca por proximidade nem para o admin dono")
    void naoDeveRetornarInativaNaBuscaPorProximidade() {
        assertTrue(nomesPorProximidade(adminDono.getId_usuario(), "inativa").isEmpty());
    }

    private List<String> nomesComRaio(Double raioKm) {
        return quadraBuscaService.filtrarQuadrasEntidades(null, -23.5500, -46.6300, raioKm, null, null, null, null, null, null)
                .stream()
                .map(Quadra::getNome)
                .filter(semeadas::contains)
                .toList();
    }

    @Test
    @DisplayName("Deve limitar o raio a 50 km e usar 2 km quando ausente ou inválido")
    void deveLimitarRaioDaBusca() {
        // ~111 km e ~3,3 km do ponto de busca
        salvarQuadra("Arena Distante", TipoEsporte.FUTEBOL, "Rua Longe", "Centro", "Jundiaí", "13200-000", true, outroAdmin, -22.5500, -46.6300);
        salvarQuadra("Arena Tres Km", TipoEsporte.FUTEBOL, "Rua Tres", "Centro", "São Paulo", "01005-000", true, outroAdmin, -23.5800, -46.6300);
        limparContexto();

        List<String> raioEnorme = nomesComRaio(500.0);
        assertTrue(!raioEnorme.contains("Arena Distante"), "Raio acima de 50 km deve ser limitado");
        assertTrue(raioEnorme.contains("Arena Tres Km"));
        assertEquals(List.of("Sunset Arena", "Arena Norte"), nomesComRaio(0.0));
        assertEquals(List.of("Sunset Arena", "Arena Norte"), nomesComRaio(null));
    }

    @Test
    @DisplayName("Número de queries da busca por proximidade paginada não deve crescer com a quantidade de quadras")
    void naoDeveVariarQueriesNaBuscaPorProximidadePaginada() {
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        statistics.clear();
        quadraBuscaService.listar(null, -23.5500, -46.6300, 5.0, null, null, null, null, null, null, PageRequest.of(0, 2));
        long queriesAntes = statistics.getPrepareStatementCount();

        for (int i = 0; i < 8; i++) {
            salvarQuadra("Pagina " + i, TipoEsporte.FUTEBOL, "Rua Pagina", "Centro", "São Paulo", "01006-00" + i, true, adminDono, -23.5540 - i * 0.0001, -46.6340);
        }
        limparContexto();

        statistics.clear();
        Page<QuadraResponseDTO> pagina = quadraBuscaService.listar(null, -23.5500, -46.6300, 5.0, null, null, null, null, null, null, PageRequest.of(0, 2));
        long queriesDepois = statistics.getPrepareStatementCount();

        assertEquals(2, pagina.getContent().size());
        assertEquals(queriesAntes, queriesDepois,
                "A paginação geográfica não pode carregar nem consultar por quadra");
    }
}