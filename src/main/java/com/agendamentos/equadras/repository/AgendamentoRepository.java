package com.agendamentos.equadras.repository;

import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    @Query("""
        SELECT COUNT(a) > 0 FROM Agendamento a
        WHERE a.quadra.id_quadra = :quadraId
        AND a.status <> :statusCancelado
        AND (a.dataHoraInicio < :fim AND a.dataHoraFim > :inicio)
    """)
    boolean existeConflitoHorario(
            @Param("quadraId") Long quadraId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("statusCancelado") StatusAgendamento statusCancelado
    );

    @EntityGraph(attributePaths = {"usuario", "quadra"})
    @Query("""
        SELECT a FROM Agendamento a
        WHERE a.quadra.id_quadra = :quadraId
        AND a.status <> :statusCancelado
        AND (a.dataHoraInicio < :fimDoDia AND a.dataHoraFim > :inicioDoDia)
        ORDER BY a.dataHoraInicio ASC
    """)
    List<Agendamento> buscarPorQuadraEData(
            @Param("quadraId") Long quadraId,
            @Param("statusCancelado") StatusAgendamento statusCancelado,
            @Param("inicioDoDia") LocalDateTime inicioDoDia,
            @Param("fimDoDia") LocalDateTime fimDoDia
    );

    @EntityGraph(attributePaths = {"usuario", "quadra"})
    @Query("SELECT a FROM Agendamento a WHERE a.usuario.id_usuario = :usuarioId")
    List<Agendamento> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @EntityGraph(attributePaths = {"usuario", "quadra"})
    @Query("SELECT a FROM Agendamento a WHERE a.quadra.admin.id_usuario = :adminId")
    List<Agendamento> findByAdminId(@Param("adminId") Long adminId);

    @Query("SELECT COUNT(a) > 0 FROM Agendamento a WHERE a.quadra.id_quadra = :quadraId")
    boolean existsByQuadraId(@Param("quadraId") Long quadraId);

    @EntityGraph(attributePaths = {"usuario", "quadra"})
    List<Agendamento> findAll();

    @Override
    @EntityGraph(attributePaths = {"usuario", "quadra"})
    java.util.Optional<Agendamento> findById(Long id);
}