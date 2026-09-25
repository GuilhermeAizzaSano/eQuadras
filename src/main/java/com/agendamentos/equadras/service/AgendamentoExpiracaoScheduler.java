package com.agendamentos.equadras.service;

import com.agendamentos.equadras.event.AgendamentosExpiradosCanceladosEvent;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class AgendamentoExpiracaoScheduler {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoExpiracaoScheduler.class);

    private final AgendamentoRepository agendamentoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public AgendamentoExpiracaoScheduler(AgendamentoRepository agendamentoRepository,
                                         ApplicationEventPublisher eventPublisher,
                                         Clock clock) {
        this.agendamentoRepository = agendamentoRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirarAgendamentosPendentes() {
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime limite = agora.minusMinutes(15);
        int cancelados = agendamentoRepository.cancelarPendentesExpirados(
                StatusAgendamento.PENDENTE,
                StatusAgendamento.CANCELADO,
                limite,
                agora
        );
        if (cancelados > 0) {
            log.info("Rotina de expiração: {} agendamentos pendentes expirados foram cancelados.", cancelados);
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new AgendamentosExpiradosCanceladosEvent(cancelados));
            }
        }
    }
}
