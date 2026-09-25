package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.dto.response.QuadraResumoResponseDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.specification.QuadraSpecifications;
import com.agendamentos.equadras.util.TextoUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@Service
public class QuadraBuscaService {

    private static final String PROPRIEDADE_ID_QUADRA_ORDENACAO = "id_quadra";

    private static final com.agendamentos.equadras.shared.pagination.SortPolicy SORT_POLICY_QUADRAS =
            com.agendamentos.equadras.shared.pagination.SortPolicy.of(
                    Set.of("nome", PROPRIEDADE_ID_QUADRA_ORDENACAO),
                    Sort.by(Sort.Direction.ASC, "nome"),
                    PROPRIEDADE_ID_QUADRA_ORDENACAO);

    private static Sort comOrdenacaoIdQuadraSegura(Sort sort) {
        List<Sort.Order> ordens = new ArrayList<>();
        for (Sort.Order order : sort) {
            if (PROPRIEDADE_ID_QUADRA_ORDENACAO.equals(order.getProperty())) {
                ordens.addAll(JpaSort
                        .unsafe(order.getDirection(), PROPRIEDADE_ID_QUADRA_ORDENACAO)
                        .toList());
            } else {
                ordens.add(order);
            }
        }
        return Sort.by(ordens);
    }

    private final QuadraRepository quadraRepository;
    private final UsuarioService usuarioService;

    public QuadraBuscaService(QuadraRepository quadraRepository, UsuarioService usuarioService) {
        this.quadraRepository = quadraRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public List<QuadraResponseDTO> listar(Long usuarioId, Double latitude, Double longitude, Double raioKm) {
        return listar(usuarioId, latitude, longitude, raioKm, (String) null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<QuadraResponseDTO> listar(Long usuarioId, Double latitude, Double longitude, Double raioKm,
                                          TipoEsporte tipoEsporte,
                                          String nome, String cidade, String bairro, String cep) {
        return listar(usuarioId, latitude, longitude, raioKm, tipoEsporte != null ? tipoEsporte.name() : null, nome, cidade, bairro, cep);
    }

    @Transactional(readOnly = true)
    public List<QuadraResponseDTO> listar(Long usuarioId, Double latitude, Double longitude, Double raioKm,
                                          String tipoEsporte,
                                          String nome, String cidade, String bairro, String cep) {
        return listar(usuarioId, latitude, longitude, raioKm, tipoEsporte, nome, null, cidade, bairro, cep);
    }

    @Transactional(readOnly = true)
    public List<QuadraResponseDTO> listar(Long usuarioId, Double latitude, Double longitude, Double raioKm,
                                          String tipoEsporte,
                                          String nome, String endereco, String cidade, String bairro, String cep) {
        return filtrarQuadrasEntidades(usuarioId, latitude, longitude, raioKm, tipoEsporte, nome, endereco, cidade, bairro, cep)
                .stream()
                .map(QuadraResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<QuadraResponseDTO> listar(Long usuarioId, Double latitude, Double longitude, Double raioKm,
                                          String tipoEsporte,
                                          String nome, String endereco, String cidade, String bairro, String cep,
                                          Pageable pageable) {
        if (latitude != null && longitude != null) {
            List<QuadraResponseDTO> todas = listar(usuarioId, latitude, longitude, raioKm, tipoEsporte, nome, endereco, cidade, bairro, cep);
            if (pageable == null || pageable.isUnpaged()) {
                return new PageImpl<>(todas);
            }
            int total = todas.size();
            int start = (int) pageable.getOffset();
            if (start >= total) {
                return new PageImpl<>(List.of(), pageable, total);
            }
            int end = Math.min(start + pageable.getPageSize(), total);
            return new PageImpl<>(todas.subList(start, end), pageable, total);
        }

        Specification<Quadra> spec = (root, query, cb) -> cb.conjunction();

        if (usuarioId != null) {
            Usuario usuario = usuarioService.buscarPorIdEntidade(usuarioId).orElse(null);
            if (usuario != null && usuario.getRole() == Role.ADMIN) {
                if (!usuario.isMasterAdmin()) {
                    spec = spec.and(QuadraSpecifications.doAdmin(usuarioId));
                }
            } else {
                spec = spec.and(QuadraSpecifications.ativa());
            }
        } else {
            spec = spec.and(QuadraSpecifications.ativa());
        }

        if (tipoEsporte != null && !tipoEsporte.isBlank()) {
            TipoEsporte esporteEnum = parseTipoEsporte(tipoEsporte);
            if (esporteEnum != null) {
                spec = spec.and(QuadraSpecifications.comTipoEsporte(esporteEnum));
            } else {
                return new PageImpl<>(List.of(), pageable != null ? pageable : Pageable.unpaged(), 0);
            }
        }

        if (nome != null && !nome.isBlank()) {
            spec = spec.and(QuadraSpecifications.comNome(nome));
        }

        if (endereco != null && !endereco.isBlank()) {
            spec = spec.and(QuadraSpecifications.comEndereco(endereco));
        }

        if (cidade != null && !cidade.isBlank()) {
            spec = spec.and(QuadraSpecifications.comCidade(cidade));
        }

        if (bairro != null && !bairro.isBlank()) {
            spec = spec.and(QuadraSpecifications.comBairro(bairro));
        }

        if (cep != null && !cep.isBlank()) {
            spec = spec.and(QuadraSpecifications.comCep(cep));
        }

        Pageable pageableEfetivo = (pageable != null && pageable.isPaged()) ? pageable : PageRequest.of(0, 10);
        Pageable pageableOrdenado = SORT_POLICY_QUADRAS.apply(pageableEfetivo);
        Pageable pageableOrdenadoSeguro = PageRequest.of(
                pageableOrdenado.getPageNumber(),
                pageableOrdenado.getPageSize(),
                comOrdenacaoIdQuadraSegura(pageableOrdenado.getSort()));
        Page<Quadra> paginaQuadras = quadraRepository.findAll(spec, pageableOrdenadoSeguro);

        return paginaQuadras.map(QuadraResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<QuadraResumoResponseDTO> listarResumido(Long usuarioId, Double latitude, Double longitude, Double raioKm,
                                                        String tipoEsporte,
                                                        String nome, String cidade, String bairro, String cep) {
        return listarResumido(usuarioId, latitude, longitude, raioKm, tipoEsporte, nome, null, cidade, bairro, cep);
    }

    @Transactional(readOnly = true)
    public List<QuadraResumoResponseDTO> listarResumido(Long usuarioId, Double latitude, Double longitude, Double raioKm,
                                                        String tipoEsporte,
                                                        String nome, String endereco, String cidade, String bairro, String cep) {
        return filtrarQuadrasEntidades(usuarioId, latitude, longitude, raioKm, tipoEsporte, nome, endereco, cidade, bairro, cep)
                .stream()
                .map(QuadraResumoResponseDTO::fromEntity)
                .toList();
    }

    public List<Quadra> filtrarQuadrasEntidades(Long usuarioId, Double latitude, Double longitude, Double raioKm,
                                                String tipoEsporte,
                                                String nome, String cidade, String bairro, String cep) {
        return filtrarQuadrasEntidades(usuarioId, latitude, longitude, raioKm, tipoEsporte, nome, null, cidade, bairro, cep);
    }

    public List<Quadra> filtrarQuadrasEntidades(Long usuarioId, Double latitude, Double longitude, Double raioKm,
                                                String tipoEsporte,
                                                String nome, String endereco, String cidade, String bairro, String cep) {
        List<Quadra> quadras;
        double raio = (raioKm != null && raioKm > 0) ? raioKm : 2.0;

        BiFunction<Double, Double, List<Quadra>> buscarPorProximidade = (lat, lng) -> {
            double deltaLat = raio / 111.0;
            double cosLat = Math.cos(Math.toRadians(lat));
            double deltaLng = (Math.abs(cosLat) > 0.0001) ? raio / (111.0 * Math.abs(cosLat)) : deltaLat;

            double minLat = lat - deltaLat;
            double maxLat = lat + deltaLat;
            double minLng = lng - deltaLng;
            double maxLng = lng + deltaLng;

            return quadraRepository.findByAtivaTrueAndProximidadeMenorQue(lat, lng, raio, minLat, maxLat, minLng, maxLng);
        };

        if (latitude != null && longitude != null) {
            quadras = buscarPorProximidade.apply(latitude, longitude);
            if (usuarioId != null) {
                Usuario usuario = usuarioService.buscarPorIdEntidade(usuarioId).orElse(null);
                if (usuario != null && usuario.getRole() == Role.ADMIN && !usuario.isMasterAdmin()) {
                    quadras = quadras.stream()
                            .filter(q -> q.getAdmin() != null && usuarioId.equals(q.getAdmin().getId_usuario()))
                            .toList();
                }
            }
        } else if (usuarioId != null) {
            Usuario usuario = usuarioService.buscarPorIdEntidade(usuarioId).orElse(null);
            if (usuario != null && usuario.getRole() == Role.ADMIN) {
                if (usuario.isMasterAdmin()) {
                    quadras = quadraRepository.findAllWithAdminEFotos();
                } else {
                    quadras = quadraRepository.findByAdminId(usuarioId);
                }
            } else {
                quadras = quadraRepository.findByAtivaTrue();
            }
        } else {
            quadras = quadraRepository.findByAtivaTrue();
        }

        quadras.forEach(q -> {
            if (q.getFotos() != null) {
                q.getFotos().size();
            }
            if (q.getDisponibilidades() != null) {
                q.getDisponibilidades().size();
            }
        });

        Stream<Quadra> stream = quadras.stream();

        if (tipoEsporte != null && !tipoEsporte.isBlank()) {
            TipoEsporte esporteEnum = parseTipoEsporte(tipoEsporte);
            if (esporteEnum != null) {
                stream = stream.filter(q -> q.getTipoEsporte() == esporteEnum);
            } else {
                stream = stream.filter(q -> false);
            }
        }

        if (nome != null && !nome.isBlank()) {
            String nomeNorm = normalizarTexto(nome);
            stream = stream.filter(q -> q.getNome() != null && normalizarTexto(q.getNome()).contains(nomeNorm));
        }

        if (endereco != null && !endereco.isBlank()) {
            String enderecoNorm = normalizarTexto(endereco);
            stream = stream.filter(q ->
                    (q.getLogradouro() != null && normalizarTexto(q.getLogradouro()).contains(enderecoNorm)) ||
                            (q.getBairro() != null && normalizarTexto(q.getBairro()).contains(enderecoNorm))
            );
        }

        if (cidade != null && !cidade.isBlank()) {
            String cidadeNorm = normalizarTexto(cidade);
            stream = stream.filter(q -> q.getCidade() != null && normalizarTexto(q.getCidade()).contains(cidadeNorm));
        }

        if (bairro != null && !bairro.isBlank()) {
            String bairroNorm = normalizarTexto(bairro);
            stream = stream.filter(q -> q.getBairro() != null && normalizarTexto(q.getBairro()).contains(bairroNorm));
        }

        if (cep != null && !cep.isBlank()) {
            String cepLimpo = cep.replaceAll("[^0-9]", "");
            stream = stream.filter(q -> q.getCep() != null && q.getCep().replaceAll("[^0-9]", "").equals(cepLimpo));
        }

        return stream.toList();
    }

    public TipoEsporte parseTipoEsporte(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = normalizarTexto(valor).toUpperCase(Locale.ROOT).replace("-", "_").replace(" ", "_");

        for (TipoEsporte t : TipoEsporte.values()) {
            if (t.name().equals(normalizado)) {
                return t;
            }
        }

        if (normalizado.contains("SALAO")) {
            return TipoEsporte.FUTSAL;
        }
        if (normalizado.contains("SOCIETY") || normalizado.contains("CAMPO") || normalizado.contains("FUT")) {
            return TipoEsporte.FUTEBOL;
        }
        if (normalizado.contains("BEACH") || normalizado.contains("AREIA") || normalizado.contains("FUTEVOLEI") || normalizado.contains("BIT")) {
            return TipoEsporte.BEACH_TENNIS;
        }
        if (normalizado.contains("BASQUET")) {
            return TipoEsporte.BASQUETE;
        }
        if (normalizado.contains("TENIS")) {
            return TipoEsporte.TENIS;
        }
        if (normalizado.contains("VOLEI")) {
            return TipoEsporte.VOLEI;
        }

        return null;
    }

    private String normalizarTexto(String texto) {
        return TextoUtil.normalizar(texto);
    }
}
