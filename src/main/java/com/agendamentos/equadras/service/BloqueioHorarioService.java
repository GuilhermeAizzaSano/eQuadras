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
import java.util.ArrayList;
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

        LocalDate data = dto.data();
        if (data == null && dto.bloqueioId() != null) {
            BloqueioHorario b = bloqueioHorarioRepository.findById(dto.bloqueioId()).orElse(null);
            if (b != null) {
                data = b.getData();
            }
        }

        // Se NÃO informou horários específicos (desbloqueio geral do dia ou por ID do bloqueio):
        if (dto.horaInicio() == null || dto.horaFim() == null) {
            if (dto.bloqueioId() != null) {
                removerBloqueio(quadraId, dto.bloqueioId(), adminId);
                return 1;
            }
            if (data == null) {
                throw new IllegalArgumentException("Informe o ID do bloqueio ou a data a ser desbloqueada.");
            }
            List<BloqueioHorario> bloqueios = bloqueioHorarioRepository.findByQuadraIdAndData(quadraId, data);
            if (bloqueios.isEmpty()) {
                return 0;
            }
            bloqueioHorarioRepository.deleteAll(bloqueios);
            if (auditoriaService != null) {
                auditoriaService.registrarAcaoPorUsuarioId(adminId, CategoriaAuditoria.BLOQUEIO, "EXCLUIR", "BLOQUEIO",
                        quadraId.toString(),
                        "Todos os bloqueios removidos da quadra " + quadra.getNome() + " referente à data " + data);
            }
            return bloqueios.size();
        }

        // Se informou horários específicos (desbloquear slot pontual):
        if (data == null) {
            throw new IllegalArgumentException("Informe a data a ser desbloqueada.");
        }

        LocalTime slotInicio = dto.horaInicio();
        LocalTime slotFim = dto.horaFim();
        if (slotFim.equals(LocalTime.MIDNIGHT)) {
            slotFim = LocalTime.of(23, 59, 59);
        }
        if (!slotInicio.isBefore(slotFim)) {
            throw new IllegalArgumentException("A hora de início deve ser anterior à hora de término.");
        }

        List<BloqueioHorario> existentes = bloqueioHorarioRepository.findByQuadraIdAndData(quadraId, data);
        if (existentes.isEmpty()) {
            return 0;
        }

        // Identifica os limites de funcionamento da quadra no dia da semana para o caso de bloqueio de dia inteiro
        java.time.DayOfWeek diaSemana = data.getDayOfWeek();
        LocalTime quadraAbertura = LocalTime.of(6, 0);
        LocalTime quadraFechamento = LocalTime.of(23, 0);
        if (quadra.getDisponibilidades() != null && !quadra.getDisponibilidades().isEmpty()) {
            for (com.agendamentos.equadras.model.entity.DisponibilidadeDia d : quadra.getDisponibilidades()) {
                if (d.getDiaSemana() == diaSemana) {
                    if (d.getHoraInicio() != null) quadraAbertura = d.getHoraInicio();
                    if (d.getHoraFim() != null) quadraFechamento = d.getHoraFim();
                    break;
                }
            }
        }

        // Filtra todos os bloqueios que colidem com [slotInicio, slotFim]
        final LocalTime sIni = slotInicio;
        final LocalTime sFim = slotFim;
        List<BloqueioHorario> sobrepostos = existentes.stream().filter(b -> {
            if (b.getHoraInicio() == null || b.getHoraFim() == null) {
                return true; // dia inteiro engloba o slot
            }
            return b.getHoraInicio().isBefore(sFim) && b.getHoraFim().isAfter(sIni);
        }).toList();

        if (sobrepostos.isEmpty()) {
            return 0;
        }

        boolean jaProcessouDiaInteiro = false;
        List<BloqueioHorario> novosBloqueios = new ArrayList<>();
        for (BloqueioHorario b : sobrepostos) {
            boolean isDiaInteiro = (b.getHoraInicio() == null || b.getHoraFim() == null);
            if (isDiaInteiro) {
                if (jaProcessouDiaInteiro) {
                    continue; // previne duplicatas residuais
                }
                jaProcessouDiaInteiro = true;
            }

            LocalTime bInicio;
            LocalTime bFim;
            if (isDiaInteiro) {
                bInicio = slotInicio.isBefore(quadraAbertura) ? slotInicio : quadraAbertura;
                bFim = (slotFim.isAfter(quadraFechamento) && !slotFim.equals(LocalTime.of(23, 59, 59))) ? slotFim : quadraFechamento;
            } else {
                bInicio = b.getHoraInicio();
                bFim = b.getHoraFim();
            }

            // Intervalo residual anterior ao slot
            if (bInicio.isBefore(slotInicio)) {
                novosBloqueios.add(new BloqueioHorario(quadra, data, bInicio, slotInicio, b.getMotivo()));
            }

            // Intervalo residual posterior ao slot
            if (slotFim.isBefore(bFim)) {
                novosBloqueios.add(new BloqueioHorario(quadra, data, slotFim, bFim, b.getMotivo()));
            }
        }

        bloqueioHorarioRepository.deleteAll(sobrepostos);
        if (!novosBloqueios.isEmpty()) {
            bloqueioHorarioRepository.saveAll(novosBloqueios);
        }

        if (auditoriaService != null) {
            auditoriaService.registrarAcaoPorUsuarioId(adminId, CategoriaAuditoria.BLOQUEIO, "EXCLUIR", "BLOQUEIO",
                    quadraId.toString(),
                    "Horário das " + slotInicio + " às " + slotFim + " desbloqueado na quadra " + quadra.getNome() + " em " + data);
        }

        return sobrepostos.size();
    }
}
