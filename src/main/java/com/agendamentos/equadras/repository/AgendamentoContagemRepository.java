package com.agendamentos.equadras.repository;

import com.agendamentos.equadras.model.entity.Agendamento;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public interface AgendamentoContagemRepository {

    ContagemPorAba contarPorAba(Specification<Agendamento> base, LocalDateTime agora);
}
