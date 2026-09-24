package com.agendamentos.equadras.specification;

import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.enums.AbaAgendamento;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class AgendamentoSpecifications {

    private AgendamentoSpecifications() {}

    public static Specification<Agendamento> doUsuario(Long usuarioId) {
        return (root, query, cb) -> cb.equal(root.get("usuario").get("id_usuario"), usuarioId);
    }

    public static Specification<Agendamento> daAba(AbaAgendamento aba, LocalDateTime agora) {
        return switch (aba) {
            case ATIVOS -> (root, query, cb) -> cb.and(
                    cb.notEqual(root.get("status"), StatusAgendamento.CANCELADO),
                    cb.greaterThanOrEqualTo(root.get("dataHoraFim"), agora)
            );
            case REALIZADOS -> (root, query, cb) -> cb.and(
                    cb.notEqual(root.get("status"), StatusAgendamento.CANCELADO),
                    cb.lessThan(root.get("dataHoraFim"), agora)
            );
            case CANCELADOS -> (root, query, cb) -> cb.equal(root.get("status"), StatusAgendamento.CANCELADO);
        };
    }

    public static Specification<Agendamento> comStatus(StatusAgendamento status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Agendamento> daQuadra(Long quadraId) {
        return (root, query, cb) -> cb.equal(root.get("quadra").get("id_quadra"), quadraId);
    }

    public static Specification<Agendamento> apenasPendentesValidos(LocalDateTime agora) {
        // Pendente e criado há menos de 15 minutos
        LocalDateTime limite = agora.minusMinutes(15);
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), StatusAgendamento.PENDENTE),
                cb.greaterThan(root.get("criadoEm"), limite)
        );
    }
}
