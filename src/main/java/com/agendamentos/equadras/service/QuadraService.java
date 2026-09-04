package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.QuadraCriacaoDTO;
import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuadraService {

    private final QuadraRepository quadraRepository;
    private final UsuarioRepository usuarioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final FileStorageService fileStorageService;

    public QuadraService(QuadraRepository quadraRepository, 
                         UsuarioRepository usuarioRepository,
                         AgendamentoRepository agendamentoRepository,
                         FileStorageService fileStorageService) {
        this.quadraRepository = quadraRepository;
        this.usuarioRepository = usuarioRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public QuadraResponseDTO cadastrar(QuadraCriacaoDTO dto, Long adminId) {
        if (adminId == null) {
            throw new IllegalArgumentException("ID do administrador é obrigatório.");
        }
        
        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Administrador não encontrado."));
                
        if (admin.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Apenas administradores podem cadastrar quadras.");
        }

        java.util.List<String> fotosIniciais = dto.fotos() != null ? new java.util.ArrayList<>(dto.fotos()) : new java.util.ArrayList<>();
        if (fotosIniciais.size() > 5) {
            throw new IllegalArgumentException("Uma quadra pode ter no máximo 5 fotos.");
        }

        List<com.agendamentos.equadras.model.entity.DisponibilidadeDia> disponibilidades;
        if (dto.disponibilidades() == null || dto.disponibilidades().isEmpty()) {
            disponibilidades = new java.util.ArrayList<>();
            for (java.time.DayOfWeek dia : java.time.DayOfWeek.values()) {
                disponibilidades.add(new com.agendamentos.equadras.model.entity.DisponibilidadeDia(dia, java.time.LocalTime.of(6, 0), java.time.LocalTime.of(23, 0)));
            }
        } else {
            disponibilidades = new java.util.ArrayList<>(
                    dto.disponibilidades().stream()
                            .map(d -> new com.agendamentos.equadras.model.entity.DisponibilidadeDia(d.diaSemana(), d.horaInicio(), d.horaFim()))
                            .toList()
            );
        }

        Quadra quadra = Quadra.builder()
                .nome(dto.nome())
                .tipoEsporte(dto.tipoEsporte())
                .valorHora(dto.valorHora())
                .cep(dto.cep())
                .logradouro(dto.logradouro())
                .bairro(dto.bairro())
                .cidade(dto.cidade())
                .estado(dto.estado())
                .latitude(dto.latitude())
                .longitude(dto.longitude())
                .descricao(dto.descricao())
                .dataLimiteAgendamento(dto.dataLimiteAgendamento())
                .fotos(fotosIniciais)
                .disponibilidades(disponibilidades)
                .ativa(true)
                .admin(admin)
                .build();

        Quadra quadraSalva = quadraRepository.save(quadra);
        return QuadraResponseDTO.fromEntity(quadraSalva);
    }

    private boolean podeGerenciarQuadra(Quadra quadra, Long adminId) {
        if (adminId == null) return false;
        Usuario admin = usuarioRepository.findById(adminId).orElse(null);
        if (admin == null) return false;
        if (admin.isMasterAdmin()) return true;
        return quadra.getAdmin() != null && quadra.getAdmin().getId_usuario().equals(adminId);
    }

    @Transactional
    public QuadraResponseDTO editar(Long id, QuadraCriacaoDTO dto, Long adminId) {
        Quadra quadra = quadraRepository.findByIdWithAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + id));

        // Se a quadra não tiver admin vinculado (legado), vincula ao admin atual
        if (quadra.getAdmin() == null) {
            Usuario admin = usuarioRepository.findById(adminId)
                    .orElseThrow(() -> new IllegalArgumentException("Administrador não encontrado."));
            quadra.setAdmin(admin);
        } else if (!podeGerenciarQuadra(quadra, adminId)) {
            throw new IllegalArgumentException("Apenas o administrador dono da quadra ou o Master Admin pode editá-la.");
        }

        quadra.setNome(dto.nome());
        quadra.setTipoEsporte(dto.tipoEsporte());
        quadra.setValorHora(dto.valorHora());
        quadra.setCep(dto.cep());
        quadra.setLogradouro(dto.logradouro());
        quadra.setBairro(dto.bairro());
        quadra.setCidade(dto.cidade());
        quadra.setEstado(dto.estado());
        quadra.setLatitude(dto.latitude());
        quadra.setLongitude(dto.longitude());
        quadra.setDescricao(dto.descricao());
        quadra.setDataLimiteAgendamento(dto.dataLimiteAgendamento());

        if (dto.disponibilidades() != null) {
            quadra.getDisponibilidades().clear();
            for (com.agendamentos.equadras.dto.request.DisponibilidadeDiaDTO d : dto.disponibilidades()) {
                quadra.getDisponibilidades().add(new com.agendamentos.equadras.model.entity.DisponibilidadeDia(d.diaSemana(), d.horaInicio(), d.horaFim()));
            }
        }

        if (dto.fotos() != null) {
            if (dto.fotos().size() > 5) {
                throw new IllegalArgumentException("Uma quadra pode ter no máximo 5 fotos.");
            }
            List<String> novasFotos = dto.fotos();
            quadra.getFotos().removeIf(foto -> !novasFotos.contains(foto));
            for (String foto : novasFotos) {
                if (!quadra.getFotos().contains(foto)) {
                    quadra.getFotos().add(foto);
                }
            }
        }

        Quadra quadraSalva = quadraRepository.save(quadra);
        return QuadraResponseDTO.fromEntity(quadraSalva);
    }

    @Transactional
    public QuadraResponseDTO uploadFotos(Long id, java.util.List<org.springframework.web.multipart.MultipartFile> arquivos, Long adminId) {
        Quadra quadra = quadraRepository.findByIdWithAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + id));

        if (!podeGerenciarQuadra(quadra, adminId)) {
            throw new IllegalArgumentException("Apenas o administrador dono da quadra ou o Master Admin pode fazer upload de fotos.");
        }

        if (arquivos == null || arquivos.isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo enviado.");
        }

        if (quadra.getFotos().size() + arquivos.size() > 5) {
            throw new IllegalArgumentException("Limite de 5 fotos por quadra atingido. Remova fotos existentes antes de enviar novas.");
        }

        for (org.springframework.web.multipart.MultipartFile file : arquivos) {
            String url = fileStorageService.salvarArquivo(file);
            quadra.getFotos().add(url);
        }

        Quadra quadraSalva = quadraRepository.save(quadra);
        return QuadraResponseDTO.fromEntity(quadraSalva);
    }

    @Transactional
    public QuadraResponseDTO removerFoto(Long id, String fotoUrl, Long adminId) {
        Quadra quadra = quadraRepository.findByIdWithAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + id));

        if (!podeGerenciarQuadra(quadra, adminId)) {
            throw new IllegalArgumentException("Apenas o administrador dono da quadra ou o Master Admin pode remover fotos.");
        }

        if (quadra.getFotos().remove(fotoUrl)) {
            fileStorageService.excluirArquivo(fotoUrl);
        }

        Quadra quadraSalva = quadraRepository.save(quadra);
        return QuadraResponseDTO.fromEntity(quadraSalva);
    }

    @Transactional
    public void excluir(Long id, Long adminId) {
        Quadra quadra = quadraRepository.findByIdWithAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + id));

        if (!podeGerenciarQuadra(quadra, adminId)) {
            throw new IllegalArgumentException("Apenas o administrador dono da quadra ou o Master Admin pode excluí-la.");
        }

        if (agendamentoRepository.existsByQuadraId(id)) {
            throw new IllegalStateException("Esta quadra não pode ser excluída porque possui agendamentos vinculados (histórico de reservas). Recomendamos inativar a quadra.");
        }

        // Limpa fotos físicas
        for (String fotoUrl : quadra.getFotos()) {
            fileStorageService.excluirArquivo(fotoUrl);
        }

        quadraRepository.delete(quadra);
    }

    @Transactional(readOnly = true)
    public List<QuadraResponseDTO> listar(Long usuarioId, Double latitude, Double longitude, Double raioKm) {
        return listar(usuarioId, latitude, longitude, raioKm, (String) null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<QuadraResponseDTO> listar(Long usuarioId, Double latitude, Double longitude, Double raioKm,
                                          com.agendamentos.equadras.model.enums.TipoEsporte tipoEsporte,
                                          String nome, String cidade, String bairro, String cep) {
        return listar(usuarioId, latitude, longitude, raioKm, tipoEsporte != null ? tipoEsporte.name() : null, nome, cidade, bairro, cep);
    }

    @Transactional(readOnly = true)
    public List<QuadraResponseDTO> listar(Long usuarioId, Double latitude, Double longitude, Double raioKm,
                                          String tipoEsporte,
                                          String nome, String cidade, String bairro, String cep) {
        return filtrarQuadrasEntidades(usuarioId, latitude, longitude, raioKm, tipoEsporte, nome, cidade, bairro, cep)
                .stream()
                .map(QuadraResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.agendamentos.equadras.dto.response.QuadraResumoResponseDTO> listarResumido(Long usuarioId, Double latitude, Double longitude, Double raioKm,
                                                        String tipoEsporte,
                                                        String nome, String cidade, String bairro, String cep) {
        return filtrarQuadrasEntidades(usuarioId, latitude, longitude, raioKm, tipoEsporte, nome, cidade, bairro, cep)
                .stream()
                .map(com.agendamentos.equadras.dto.response.QuadraResumoResponseDTO::fromEntity)
                .toList();
    }

    public List<Quadra> filtrarQuadrasEntidades(Long usuarioId, Double latitude, Double longitude, Double raioKm,
                                                String tipoEsporte,
                                                String nome, String cidade, String bairro, String cep) {
        List<Quadra> quadras;
        double raio = (raioKm != null && raioKm > 0) ? raioKm : 2.0;
        
        if (usuarioId != null) {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            if (usuario != null && usuario.getRole() == Role.ADMIN) {
                if (usuario.isMasterAdmin()) {
                    quadras = quadraRepository.findAllWithAdminEFotos();
                } else {
                    quadras = quadraRepository.findByAdminId(usuarioId);
                }
            } else {
                if (latitude != null && longitude != null) {
                    quadras = quadraRepository.findByAtivaTrueAndProximidadeMenorQue(latitude, longitude, raio);
                } else {
                    quadras = quadraRepository.findByAtivaTrue();
                }
            }
        } else {
            if (latitude != null && longitude != null) {
                quadras = quadraRepository.findByAtivaTrueAndProximidadeMenorQue(latitude, longitude, raio);
            } else {
                quadras = quadraRepository.findByAtivaTrue();
            }
        }

        quadras.forEach(q -> {
            if (q.getFotos() != null) {
                q.getFotos().size();
            }
            if (q.getDisponibilidades() != null) {
                q.getDisponibilidades().size();
            }
        });

        java.util.stream.Stream<Quadra> stream = quadras.stream();

        if (tipoEsporte != null && !tipoEsporte.isBlank()) {
            com.agendamentos.equadras.model.enums.TipoEsporte esporteEnum = parseTipoEsporte(tipoEsporte);
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

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT)
                .trim();
    }

    private com.agendamentos.equadras.model.enums.TipoEsporte parseTipoEsporte(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = normalizarTexto(valor).toUpperCase(java.util.Locale.ROOT).replace("-", "_").replace(" ", "_");

        for (com.agendamentos.equadras.model.enums.TipoEsporte t : com.agendamentos.equadras.model.enums.TipoEsporte.values()) {
            if (t.name().equals(normalizado)) {
                return t;
            }
        }

        if (normalizado.contains("SOCIETY") || normalizado.contains("CAMPO") || normalizado.contains("FUT")) {
            return com.agendamentos.equadras.model.enums.TipoEsporte.FUTEBOL;
        }
        if (normalizado.contains("SALAO")) {
            return com.agendamentos.equadras.model.enums.TipoEsporte.FUTSAL;
        }
        if (normalizado.contains("BEACH") || normalizado.contains("AREIA") || normalizado.contains("FUTEVOLEI") || normalizado.contains("BIT")) {
            return com.agendamentos.equadras.model.enums.TipoEsporte.BEACH_TENNIS;
        }
        if (normalizado.contains("BASQUET")) {
            return com.agendamentos.equadras.model.enums.TipoEsporte.BASQUETE;
        }
        if (normalizado.contains("TENIS")) {
            return com.agendamentos.equadras.model.enums.TipoEsporte.TENIS;
        }
        if (normalizado.contains("VOLEI")) {
            return com.agendamentos.equadras.model.enums.TipoEsporte.VOLEI;
        }

        return null;
    }

    @Transactional(readOnly = true)
    public QuadraResponseDTO buscarPorId(Long id) {
        Quadra quadra = quadraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + id));
        return QuadraResponseDTO.fromEntity(quadra);
    }

    @Transactional
    public QuadraResponseDTO alternarStatus(Long id, boolean status, Long adminId) {
        Quadra quadra = quadraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + id));

        if (!podeGerenciarQuadra(quadra, adminId)) {
            throw new IllegalArgumentException("Apenas o administrador dono da quadra ou o Master Admin pode alterar seu status.");
        }

        quadra.setAtiva(status);
        Quadra quadraAtualizada = quadraRepository.save(quadra);
        return QuadraResponseDTO.fromEntity(quadraAtualizada);
    }
}