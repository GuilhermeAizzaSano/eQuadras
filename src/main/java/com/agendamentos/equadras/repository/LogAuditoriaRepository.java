package com.agendamentos.equadras.repository;

import com.agendamentos.equadras.model.entity.LogAuditoria;
import com.agendamentos.equadras.model.enums.CategoriaAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long>, JpaSpecificationExecutor<LogAuditoria> {

    @Query("SELECT COUNT(l) FROM LogAuditoria l WHERE l.acao = :acao AND l.criadoEm >= :desde")
    long countByAcaoDesde(@Param("acao") String acao, @Param("desde") Instant desde);

    @Query("SELECT COUNT(l) FROM LogAuditoria l WHERE l.categoria = :categoria AND l.criadoEm >= :desde")
    long countByCategoriaDesde(@Param("categoria") CategoriaAuditoria categoria, @Param("desde") Instant desde);
}
