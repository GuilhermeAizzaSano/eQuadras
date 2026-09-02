package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
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

@Service
public class AgendamentoLockService {

    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final QuadraRepository quadraRepository;
    private final com.agendamentos.equadras.repository.BloqueioHorarioRepository bloqueioHorarioRepository;

    public AgendamentoLockService(AgendamentoRepository agendamentoRepository,
                                  UsuarioRepository usuarioRepository,
                                  QuadraRepository quadraRepository,
                                  com.agendamentos.equadras.repository.BloqueioHorarioRepository bloqueioHorarioRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.quadraRepository = quadraRepository;
        this.bloqueioHorarioRepository = bloqueioHorarioRepository;
    }

    @Transactional
    public Agendamento criarAgendamentoPendenteComLock(AgendamentoCriacaoDTO dto, Long usuarioIdAutenticado) {
        Usuario usuario = usuarioRepository.findById(usuarioIdAutenticado)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado. ID: " + usuarioIdAutenticado));

        Quadra quadra = quadraRepository.buscarComLockParaAgendamento(dto.quadraId())
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada. ID: " + dto.quadraId()));

        if (!quadra.isAtiva()) {
            throw new IllegalArgumentException("Esta quadra está inativa para agendamentos.");
        }

        if (quadra.getDataLimiteAgendamento() != null && dto.dataHoraInicio().toLocalDate().isAfter(quadra.getDataLimiteAgendamento())) {
            throw new IllegalArgumentException("Esta quadra não aceita agendamentos após " + quadra.getDataLimiteAgendamento());
        }

        if (!dto.dataHoraInicio().toLocalDate().isEqual(dto.dataHoraFim().toLocalDate())) {
            throw new IllegalArgumentException("Horário selecionado está fora do horário de funcionamento da quadra.");
        }

        java.time.LocalDate dataAgendamento = dto.dataHoraInicio().toLocalDate();
        java.time.LocalTime horaInicio = dto.dataHoraInicio().toLocalTime();
        java.time.LocalTime horaFim = dto.dataHoraFim().toLocalTime();

        java.util.List<com.agendamentos.equadras.model.entity.BloqueioHorario> bloqueios = bloqueioHorarioRepository.findByQuadraIdAndData(quadra.getId_quadra(), dataAgendamento);
        for (com.agendamentos.equadras.model.entity.BloqueioHorario b : bloqueios) {
            boolean colide = (b.getHoraInicio() == null || b.getHoraFim() == null)
                    || (b.getHoraInicio().isBefore(horaFim) && b.getHoraFim().isAfter(horaInicio));
            if (colide) {
                String motivoMsg = (b.getMotivo() != null && !b.getMotivo().isBlank())
                        ? b.getMotivo()
                        : "Horário bloqueado pelo administrador";
                throw new IllegalArgumentException("Este horário está bloqueado pelo administrador. Motivo: " + motivoMsg);
            }
        }

        java.time.DayOfWeek diaSemana = dto.dataHoraInicio().getDayOfWeek();
        com.agendamentos.equadras.model.entity.DisponibilidadeDia disp = null;
        if (quadra.getDisponibilidades() != null) {
            disp = quadra.getDisponibilidades().stream()
                    .filter(d -> d.getDiaSemana() == diaSemana)
                    .findFirst()
                    .orElse(null);
        }

        if (disp == null) {
            throw new IllegalArgumentException("Horário selecionado está fora do horário de funcionamento da quadra.");
        }

        if (horaInicio.isBefore(disp.getHoraInicio()) || horaFim.isAfter(disp.getHoraFim())) {
            throw new IllegalArgumentException("Horário selecionado está fora do horário de funcionamento da quadra.");
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

        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public Agendamento atualizarDadosPix(Long agendamentoId, PagamentoService.PixDados pixDados) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado. ID: " + agendamentoId));

        agendamento.setTransacaoPagamentoId(pixDados.transacaoId());
        agendamento.setPixCopiaECola(pixDados.pixCopiaECola());
        agendamento.setQrCodeBase64(pixDados.qrCodeBase64());

        return agendamentoRepository.save(agendamento);
    }
}
