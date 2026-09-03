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
        List<Quadra> quadras;
        double raio = (raioKm != null && raioKm > 0) ? raioKm : 2.0;
        
        if (usuarioId != null) {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            if (usuario != null && usuario.getRole() == Role.ADMIN) {
                if (usuario.isMasterAdmin()) {
                    // Master Admin vê todas as quadras cadastradas no sistema
                    quadras = quadraRepository.findAllWithAdminEFotos();
                } else {
                    // Admin comum vê apenas as suas
                    quadras = quadraRepository.findByAdminId(usuarioId);
                }
            } else {
                // Cliente vê todas as ativas (filtradas por raio se lat/lon informados)
                if (latitude != null && longitude != null) {
                    quadras = quadraRepository.findByAtivaTrueAndProximidadeMenorQue(latitude, longitude, raio);
                } else {
                    quadras = quadraRepository.findByAtivaTrue();
                }
            }
        } else {
            // Visitante não logado vê ativas
            if (latitude != null && longitude != null) {
                quadras = quadraRepository.findByAtivaTrueAndProximidadeMenorQue(latitude, longitude, raio);
            } else {
                quadras = quadraRepository.findByAtivaTrue();
            }
        }

        // Garante inicialização das fotos e disponibilidades dentro da transação para evitar LazyInitializationException
        quadras.forEach(q -> {
            if (q.getFotos() != null) {
                q.getFotos().size();
            }
            if (q.getDisponibilidades() != null) {
                q.getDisponibilidades().size();
            }
        });

        return quadras.stream()
                .map(QuadraResponseDTO::fromEntity)
                .toList();
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