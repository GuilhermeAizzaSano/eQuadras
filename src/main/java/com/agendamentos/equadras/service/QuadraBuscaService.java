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
import java.util.Optional;
import java.util.Set;

@Service
public class QuadraBuscaService {

    private static final String PROPRIEDADE_ID_QUADRA_ORDENACAO = "id_quadra";
    private static final double RAIO_MAXIMO_KM = 2.0;

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
        Optional<Specification<Quadra>> spec = montarSpecification(usuarioId, latitude, longitude, raioKm,
                tipoEsporte, nome, endereco, cidade, bairro, cep);
        if (spec.isEmpty()) {
            return new PageImpl<>(List.of(), pageable != null ? pageable : Pageable.unpaged(), 0);
        }

        if (latitude != null && longitude != null) {
            if (pageable == null || pageable.isUnpaged()) {
                return new PageImpl<>(listar(usuarioId, latitude, longitude, raioKm, tipoEsporte, nome, endereco, cidade, bairro, cep));
            }
            // Sem Sort: a ordem por distância definida em dentroDoRaio prevalece
            return quadraRepository.findAll(spec.get(), PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()))
                    .map(QuadraResponseDTO::fromEntity);
        }

        Pageable pageableEfetivo = (pageable != null && pageable.isPaged()) ? pageable : PageRequest.of(0, 10);
        Pageable pageableOrdenado = SORT_POLICY_QUADRAS.apply(pageableEfetivo);
        Pageable pageableOrdenadoSeguro = PageRequest.of(
                pageableOrdenado.getPageNumber(),
                pageableOrdenado.getPageSize(),
                comOrdenacaoIdQuadraSegura(pageableOrdenado.getSort()));
        Page<Quadra> paginaQuadras = quadraRepository.findAll(spec.get(), pageableOrdenadoSeguro);

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

    /**
     * Coleções LAZY (disponibilidades) não são pré-carregadas: cada chamador lê só o que usa,
     * dentro da própria transação (open-in-view está desligado).
     */
    @Transactional(readOnly = true)
    public List<Quadra> filtrarQuadrasEntidades(Long usuarioId, Double latitude, Double longitude, Double raioKm,
                                                String tipoEsporte,
                                                String nome, String endereco, String cidade, String bairro, String cep) {
        Optional<Specification<Quadra>> spec = montarSpecification(usuarioId, latitude, longitude, raioKm,
                tipoEsporte, nome, endereco, cidade, bairro, cep);
        if (spec.isEmpty()) {
            return List.of();
        }

        return quadraRepository.findAll(spec.get());
    }

    // Regra de negócio: a busca por proximidade cobre no máximo 2 km; raio ausente/inválido usa o máximo.
    private static double raioEfetivo(Double raioKm) {
        if (raioKm == null || raioKm <= 0) {
            return RAIO_MAXIMO_KM;
        }
        return Math.min(raioKm, RAIO_MAXIMO_KM);
    }

    // Escopo por perfil (regra de negócio) + filtros (executados no SQL). Vazio quando o esporte não é reconhecido.
    private Optional<Specification<Quadra>> montarSpecification(Long usuarioId, Double latitude, Double longitude, Double raioKm,
                                                                String tipoEsporte, String nome,
                                                                String endereco, String cidade, String bairro, String cep) {
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
            if (esporteEnum == null) {
                return Optional.empty();
            }
            spec = spec.and(QuadraSpecifications.comTipoEsporte(esporteEnum));
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

        // A busca por proximidade sempre considera só quadras ativas (comportamento da consulta nativa anterior)
        if (latitude != null && longitude != null) {
            spec = spec.and(QuadraSpecifications.ativa())
                    .and(QuadraSpecifications.dentroDoRaio(latitude, longitude, raioEfetivo(raioKm)));
        }

        return Optional.of(spec);
    }

    @Transactional(readOnly = true)
    public List<Quadra> buscarQuadrasAtivas(Long quadraId, String tipoEsporte, String nomeQuadra) {
        if (quadraId != null) {
            return quadraRepository.findById(quadraId)
                    .filter(Quadra::isAtiva)
                    .map(List::of)
                    .orElse(List.of());
        }

        Specification<Quadra> spec = QuadraSpecifications.ativa();

        if (tipoEsporte != null && !tipoEsporte.isBlank()) {
            TipoEsporte esporteEnum = parseTipoEsporte(tipoEsporte);
            if (esporteEnum != null) {
                spec = spec.and(QuadraSpecifications.comTipoEsporte(esporteEnum));
            } else {
                return List.of();
            }
        }

        if (nomeQuadra != null && !nomeQuadra.isBlank()) {
            spec = spec.and(QuadraSpecifications.comNome(nomeQuadra));
        }

        return quadraRepository.findAll(spec);
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
