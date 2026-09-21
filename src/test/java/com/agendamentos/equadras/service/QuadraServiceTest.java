package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.QuadraCriacaoDTO;
import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuadraServiceTest {

    @Mock
    private QuadraRepository quadraRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private QuadraService quadraService;

    private Usuario adminComum;
    private Usuario masterAdmin;
    private Quadra quadraAdminComum;

    @BeforeEach
    void setUp() {
        adminComum = Usuario.builder()
                .id_usuario(1L)
                .nome_usuario("Admin Comum")
                .email_usuario("admin@equadras.com")
                .role(Role.ADMIN)
                .build();

        masterAdmin = Usuario.builder()
                .id_usuario(99L)
                .nome_usuario("Master Admin")
                .email_usuario("gui@gmail.com")
                .role(Role.ADMIN)
                .build();

        quadraAdminComum = Quadra.builder()
                .id_quadra(10L)
                .nome("Quadra de Futebol")
                .tipoEsporte(TipoEsporte.FUTEBOL)
                .valorHora(BigDecimal.valueOf(100.00))
                .ativa(true)
                .admin(adminComum)
                .fotos(new ArrayList<>())
                .disponibilidades(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Master Admin deve listar todas as quadras do sistema")
    void deveListarTodasAsQuadrasQuandoMasterAdmin() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(masterAdmin));
        when(quadraRepository.findAllWithAdminEFotos()).thenReturn(List.of(quadraAdminComum));

        List<QuadraResponseDTO> resultado = quadraService.listar(99L, null, null, null);

        assertEquals(1, resultado.size());
        verify(quadraRepository, times(1)).findAllWithAdminEFotos();
        verify(quadraRepository, never()).findByAdminId(99L);
    }

    @Test
    @DisplayName("Admin comum deve listar apenas suas próprias quadras")
    void deveListarApenasSuasQuadrasQuandoAdminComum() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(adminComum));
        when(quadraRepository.findByAdminId(1L)).thenReturn(List.of(quadraAdminComum));

        List<QuadraResponseDTO> resultado = quadraService.listar(1L, null, null, null);

        assertEquals(1, resultado.size());
        verify(quadraRepository, times(1)).findByAdminId(1L);
        verify(quadraRepository, never()).findAllWithAdminEFotos();
    }

    @Test
    @DisplayName("Master Admin deve conseguir editar quadra pertencente a outro administrador")
    void devePermitirMasterAdminEditarQuadraDeOutroAdmin() {
        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadraAdminComum));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(masterAdmin));
        when(quadraRepository.save(any(Quadra.class))).thenAnswer(i -> i.getArgument(0));

        QuadraCriacaoDTO dto = new QuadraCriacaoDTO(
                "Quadra Atualizada pelo Master",
                TipoEsporte.FUTEBOL,
                BigDecimal.valueOf(150.00),
                "01001-000",
                "Rua A",
                "Centro",
                "SP",
                "SP",
                -23.55,
                -46.63,
                "Descrição",
                null,
                List.of(),
                List.of()
        );

        QuadraResponseDTO atualizada = quadraService.editar(10L, dto, 99L);

        assertNotNull(atualizada);
        assertEquals("Quadra Atualizada pelo Master", atualizada.nome());
        verify(quadraRepository, times(1)).save(quadraAdminComum);
    }

    @Test
    @DisplayName("Admin comum não deve conseguir editar quadra de outro administrador")
    void deveBarrarAdminComumEditandoQuadraAlheia() {
        Usuario outroAdmin = Usuario.builder()
                .id_usuario(2L)
                .nome_usuario("Outro Admin")
                .email_usuario("outro@equadras.com")
                .role(Role.ADMIN)
                .build();

        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadraAdminComum));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(outroAdmin));

        QuadraCriacaoDTO dto = new QuadraCriacaoDTO(
                "Tentativa Hacker",
                TipoEsporte.FUTEBOL,
                BigDecimal.valueOf(150.00),
                "01001-000",
                "Rua A",
                "Centro",
                "SP",
                "SP",
                -23.55,
                -46.63,
                "Descrição",
                null,
                List.of(),
                List.of()
        );

        assertThrows(IllegalArgumentException.class, () -> quadraService.editar(10L, dto, 2L));
        verify(quadraRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve filtrar quadras por proximidade calculando os limites de Bounding Box corretamente")
    void deveFiltrarQuadrasPorProximidadeComBoundingBox() {
        Double lat = -23.5505;
        Double lng = -46.6333;
        Double raioKm = 5.0;

        when(quadraRepository.findByAtivaTrueAndProximidadeMenorQue(
                eq(lat), eq(lng), eq(raioKm),
                anyDouble(), anyDouble(), anyDouble(), anyDouble()
        )).thenReturn(List.of(quadraAdminComum));

        List<Quadra> resultado = quadraService.filtrarQuadrasEntidades(null, lat, lng, raioKm, null, null, null, null, null);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(quadraRepository, times(1)).findByAtivaTrueAndProximidadeMenorQue(
                eq(lat), eq(lng), eq(raioKm),
                anyDouble(), anyDouble(), anyDouble(), anyDouble()
        );
    }

    @Test
    @DisplayName("Deve filtrar quadras por nome ignorando maiúsculas e acentos")
    void deveFiltrarQuadrasPorNome() {
        Quadra q1 = Quadra.builder().id_quadra(1L).nome("Arena Sunset Vôlei").logradouro("Rua 13").bairro("Samambaia").ativa(true).fotos(new ArrayList<>()).disponibilidades(new ArrayList<>()).build();
        Quadra q2 = Quadra.builder().id_quadra(2L).nome("Complexo Esportivo do Bosque").logradouro("Av Brasil").bairro("Centro").ativa(true).fotos(new ArrayList<>()).disponibilidades(new ArrayList<>()).build();

        when(quadraRepository.findByAtivaTrue()).thenReturn(List.of(q1, q2));

        List<Quadra> resultado = quadraService.filtrarQuadrasEntidades(null, null, null, null, null, "sunset", null, null, null, null);

        assertEquals(1, resultado.size());
        assertEquals("Arena Sunset Vôlei", resultado.get(0).getNome());
    }

    @Test
    @DisplayName("Deve filtrar quadras por endereço casando com logradouro ou bairro")
    void deveFiltrarQuadrasPorEndereco() {
        Quadra q1 = Quadra.builder().id_quadra(1L).nome("Quadra A").logradouro("Rua dos Aviadores, 120").bairro("Jardim Municipal").ativa(true).fotos(new ArrayList<>()).disponibilidades(new ArrayList<>()).build();
        Quadra q2 = Quadra.builder().id_quadra(2L).nome("Quadra B").logradouro("Av. Brasília, 934").bairro("JACB II").ativa(true).fotos(new ArrayList<>()).disponibilidades(new ArrayList<>()).build();

        when(quadraRepository.findByAtivaTrue()).thenReturn(List.of(q1, q2));

        // Busca por logradouro
        List<Quadra> resLogradouro = quadraService.filtrarQuadrasEntidades(null, null, null, null, null, null, "aviadores", null, null, null);
        assertEquals(1, resLogradouro.size());
        assertEquals("Quadra A", resLogradouro.get(0).getNome());

        // Busca por bairro
        List<Quadra> resBairro = quadraService.filtrarQuadrasEntidades(null, null, null, null, null, null, "jacb", null, null, null);
        assertEquals(1, resBairro.size());
        assertEquals("Quadra B", resBairro.get(0).getNome());
    }

    @Test
    @DisplayName("Deve combinar filtros de nome e endereço simultaneamente")
    void deveCombinarFiltroNomeEEndereco() {
        Quadra q1 = Quadra.builder().id_quadra(1L).nome("Arena Beach").logradouro("Rua das Rosas").bairro("Jardim Oiti").ativa(true).fotos(new ArrayList<>()).disponibilidades(new ArrayList<>()).build();
        Quadra q2 = Quadra.builder().id_quadra(2L).nome("Arena Gol").logradouro("Rua das Rosas").bairro("Centro").ativa(true).fotos(new ArrayList<>()).disponibilidades(new ArrayList<>()).build();

        when(quadraRepository.findByAtivaTrue()).thenReturn(List.of(q1, q2));

        List<Quadra> resultado = quadraService.filtrarQuadrasEntidades(null, null, null, null, null, "Beach", "oiti", null, null, null);
        assertEquals(1, resultado.size());
        assertEquals("Arena Beach", resultado.get(0).getNome());
    }

    @Test
    @DisplayName("Deve listar quadras de forma paginada respeitando limite por página")
    void deveListarQuadrasPaginadas() {
        List<Quadra> quadras = new ArrayList<>();
        for (long i = 1; i <= 14; i++) {
            quadras.add(Quadra.builder()
                    .id_quadra(i)
                    .nome("Quadra " + i)
                    .ativa(true)
                    .fotos(new ArrayList<>())
                    .disponibilidades(new ArrayList<>())
                    .build());
        }

        when(quadraRepository.findByAtivaTrue()).thenReturn(quadras);

        Pageable pageable = PageRequest.of(0, 6);
        Page<QuadraResponseDTO> primeiraPagina = quadraService.listar(null, null, null, null, null, null, null, null, null, null, pageable);

        assertEquals(6, primeiraPagina.getContent().size());
        assertEquals(14, primeiraPagina.getTotalElements());
        assertEquals(3, primeiraPagina.getTotalPages());
        assertEquals(0, primeiraPagina.getNumber());
        assertEquals("Quadra 1", primeiraPagina.getContent().get(0).nome());

        Pageable pageableSegunda = PageRequest.of(1, 6);
        Page<QuadraResponseDTO> segundaPagina = quadraService.listar(null, null, null, null, null, null, null, null, null, null, pageableSegunda);
        assertEquals(6, segundaPagina.getContent().size());
        assertEquals("Quadra 7", segundaPagina.getContent().get(0).nome());

        Pageable pageableTerceira = PageRequest.of(2, 6);
        Page<QuadraResponseDTO> terceiraPagina = quadraService.listar(null, null, null, null, null, null, null, null, null, null, pageableTerceira);
        assertEquals(2, terceiraPagina.getContent().size());
        assertEquals("Quadra 13", terceiraPagina.getContent().get(0).nome());
    }

    @Test
    @DisplayName("Deve aplicar paginação sobre os resultados dos filtros de CEP/endereço")
    void deveAplicarPaginacaoAposFiltros() {
        Quadra q1 = Quadra.builder().id_quadra(1L).nome("Quadra Centro 1").logradouro("Rua Central").bairro("Centro").ativa(true).fotos(new ArrayList<>()).disponibilidades(new ArrayList<>()).build();
        Quadra q2 = Quadra.builder().id_quadra(2L).nome("Quadra Bairro 1").logradouro("Rua Norte").bairro("Norte").ativa(true).fotos(new ArrayList<>()).disponibilidades(new ArrayList<>()).build();
        Quadra q3 = Quadra.builder().id_quadra(3L).nome("Quadra Centro 2").logradouro("Av Central").bairro("Centro").ativa(true).fotos(new ArrayList<>()).disponibilidades(new ArrayList<>()).build();

        when(quadraRepository.findByAtivaTrue()).thenReturn(List.of(q1, q2, q3));

        Pageable pageable = PageRequest.of(0, 6);
        Page<QuadraResponseDTO> pagina = quadraService.listar(null, null, null, null, null, null, "Centro", null, null, null, pageable);

        assertEquals(2, pagina.getTotalElements());
        assertEquals(1, pagina.getTotalPages());
        assertEquals(2, pagina.getContent().size());
    }
}
