package com.agendamentos.equadras.repository;

import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.enums.AbaAgendamento;
import com.agendamentos.equadras.specification.AgendamentoSpecifications;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Conta total e abas em uma única consulta (COUNT + SUM(CASE WHEN)), reaproveitando as regras de
 * {@link AgendamentoSpecifications#daAba} para não duplicar a definição de cada aba.
 */
class AgendamentoContagemRepositoryImpl implements AgendamentoContagemRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ContagemPorAba contarPorAba(Specification<Agendamento> base, LocalDateTime agora) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Agendamento> root = query.from(Agendamento.class);

        AbaAgendamento[] abas = AbaAgendamento.values();
        List<Selection<?>> colunas = new ArrayList<>();
        colunas.add(cb.count(root));
        for (AbaAgendamento aba : abas) {
            Predicate daAba = AgendamentoSpecifications.daAba(aba, agora).toPredicate(root, query, cb);
            colunas.add(cb.sum(cb.<Long>selectCase().when(daAba, 1L).otherwise(0L)));
        }
        query.multiselect(colunas);

        Predicate filtro = base.toPredicate(root, query, cb);
        if (filtro != null) {
            query.where(filtro);
        }

        Object[] linha = entityManager.createQuery(query).getSingleResult();
        Map<AbaAgendamento, Long> porAba = new EnumMap<>(AbaAgendamento.class);
        for (int i = 0; i < abas.length; i++) {
            porAba.put(abas[i], paraLong(linha[i + 1]));
        }
        return new ContagemPorAba(paraLong(linha[0]), porAba);
    }

    private static long paraLong(Object valor) {
        return valor == null ? 0L : ((Number) valor).longValue();
    }
}
