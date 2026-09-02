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

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              UsuarioRepository usuarioRepository,
                              QuadraRepository quadraRepository,
                              NotificacaoService notificacaoService,
                              PagamentoService pagamentoService) {
        this.agendamentoRepository = agendamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.quadraRepository = quadraRepository;
        this.notificacaoService = notificacaoService;
        this.pagamentoService = pagamentoService;
    }

    @Transactional
    public AgendamentoResponseDTO agendar(AgendamentoCriacaoDTO dto) {
        if (!dto.dataHoraFim().isAfter(dto.dataHoraInicio())) {
            throw new IllegalArgumentException("A data/hora de término deve ser posterior à data/hora de início.");
        }

        if (dto.dataHoraInicio().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível realizar agendamentos em horários passados.");
        }

        Usuario usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado. ID: " + dto.usuarioId()));

        Quadra quadra = quadraRepository.findByIdWithAdmin(dto.quadraId())
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada. ID: " + dto.quadraId()));

        if (!quadra.isAtiva()) {
            throw new IllegalArgumentException("Esta quadra está inativa para agendamentos.");
        }

        boolean conflito = agendamentoRepository.existeConflitoHorario(
                quadra.getId_quadra(),
                dto.dataHoraInicio(),
                dto.dataHoraFim(),
                StatusAgendamento.CANCELADO
        );

        if (conflito) {
            throw new IllegalArgumentException("Este horário não está disponível para agendamento. Por favor, escolha outro horário.");
        }

        long minutos = Duration.between(dto.dataHoraInicio(), dto.dataHoraFim()).toMinutes();
        BigDecimal horas = BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal valorTotal = quadra.getValorHora().multiply(horas);

        Agendamento agendamento = Agendamento.builder()
                .usuario(usuario)
                .quadra(quadra)
                .dataHoraInicio(dto.dataHoraInicio())
                .dataHoraFim(dto.dataHoraFim())
                .valorTotal(valorTotal)
                .status(StatusAgendamento.PENDENTE)
                .build();

        // Gerar Pix de Pagamento
        PagamentoService.PixDados pixDados = pagamentoService.gerarPix(agendamento);
        agendamento.setTransacaoPagamentoId(pixDados.transacaoId());
        agendamento.setPixCopiaECola(pixDados.pixCopiaECola());
        agendamento.setQrCodeBase64(pixDados.qrCodeBase64());

        Agendamento agendamentoSalvo = agendamentoRepository.save(agendamento);

        return AgendamentoResponseDTO.fromEntity(agendamentoSalvo);
    }

    @Transactional
    public AgendamentoResponseDTO confirmarPagamento(Long idAgendamento) {
        Agendamento agendamento = agendamentoRepository.findById(idAgendamento)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado. ID: " + idAgendamento));

        if (agendamento.getStatus() == StatusAgendamento.CONFIRMADO) {
            return AgendamentoResponseDTO.fromEntity(agendamento);
        }

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new IllegalStateException("Não é possível confirmar pagamento de um agendamento cancelado.");
        }

        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        Agendamento salvo = agendamentoRepository.save(agendamento);

        // Notificar o admin em tempo real após a confirmação do pagamento
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

        return AgendamentoResponseDTO.fromEntity(salvo);
    }

    @Transactional
    public AgendamentoResponseDTO cancelar(Long id, Long usuarioId) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado para o ID: " + id));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (usuario.getRole() == com.agendamentos.equadras.model.enums.Role.CLIENT) {
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
                .map(AgendamentoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HorarioDisponivelDTO> listarHorariosDisponiveis(Long quadraId, LocalDate data) {
        Quadra quadra = quadraRepository.findById(quadraId)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + quadraId));

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

        LocalTime slotInicio = HORARIO_ABERTURA;
        while (slotInicio.isBefore(HORARIO_FECHAMENTO)) {
            LocalTime slotFim = slotInicio.plusHours(1);
            LocalDateTime slotDataHoraInicio = data.atTime(slotInicio);
            LocalDateTime slotDataHoraFim = data.atTime(slotFim);

            boolean disponivel = true;
            String motivo = "Disponível";

            if (!quadra.isAtiva()) {
                disponivel = false;
                motivo = "Quadra inativa";
            } else if (slotDataHoraInicio.isBefore(agora)) {
                disponivel = false;
                motivo = "Horário indisponível (passado)";
            } else {
                boolean ocupado = agendamentosDoDia.stream().anyMatch(a ->
                        a.getDataHoraInicio().isBefore(slotDataHoraFim) && a.getDataHoraFim().isAfter(slotDataHoraInicio)
                );
                if (ocupado) {
                    disponivel = false;
                    motivo = "Horário ocupado";
                }
            }

            slots.add(new HorarioDisponivelDTO(slotInicio, slotFim, disponivel, motivo));
            slotInicio = slotFim;
        }

        return slots;
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listarTodos(Long usuarioId) {
        List<Agendamento> agendamentos;
        
        if (usuarioId != null) {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            if (usuario != null && usuario.getRole() == com.agendamentos.equadras.model.enums.Role.ADMIN) {
                agendamentos = agendamentoRepository.findByAdminId(usuarioId);
            } else if (usuario != null && usuario.getRole() == com.agendamentos.equadras.model.enums.Role.CLIENT) {
                agendamentos = agendamentoRepository.findByUsuarioId(usuarioId);
            } else {
                agendamentos = agendamentoRepository.findAll();
            }
        } else {
            agendamentos = agendamentoRepository.findAll();
        }

        return agendamentos.stream()
                .map(AgendamentoResponseDTO::fromEntity)
                .toList();
    }
}