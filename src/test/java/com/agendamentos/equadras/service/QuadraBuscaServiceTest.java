package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.dto.response.QuadraResumoResponseDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.QuadraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuadraBuscaServiceTest {

    @Mock
    private QuadraRepository quadraRepository;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private QuadraBuscaService quadraBuscaService;

    private Usuario adminComum;
    private Usuario masterAdmin;
    private Quadra quadraAdminComum;

    @BeforeEach
    void setUp() {
        adminComum = Usuario.builder()
                .id_usuario(1L)
                .nome_usuario("Admin Comum")
                .email_usuario("admin@test.com")
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
                .nome("Quadra do Admin")
                .tipoEsporte(TipoEsporte.FUTEBOL)
                .valorHora(new BigDecimal("100.00"))
                .ativa(true)
                .admin(adminComum)
                .fotos(new ArrayList<>())
                .disponibilidades(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Admin comum deve listar apenas as suas quadras")
    void deveListarApenasQuadrasDoAdminComum() {
        when(usuarioService.buscarPorIdEntidade(1L)).thenReturn(Optional.of(adminComum));
        when(quadraRepository.findAll(any(Specification.class))).thenReturn(List.of(quadraAdminComum));

        List<QuadraResponseDTO> resultado = quadraBuscaService.listar(1L, null, null, null);

        assertEquals(1, resultado.size());
        assertEquals("Quadra do Admin", resultado.get(0).nome());
        verify(quadraRepository, times(1)).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Master Admin deve listar todas as quadras")
    void deveListarTodasAsQuadrasParaMasterAdmin() {
        when(usuarioService.buscarPorIdEntidade(99L)).thenReturn(Optional.of(masterAdmin));
        when(quadraRepository.findAll(any(Specification.class))).thenReturn(List.of(quadraAdminComum));

        List<QuadraResponseDTO> resultado = quadraBuscaService.listar(99L, null, null, null);

        assertEquals(1, resultado.size());
        verify(quadraRepository, times(1)).findAll(any(Specification.class));
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
        when(quadraRepository.findAll(any(Specification.class))).thenReturn(List.of(quadraAdminComum));

        List<Quadra> resultado = quadraBuscaService.filtrarQuadrasEntidades(null, lat, lng, raioKm, null, null, null, null, null);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(quadraRepository, times(1)).findByAtivaTrueAndProximidadeMenorQue(
                eq(lat), eq(lng), eq(raioKm),
                anyDouble(), anyDouble(), anyDouble(), anyDouble()
        );
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

        when(quadraRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable p = invocation.getArgument(1);
                    int start = (int) p.getOffset();
                    int end = Math.min(start + p.getPageSize(), quadras.size());
                    List<Quadra> sub = start >= quadras.size() ? List.of() : quadras.subList(start, end);
                    return new PageImpl<>(sub, p, quadras.size());
                });

        Pageable pageable = PageRequest.of(0, 6);
        Page<QuadraResponseDTO> primeiraPagina = quadraBuscaService.listar(null, null, null, null, null, null, null, null, null, null, pageable);

        assertEquals(6, primeiraPagina.getContent().size());
        assertEquals(14, primeiraPagina.getTotalElements());
        assertEquals(3, primeiraPagina.getTotalPages());
        assertEquals(0, primeiraPagina.getNumber());
        assertEquals("Quadra 1", primeiraPagina.getContent().get(0).nome());
    }

    @Test
    @DisplayName("Deve buscar quadras ativas por especificação quando esporte for 'futebol de salão'")
    void deveBuscarQuadrasAtivasComFutsal() {
        Quadra futsal = Quadra.builder().id_quadra(7L).nome("Salão").tipoEsporte(TipoEsporte.FUTSAL).ativa(true).build();
        when(quadraRepository.findAll(any(Specification.class))).thenReturn(List.of(futsal));

        List<Quadra> resultado = quadraBuscaService.buscarQuadrasAtivas(null, "futebol de salão", null);

        assertEquals(List.of(futsal), resultado);
        verify(quadraRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve retornar vazio sem consultar o banco quando esporte não for reconhecido")
    void deveRetornarVazioParaEsporteDesconhecido() {
        assertTrue(quadraBuscaService.buscarQuadrasAtivas(null, "xadrez", null).isEmpty());
        verifyNoInteractions(quadraRepository);
    }

    @Test
    @DisplayName("Deve buscar por quadraId retornando apenas quadra ativa")
    void deveBuscarQuadraAtivaPorId() {
        Quadra inativa = Quadra.builder().id_quadra(8L).ativa(false).build();
        when(quadraRepository.findById(8L)).thenReturn(Optional.of(inativa));

        assertTrue(quadraBuscaService.buscarQuadrasAtivas(8L, null, null).isEmpty());
    }

    @Test
    @DisplayName("Deve realizar parse de tipos de esportes de forma resiliente")
    void deveFazerParseTipoEsporteResiliente() {
        assertEquals(TipoEsporte.FUTEBOL, quadraBuscaService.parseTipoEsporte("society"));
        assertEquals(TipoEsporte.FUTSAL, quadraBuscaService.parseTipoEsporte("futebol de salão"));
        assertEquals(TipoEsporte.BEACH_TENNIS, quadraBuscaService.parseTipoEsporte("beach tennis"));
        assertEquals(TipoEsporte.BEACH_TENNIS, quadraBuscaService.parseTipoEsporte("bit"));
        assertEquals(TipoEsporte.BASQUETE, quadraBuscaService.parseTipoEsporte("basquete"));
        assertEquals(TipoEsporte.TENIS, quadraBuscaService.parseTipoEsporte("tênis"));
        assertEquals(TipoEsporte.VOLEI, quadraBuscaService.parseTipoEsporte("vôlei"));
        assertNull(quadraBuscaService.parseTipoEsporte("esporte_desconhecido_xyz"));
        assertNull(quadraBuscaService.parseTipoEsporte(""));
        assertNull(quadraBuscaService.parseTipoEsporte(null));
    }

    @Test
    @DisplayName("Deve listar quadras no formato resumido para integrações/bots")
    void deveListarResumido() {
        when(quadraRepository.findAll(any(Specification.class))).thenReturn(List.of(quadraAdminComum));

        List<QuadraResumoResponseDTO> resultado = quadraBuscaService.listarResumido(null, null, null, null, null, null, null, null, null);

        assertEquals(1, resultado.size());
        assertEquals("Quadra do Admin", resultado.get(0).nome());
    }

    @Test
    @DisplayName("Deve aplicar paginação sobre os resultados dos filtros de CEP/endereço")
    void deveAplicarPaginacaoAposFiltros() {
        Quadra q1 = Quadra.builder().id_quadra(1L).nome("Quadra Centro 1").logradouro("Rua Central").bairro("Centro").ativa(true).fotos(new ArrayList<>()).disponibilidades(new ArrayList<>()).build();
        Quadra q3 = Quadra.builder().id_quadra(3L).nome("Quadra Centro 2").logradouro("Av Central").bairro("Centro").ativa(true).fotos(new ArrayList<>()).disponibilidades(new ArrayList<>()).build();

        when(quadraRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(q1, q3), PageRequest.of(0, 6), 2));

        Pageable pageable = PageRequest.of(0, 6);
        Page<QuadraResponseDTO> pagina = quadraBuscaService.listar(null, null, null, null, null, null, "Centro", null, null, null, pageable);

        assertEquals(2, pagina.getTotalElements());
        assertEquals(1, pagina.getTotalPages());
        assertEquals(2, pagina.getContent().size());
    }
}
