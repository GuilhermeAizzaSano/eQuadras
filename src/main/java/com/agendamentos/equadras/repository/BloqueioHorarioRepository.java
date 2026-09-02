package com.agendamentos.equadras.repository;

import com.agendamentos.equadras.model.entity.BloqueioHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BloqueioHorarioRepository extends JpaRepository<BloqueioHorario, Long> {

    @Query("SELECT b FROM BloqueioHorario b WHERE b.quadra.id_quadra = :quadraId AND b.data = :data")
    List<BloqueioHorario> findByQuadraIdAndData(@Param("quadraId") Long quadraId, @Param("data") LocalDate data);

    @Query("SELECT b FROM BloqueioHorario b WHERE b.quadra.id_quadra = :quadraId ORDER BY b.data ASC, b.horaInicio ASC NULLS FIRST")
    List<BloqueioHorario> findByQuadraId(@Param("quadraId") Long quadraId);

    @Query("SELECT b FROM BloqueioHorario b WHERE b.quadra.admin.id_usuario = :adminId ORDER BY b.data ASC, b.horaInicio ASC NULLS FIRST")
    List<BloqueioHorario> findAllByAdminId(@Param("adminId") Long adminId);
}
