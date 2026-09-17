package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.BloqueioHorarioCriacaoDTO;
import com.agendamentos.equadras.dto.response.BloqueioHorarioResponseDTO;
import com.agendamentos.equadras.model.entity.BloqueioHorario;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.CategoriaAuditoria;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.BloqueioHorarioRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class BloqueioHorarioService {

    private final BloqueioHorarioRepository bloqueioHorarioRepository;
    private final QuadraRepository quadraRepository;
    private final UsuarioRepository usuarioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AuditoriaService auditoriaService;

    public BloqueioHorarioService(BloqueioHorarioRepository bloqueioHorarioRepository,
                                  QuadraRepository quadraRepository,
                                  UsuarioRepository usuarioRepository,
                                  AgendamentoRepository agendamentoRepository,
                                  AuditoriaService auditoriaService) {
        this.bloqueioHorarioRepository = bloqueioHorarioRepository;
        this.quadraRepository = quadraRepository;
        this.usuarioRepository = usuarioRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.auditoriaService = auditoriaService;
    }

    private boolean podeGerenciarBloqueio(Quadra quadra, Long adminId) {
        if (adminId == null) return false;
        Usuario admin = usuarioRepository.findById(adminId).orElse(null);
        if (admin == null) return false;
        if (admin.isMasterAdmin()) return true;
        return quadra.getAdmin() != null && quadra.getAdmin().getId_usuario().equals(adminId);
    }

    @Transactional
    public BloqueioHorarioResponseDTO criarBloqueio(Long quadraId, BloqueioHorarioCriacaoDTO dto, Long adminId) {
        Quadra quadra = quadraRepository.buscarComLockParaAgendamento(quadraId)
                .or(() -> quadraRepository.findByIdWithAdmin(quadraId))
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + quadraId));

        if (!podeGerenciarBloqueio(quadra, adminId)) {
            throw new IllegalArgumentException("Apenas o administrador dono da quadra ou o Master Admin pode criar bloqueios.");
        }

        if (dto.data().isBefore(LocalDate.now(com.agendamentos.equadras.util.DataFlexivelUtil.ZONE_BRASIL))) {
            throw new IllegalArgumentException("A data do bloqueio não pode ser no passado.");
        }

        LocalDateTime inicioBloqueio = dto.horaInicio() != null ? dto.data().atTime(dto.horaInicio()) : dto.data().atStartOfDay();
        LocalDateTime fimBloqueio = dto.horaFim() != null ? dto.data().atTime(dto.horaFim()) : dto.data().atTime(LocalTime.MAX);

        boolean conflitoComReserva = agendamentoRepository.existeConflitoHorario(
                quadraId,
                inicioBloqueio,
                fimBloqueio,
                com.agendamentos.equadras.model.enums.StatusAgendamento.CANCELADO
        );
        if (conflitoComReserva) {
            throw new IllegalArgumentException("Não é possível bloquear este horário pois já existem reservas ativas no período.");
        }

        if (dto.horaInicio() == null && dto.horaFim() == null) {
            // Bloqueio do dia todo: verificar se já existe bloqueio de dia inteiro
            List<BloqueioHorario> existentes = bloqueioHorarioRepository.findByQuadraIdAndData(quadraId, dto.data());
            boolean jaExisteDiaInteiro = existentes.stream()
                    .anyMatch(b -> b.getHoraInicio() == null || b.getHoraFim() == null);
            if (jaExisteDiaInteiro) {
                throw new IllegalArgumentException("A quadra já possui um bloqueio cadastrado para o dia todo nesta data.");
            }
            // Remove eventuais bloqueios pontuais existentes nesta data para que o dia todo englobe a data
            if (!existentes.isEmpty()) {
                bloqueioHorarioRepository.deleteAll(existentes);
            }
        } else if (dto.horaInicio() != null && dto.horaFim() != null) {
            if (!dto.horaInicio().isBefore(dto.horaFim())) {
                throw new IllegalArgumentException("A hora de início deve ser anterior à hora de término.");
            }

            // Verificar se já existe um bloqueio de dia inteiro para a quadra nesta data
            List<BloqueioHorario> existentes = bloqueioHorarioRepository.findByQuadraIdAndData(quadraId, dto.data());
            List<BloqueioHorario> bloqueiosDiaInteiro = existentes.stream()
                    .filter(b -> b.getHoraInicio() == null || b.getHoraFim() == null)
                    .toList();

            if (!bloqueiosDiaInteiro.isEmpty()) {
                boolean substituir = Boolean.TRUE.equals(dto.substituirDiaInteiro());
                if (!substituir) {
                    throw new IllegalArgumentException("DIA_INTEIRO_BLOQUEADO: A quadra já está bloqueada o dia todo nesta data. Deseja desbloquear o restante do dia e manter bloqueado apenas este horário?");
                }
                // Se confirmou a substituição, remove o bloqueio de dia inteiro
                bloqueioHorarioRepository.deleteAll(bloqueiosDiaInteiro);
            } else {
                boolean conflitoExistente = existentes.stream().anyMatch(b ->
                        b.getHoraInicio() != null && b.getHoraFim() != null &&
                        dto.horaInicio().isBefore(b.getHoraFim()) && dto.horaFim().isAfter(b.getHoraInicio())
                );
                if (conflitoExistente) {
                    throw new IllegalArgumentException("Já existe um bloqueio cadastrado que coincide com este horário nesta data.");
                }
            }
        } else {
            throw new IllegalArgumentException("Para bloqueios com horário, ambos os horários (início e fim) devem ser fornecidos.");
        }

        BloqueioHorario bloqueio = new BloqueioHorario(quadra, dto.data(), dto.horaInicio(), dto.horaFim(), dto.motivo());
        BloqueioHorario salvo = bloqueioHorarioRepository.save(bloqueio);
        if (auditoriaService != null) {
            auditoriaService.registrarAcaoPorUsuarioId(adminId, CategoriaAuditoria.BLOQUEIO, "CRIAR", "BLOQUEIO",
                    salvo.getId().toString(),
                    "Bloqueio criado na quadra " + quadra.getNome() + " em " + dto.data()
                            + (dto.horaInicio() != null ? " (" + dto.horaInicio() + " - " + dto.horaFim() + ")" : " (Dia inteiro)")
                            + (dto.motivo() != null && !dto.motivo().isBlank() ? ". Motivo: " + dto.motivo() : ""));
        }
        return BloqueioHorarioResponseDTO.fromEntity(salvo);
    }

    @Transactional(readOnly = true)
    public List<BloqueioHorarioResponseDTO> listarBloqueios(Long quadraId) {
        return bloqueioHorarioRepository.findByQuadraId(quadraId)
                .stream()
                .map(BloqueioHorarioResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BloqueioHorarioResponseDTO> listarTodosDoAdmin(Long adminId) {
        Usuario admin = usuarioRepository.findById(adminId).orElse(null);
        if (admin != null && admin.isMasterAdmin()) {
            return bloqueioHorarioRepository.findAllOrdered()
                    .stream()
                    .map(BloqueioHorarioResponseDTO::fromEntity)
                    .toList();
        }

        return bloqueioHorarioRepository.findAllByAdminId(adminId)
                .stream()
                .map(BloqueioHorarioResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public void removerBloqueio(Long quadraId, Long bloqueioId, Long adminId) {
        Quadra quadra = quadraRepository.findByIdWithAdmin(quadraId)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + quadraId));

        if (!podeGerenciarBloqueio(quadra, adminId)) {
            throw new IllegalArgumentException("Apenas o administrador dono da quadra ou o Master Admin pode remover bloqueios.");
        }

        BloqueioHorario bloqueio = bloqueioHorarioRepository.findById(bloqueioId)
                .orElseThrow(() -> new IllegalArgumentException("Bloqueio não encontrado para o ID: " + bloqueioId));

        if (!bloqueio.getQuadra().getId_quadra().equals(quadraId)) {
            throw new IllegalArgumentException("O bloqueio informado não pertence a esta quadra.");
        }

        List<BloqueioHorario> paraRemover;
        if (bloqueio.getHoraInicio() == null || bloqueio.getHoraFim() == null) {
            paraRemover = bloqueioHorarioRepository.findByQuadraIdAndData(quadraId, bloqueio.getData())
                    .stream()
                    .filter(b -> b.getHoraInicio() == null || b.getHoraFim() == null)
                    .toList();
            if (paraRemover.isEmpty()) {
                paraRemover = List.of(bloqueio);
            }
        } else {
            paraRemover = bloqueioHorarioRepository.findByQuadraIdAndData(quadraId, bloqueio.getData())
                    .stream()
                    .filter(b -> java.util.Objects.equals(b.getHoraInicio(), bloqueio.getHoraInicio()) && java.util.Objects.equals(b.getHoraFim(), bloqueio.getHoraFim()))
                    .toList();
            if (paraRemover.isEmpty()) {
                paraRemover = List.of(bloqueio);
            }
        }

        if (paraRemover.size() == 1 && paraRemover.contains(bloqueio)) {
            bloqueioHorarioRepository.delete(bloqueio);
        } else {
            bloqueioHorarioRepository.deleteAll(paraRemover);
        }

        if (auditoriaService != null) {
            auditoriaService.registrarAcaoPorUsuarioId(adminId, CategoriaAuditoria.BLOQUEIO, "EXCLUIR", "BLOQUEIO",
                    bloqueioId.toString(),
                    "Bloqueio removido da quadra " + quadra.getNome() + " referente à data " + bloqueio.getData());
        }
    }

    @Transactional
    public int desbloquearHorarios(Long quadraId, com.agendamentos.equadras.dto.request.DesbloqueioHorarioDTO dto, Long adminId) {
        Quadra quadra = quadraRepository.findByIdWithAdmin(quadraId)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + quadraId));

        if (!podeGerenciarBloqueio(quadra, adminId)) {
            throw new IllegalArgumentException("Apenas o administrador dono da quadra ou o Master Admin pode remover bloqueios.");
        }

        if (dto.bloqueioId() != null) {
            removerBloqueio(quadraId, dto.bloqueioId(), adminId);
            return 1;
        }

        if (dto.data() == null) {
            throw new IllegalArgumentException("Informe o ID do bloqueio ou a data a ser desbloqueada.");
        }

        List<BloqueioHorario> bloqueios = bloqueioHorarioRepository.findByQuadraIdAndData(quadraId, dto.data());
        if (bloqueios.isEmpty()) {
            return 0;
        }

        List<BloqueioHorario> paraRemover;
        if (dto.horaInicio() != null && dto.horaFim() != null) {
            // Remove apenas bloqueios que casem exatamente com o intervalo solicitado
            paraRemover = bloqueios.stream()
                    .filter(b -> b.getHoraInicio() != null && b.getHoraFim() != null &&
                            b.getHoraInicio().equals(dto.horaInicio()) && b.getHoraFim().equals(dto.horaFim()))
                    .toList();
        } else {
            // Se não especificou horários, remove todos os bloqueios da data (dia inteiro e pontuais)
            paraRemover = bloqueios;
        }

        bloqueioHorarioRepository.deleteAll(paraRemover);
        return paraRemover.size();
    }
}
