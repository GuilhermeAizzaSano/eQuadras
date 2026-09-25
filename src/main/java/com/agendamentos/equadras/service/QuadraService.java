package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.QuadraCriacaoDTO;
import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.event.QuadraAlteradaEvent;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.QuadraRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuadraService {

    private final QuadraRepository quadraRepository;
    private final UsuarioService usuarioService;
    private final AgendamentoService agendamentoService;
    private final QuadraFotoService quadraFotoService;
    private final ApplicationEventPublisher eventPublisher;

    public QuadraService(QuadraRepository quadraRepository, 
                         UsuarioService usuarioService,
                         AgendamentoService agendamentoService,
                         QuadraFotoService quadraFotoService,
                         ApplicationEventPublisher eventPublisher) {
        this.quadraRepository = quadraRepository;
        this.usuarioService = usuarioService;
        this.agendamentoService = agendamentoService;
        this.quadraFotoService = quadraFotoService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public QuadraResponseDTO cadastrar(QuadraCriacaoDTO dto, Long adminId) {
        if (adminId == null) {
            throw new IllegalArgumentException("ID do administrador é obrigatório.");
        }
        
        Usuario admin = usuarioService.buscarPorIdEntidade(adminId)
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
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new QuadraAlteradaEvent(
                    quadraSalva.getId_quadra(),
                    quadraSalva.getNome(),
                    quadraSalva.getCidade(),
                    quadraSalva.getEstado(),
                    adminId,
                    "CRIAR",
                    "Quadra cadastrada: " + quadraSalva.getNome() + " (" + quadraSalva.getCidade() + "/" + quadraSalva.getEstado() + ")"
            ));
        }
        return QuadraResponseDTO.fromEntity(quadraSalva);
    }

    private boolean podeGerenciarQuadra(Quadra quadra, Long adminId) {
        if (adminId == null) return false;
        Usuario admin = usuarioService.buscarPorIdEntidade(adminId).orElse(null);
        if (admin == null) return false;
        if (admin.isMasterAdmin()) return true;
        return quadra.getAdmin() != null && quadra.getAdmin().getId_usuario().equals(adminId);
    }

    @Transactional
    public QuadraResponseDTO editar(Long id, QuadraCriacaoDTO dto, Long adminId) {
        Quadra quadra = quadraRepository.findByIdWithAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + id));

        if (quadra.getAdmin() == null || !podeGerenciarQuadra(quadra, adminId)) {
            throw new org.springframework.security.access.AccessDeniedException("Apenas o administrador dono da quadra ou o Master Admin pode editá-la.");
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
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new QuadraAlteradaEvent(
                    quadraSalva.getId_quadra(),
                    quadraSalva.getNome(),
                    quadraSalva.getCidade(),
                    quadraSalva.getEstado(),
                    adminId,
                    "EDITAR",
                    "Quadra editada: " + quadraSalva.getNome()
            ));
        }
        return QuadraResponseDTO.fromEntity(quadraSalva);
    }

    @Transactional
    public void excluir(Long id, Long adminId) {
        Quadra quadra = quadraRepository.findByIdWithAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + id));

        if (!podeGerenciarQuadra(quadra, adminId)) {
            throw new org.springframework.security.access.AccessDeniedException("Apenas o administrador dono da quadra ou o Master Admin pode excluí-la.");
        }

        if (agendamentoService.possuiAgendamentos(id)) {
            throw new IllegalStateException("Esta quadra não pode ser excluída porque possui agendamentos vinculados (histórico de reservas). Recomendamos inativar a quadra.");
        }

        // Limpa fotos físicas
        quadraFotoService.excluirFotosDaQuadra(quadra);

        quadraRepository.delete(quadra);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new QuadraAlteradaEvent(
                    id,
                    quadra.getNome(),
                    quadra.getCidade(),
                    quadra.getEstado(),
                    adminId,
                    "EXCLUIR",
                    "Quadra excluída: " + quadra.getNome()
            ));
        }
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Quadra> buscarPorIdEntidade(Long id) {
        if (id == null) return java.util.Optional.empty();
        return quadraRepository.findById(id);
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
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new QuadraAlteradaEvent(
                    quadraAtualizada.getId_quadra(),
                    quadraAtualizada.getNome(),
                    quadraAtualizada.getCidade(),
                    quadraAtualizada.getEstado(),
                    adminId,
                    "STATUS",
                    "Status da quadra alterado para " + (status ? "ATIVA" : "INATIVA") + ": " + quadraAtualizada.getNome()
            ));
        }
        return QuadraResponseDTO.fromEntity(quadraAtualizada);
    }
}