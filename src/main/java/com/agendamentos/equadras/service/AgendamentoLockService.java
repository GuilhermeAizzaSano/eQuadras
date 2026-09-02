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

    public AgendamentoLockService(AgendamentoRepository agendamentoRepository,
                                  UsuarioRepository usuarioRepository,
                                  QuadraRepository quadraRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.quadraRepository = quadraRepository;
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

        if (!dto.dataHoraInicio().toLocalDate().isEqual(dto.dataHoraFim().toLocalDate())) {
            throw new IllegalArgumentException("Horário selecionado está fora do horário de funcionamento da quadra.");
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

        java.time.LocalTime horaInicio = dto.dataHoraInicio().toLocalTime();
        java.time.LocalTime horaFim = dto.dataHoraFim().toLocalTime();

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
