package com.agendamentos.equadras.repository;

import com.agendamentos.equadras.model.enums.AbaAgendamento;

import java.util.Map;

/**
 * Resultado agregado de uma única consulta: total do escopo e quantidade por aba.
 */
public record ContagemPorAba(long total, Map<AbaAgendamento, Long> porAba) {
}
