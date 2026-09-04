package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.dto.response.HorarioDisponivelDTO;
import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgendamentoService {

    private static final LocalTime HORARIO_ABERTURA = LocalTime.of(6, 0);
    private static final LocalTime HORARIO_FECHAMENTO = LocalTime.of(23, 0);

    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final QuadraRepository quadraRepository;
    private final NotificacaoService notificacaoService;
    private final PagamentoService pagamentoService;
    private final AgendamentoLockService agendamentoLockService;
    private final com.agendamentos.equadras.repository.BloqueioHorarioRepository bloqueioHorarioRepository;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              UsuarioRepository usuarioRepository,
                              QuadraRepository quadraRepository,
                              NotificacaoService notificacaoService,
                              PagamentoService pagamentoService,
                              AgendamentoLockService agendamentoLockService,
                              com.agendamentos.equadras.repository.BloqueioHorarioRepository bloqueioHorarioRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.quadraRepository = quadraRepository;
        this.notificacaoService = notificacaoService;
        this.pagamentoService = pagamentoService;
        this.agendamentoLockService = agendamentoLockService;
        this.bloqueioHorarioRepository = bloqueioHorarioRepository;
    }

    public AgendamentoResponseDTO agendar(AgendamentoCriacaoDTO dto, Long usuarioIdAutenticado) {
        if (!dto.dataHoraFim().isAfter(dto.dataHoraInicio())) {
            throw new IllegalArgumentException("A data/hora de término deve ser posterior à data/hora de início.");
        }

        if (dto.dataHoraInicio().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível realizar agendamentos em horários passados.");
        }

        // 1. Cria o agendamento em transação com lock pessimista na quadra e commita imediatamente
        Agendamento agendamentoSalvo = agendamentoLockService.criarAgendamentoPendenteComLock(dto, usuarioIdAutenticado);

        // 2. Chama API externa FORA da transação e do lock do banco
        PagamentoService.PixDados pixDados = pagamentoService.gerarPix(agendamentoSalvo);

        // 3. Atualiza os dados Pix em nova transação leve
        Agendamento agendamentoAtualizado = agendamentoLockService.atualizarDadosPix(agendamentoSalvo.getId_agendamento(), pixDados);

        return AgendamentoResponseDTO.fromEntity(agendamentoAtualizado);
    }

    @Transactional
    public AgendamentoResponseDTO confirmarPagamento(Long idAgendamento, Long usuarioIdAutenticado) {
        Agendamento agendamento = agendamentoRepository.findById(idAgendamento)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado. ID: " + idAgendamento));

        Usuario usuarioAutenticado = usuarioRepository.findById(usuarioIdAutenticado).orElse(null);
        boolean ehMasterAdmin = usuarioAutenticado != null && usuarioAutenticado.isMasterAdmin();
        boolean ehDono = agendamento.getUsuario().getId_usuario().equals(usuarioIdAutenticado);
        boolean ehAdminDaQuadra = agendamento.getQuadra().getAdmin() != null
                && agendamento.getQuadra().getAdmin().getId_usuario().equals(usuarioIdAutenticado);
        if (!ehDono && !ehAdminDaQuadra && !ehMasterAdmin) {
            throw new IllegalArgumentException("Você não tem permissão para confirmar o pagamento deste agendamento.");
        }

        if (agendamento.getStatus() == StatusAgendamento.CONFIRMADO) {
            return AgendamentoResponseDTO.fromEntity(agendamento);
        }

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new IllegalStateException("Não é possível confirmar pagamento de um agendamento cancelado.");
        }

        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        Agendamento salvo = agendamentoRepository.save(agendamento);

        notificarAdminPagamento(salvo);

        return AgendamentoResponseDTO.fromEntity(salvo);
    }

    @Transactional
    public AgendamentoResponseDTO confirmarPagamentoPorWebhook(Long idAgendamento, String transacaoId) {
        Agendamento agendamento = null;
        if (idAgendamento != null) {
            agendamento = agendamentoRepository.findById(idAgendamento).orElse(null);
        }
        if (agendamento == null && transacaoId != null && !transacaoId.isBlank()) {
            agendamento = agendamentoRepository.findByTransacaoPagamentoId(transacaoId).orElse(null);
        }

        if (agendamento == null) {
            throw new IllegalArgumentException("Agendamento não encontrado para conciliação do pagamento (ID: "
                    + idAgendamento + ", transacaoId: " + transacaoId + ")");
        }

        if (agendamento.getStatus() == StatusAgendamento.CONFIRMADO) {
            return AgendamentoResponseDTO.fromEntity(agendamento);
        }

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new IllegalStateException("Não é possível confirmar pagamento de um agendamento cancelado.");
        }

        if (transacaoId != null && !transacaoId.isBlank() && agendamento.getTransacaoPagamentoId() == null) {
            agendamento.setTransacaoPagamentoId(transacaoId);
        }

        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        Agendamento salvo = agendamentoRepository.save(agendamento);

        notificarAdminPagamento(salvo);

        return AgendamentoResponseDTO.fromEntity(salvo);
    }

    private void notificarAdminPagamento(Agendamento salvo) {
        try {
            if (salvo.getQuadra().getAdmin() != null) {
                java.time.format.DateTimeFormatter formatadorData = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                java.time.format.DateTimeFormatter formatadorHora = java.time.format.DateTimeFormatter.ofPattern("HH:mm");

                String dataFormatada = salvo.getDataHoraInicio().format(formatadorData);
                String horaInicio = salvo.getDataHoraInicio().format(formatadorHora);
                String horaFim = salvo.getDataHoraFim().format(formatadorHora);
                String msg = String.format("Pagamento Pix confirmado!\n%s agendou %s\nData: %s (%s às %s)",
                        salvo.getUsuario().getNome_usuario(), salvo.getQuadra().getNome(), dataFormatada, horaInicio, horaFim);

                notificacaoService.enviarNotificacao(salvo.getQuadra().getAdmin().getId_usuario(), msg);
            }
        } catch (Exception e) {
            System.err.println("Falha ao enviar notificação: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AgendamentoResponseDTO buscarPorId(Long idAgendamento, Long usuarioIdAutenticado) {
        Agendamento agendamento = agendamentoRepository.findById(idAgendamento)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado. ID: " + idAgendamento));

        Usuario usuarioAutenticado = usuarioRepository.findById(usuarioIdAutenticado).orElse(null);
        boolean ehMasterAdmin = usuarioAutenticado != null && usuarioAutenticado.isMasterAdmin();
        boolean ehDono = agendamento.getUsuario().getId_usuario().equals(usuarioIdAutenticado);
        boolean ehAdminDaQuadra = agendamento.getQuadra().getAdmin() != null
                && agendamento.getQuadra().getAdmin().getId_usuario().equals(usuarioIdAutenticado);
        if (!ehDono && !ehAdminDaQuadra && !ehMasterAdmin) {
            throw new IllegalArgumentException("Você não tem permissão para visualizar este agendamento.");
        }

        return AgendamentoResponseDTO.fromEntity(agendamento);
    }

    @Transactional
    public AgendamentoResponseDTO cancelar(Long id, Long usuarioId) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado para o ID: " + id));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (usuario.isMasterAdmin()) {
            // Master Admin tem permissão para cancelar qualquer agendamento
        } else if (usuario.getRole() == com.agendamentos.equadras.model.enums.Role.CLIENT) {
            if (!agendamento.getUsuario().getId_usuario().equals(usuarioId)) {
                throw new IllegalArgumentException("Você não tem permissão para cancelar este agendamento.");
            }
        } else if (usuario.getRole() == com.agendamentos.equadras.model.enums.Role.ADMIN) {
            if (!agendamento.getQuadra().getAdmin().getId_usuario().equals(usuarioId)) {
                throw new IllegalArgumentException("Você não tem permissão para cancelar agendamentos desta quadra.");
            }
        }

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new IllegalArgumentException("Este agendamento já está cancelado.");
        }

        if (agendamento.getDataHoraInicio().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível cancelar um agendamento retroativo ou já iniciado.");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        Agendamento agendamentoAtualizado = agendamentoRepository.save(agendamento);
        return AgendamentoResponseDTO.fromEntity(agendamentoAtualizado);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listarPorQuadraEData(Long quadraId, LocalDate data) {
        LocalDateTime inicioDoDia = data.atStartOfDay();
        LocalDateTime fimDoDia = data.atTime(LocalTime.MAX);

        return agendamentoRepository
                .buscarPorQuadraEData(
                        quadraId,
                        StatusAgendamento.CANCELADO,
                        inicioDoDia,
                        fimDoDia
                )
                .stream()
                .map(AgendamentoResponseDTO::fromEntitySemPix)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HorarioDisponivelDTO> listarHorariosDisponiveis(Long quadraId, LocalDate data) {
        Quadra quadra = quadraRepository.findById(quadraId)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + quadraId));

        java.time.DayOfWeek diaSemana = data.getDayOfWeek();
        com.agendamentos.equadras.model.entity.DisponibilidadeDia disp = null;
        if (quadra.getDisponibilidades() != null) {
            disp = quadra.getDisponibilidades().stream()
                    .filter(d -> d.getDiaSemana() == diaSemana)
                    .findFirst()
                    .orElse(null);
        }

        if (disp == null) {
            return List.of();
        }

        LocalDateTime inicioDoDia = data.atStartOfDay();
        LocalDateTime fimDoDia = data.atTime(LocalTime.MAX);

        List<Agendamento> agendamentosDoDia = agendamentoRepository.buscarPorQuadraEData(
                quadraId,
                StatusAgendamento.CANCELADO,
                inicioDoDia,
                fimDoDia
        );

        LocalDateTime agora = LocalDateTime.now();
        List<HorarioDisponivelDTO> slots = new ArrayList<>();

        boolean dataLimiteExcedida = quadra.getDataLimiteAgendamento() != null && data.isAfter(quadra.getDataLimiteAgendamento());
        List<com.agendamentos.equadras.model.entity.BloqueioHorario> bloqueios = bloqueioHorarioRepository.findByQuadraIdAndData(quadraId, data);
        com.agendamentos.equadras.model.entity.BloqueioHorario bloqueioDiaInteiro = bloqueios.stream()
                .filter(b -> b.getHoraInicio() == null || b.getHoraFim() == null)
                .findFirst()
                .orElse(null);

        LocalTime slotInicio = disp.getHoraInicio();
        while (slotInicio.isBefore(disp.getHoraFim())) {
            LocalTime slotFim = slotInicio.plusHours(1);
            LocalDateTime slotDataHoraInicio = data.atTime(slotInicio);
            LocalDateTime slotDataHoraFim = data.atTime(slotFim);

            boolean disponivel = true;
            String motivo = "Disponível";
            com.agendamentos.equadras.model.enums.StatusHorario status = com.agendamentos.equadras.model.enums.StatusHorario.DISPONIVEL;

            if (!quadra.isAtiva()) {
                disponivel = false;
                status = com.agendamentos.equadras.model.enums.StatusHorario.INDISPONIVEL;
                motivo = "Quadra inativa";
            } else if (dataLimiteExcedida) {
                disponivel = false;
                status = com.agendamentos.equadras.model.enums.StatusHorario.BLOQUEADO;
                motivo = "Data limite de agendamento encerrada";
            } else if (bloqueioDiaInteiro != null) {
                disponivel = false;
                status = com.agendamentos.equadras.model.enums.StatusHorario.BLOQUEADO;
                motivo = (bloqueioDiaInteiro.getMotivo() != null && !bloqueioDiaInteiro.getMotivo().isBlank())
                        ? "Bloqueado: " + bloqueioDiaInteiro.getMotivo()
                        : "Horário bloqueado pelo administrador";
            } else if (slotDataHoraInicio.isBefore(agora)) {
                disponivel = false;
                status = com.agendamentos.equadras.model.enums.StatusHorario.INDISPONIVEL;
                motivo = "Horário indisponível (passado)";
            } else {
                final LocalTime sIni = slotInicio;
                final LocalTime sFim = slotFim;
                com.agendamentos.equadras.model.entity.BloqueioHorario bloqueioParcial = bloqueios.stream()
                        .filter(b -> b.getHoraInicio() != null && b.getHoraFim() != null)
                        .filter(b -> b.getHoraInicio().isBefore(sFim) && b.getHoraFim().isAfter(sIni))
                        .findFirst()
                        .orElse(null);

                if (bloqueioParcial != null) {
                    disponivel = false;
                    status = com.agendamentos.equadras.model.enums.StatusHorario.BLOQUEADO;
                    motivo = (bloqueioParcial.getMotivo() != null && !bloqueioParcial.getMotivo().isBlank())
                            ? "Bloqueado: " + bloqueioParcial.getMotivo()
                            : "Horário bloqueado pelo administrador";
                } else {
                    boolean ocupado = agendamentosDoDia.stream().anyMatch(a ->
                            a.getDataHoraInicio().isBefore(slotDataHoraFim) && a.getDataHoraFim().isAfter(slotDataHoraInicio)
                    );
                    if (ocupado) {
                        disponivel = false;
                        status = com.agendamentos.equadras.model.enums.StatusHorario.AGENDADO;
                        motivo = "Horário ocupado / agendado";
                    }
                }
            }

            slots.add(new HorarioDisponivelDTO(slotInicio, slotFim, disponivel, status, motivo));
            slotInicio = slotFim;
        }

        return slots;
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listarTodos(Long usuarioId) {
        return listarTodos(usuarioId, false);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listarTodos(Long usuarioId, boolean historico) {
        List<Agendamento> agendamentos;
        
        if (usuarioId != null) {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            if (usuario != null && usuario.getRole() == com.agendamentos.equadras.model.enums.Role.ADMIN) {
                if (usuario.isMasterAdmin()) {
                    // Master Admin vê todos os agendamentos (histórico completo ou apenas ativos)
                    if (historico) {
                        agendamentos = agendamentoRepository.findAll();
                    } else {
                        agendamentos = agendamentoRepository.findAtivosAll(
                                StatusAgendamento.CANCELADO,
                                LocalDateTime.now()
                        );
                    }
                } else {
                    // Admin comum vê os agendamentos das suas quadras (histórico completo ou apenas ativos)
                    if (historico) {
                        agendamentos = agendamentoRepository.findByAdminId(usuarioId);
                    } else {
                        agendamentos = agendamentoRepository.findAtivosByAdminId(
                                usuarioId,
                                StatusAgendamento.CANCELADO,
                                LocalDateTime.now()
                        );
                    }
                }
            } else if (usuario != null && usuario.getRole() == com.agendamentos.equadras.model.enums.Role.CLIENT) {
                if (historico) {
                    agendamentos = agendamentoRepository.findByUsuarioId(usuarioId);
                } else {
                    agendamentos = agendamentoRepository.findAtivosByUsuarioId(
                            usuarioId,
                            StatusAgendamento.CANCELADO,
                            LocalDateTime.now()
                    );
                }
            } else {
                agendamentos = agendamentoRepository.findAll();
            }
        } else {
            agendamentos = agendamentoRepository.findAll();
        }

        return agendamentos.stream()
                .map(AgendamentoResponseDTO::fromEntitySemPix)
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.Map<Long, List<HorarioDisponivelDTO>> listarHorariosDoDiaParaAdmin(LocalDate data, Long adminId) {
        Usuario admin = usuarioRepository.findById(adminId).orElse(null);
        List<Quadra> quadrasDoAdmin;
        if (admin != null && admin.isMasterAdmin()) {
            // Master Admin tem a visão do dia de todas as quadras ativas do sistema
            quadrasDoAdmin = quadraRepository.findAllWithAdminEFotos();
        } else {
            quadrasDoAdmin = quadraRepository.findByAdminId(adminId);
        }

        java.util.Map<Long, List<HorarioDisponivelDTO>> mapaResultado = new java.util.LinkedHashMap<>();

        for (Quadra quadra : quadrasDoAdmin) {
            if (quadra.isAtiva()) {
                List<HorarioDisponivelDTO> slots = listarHorariosDisponiveis(quadra.getId_quadra(), data);
                mapaResultado.put(quadra.getId_quadra(), slots);
            } else {
                mapaResultado.put(quadra.getId_quadra(), List.of());
            }
        }

        return mapaResultado;
    }
}