package com.agendamentos.equadras.repository;

import com.agendamentos.equadras.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    
    @Query("SELECT n FROM Notificacao n WHERE n.admin.id_usuario = :adminId ORDER BY n.dataCriacao DESC")
    List<Notificacao> findByAdminIdOrderByDataCriacaoDesc(@Param("adminId") Long adminId);
    
    @Query("SELECT COUNT(n) FROM Notificacao n WHERE n.admin.id_usuario = :adminId AND n.lida = false")
    long countByAdminIdAndLidaFalse(@Param("adminId") Long adminId);
}
