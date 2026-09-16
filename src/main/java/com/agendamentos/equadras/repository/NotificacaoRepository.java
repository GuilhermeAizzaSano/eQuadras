package com.agendamentos.equadras.repository;

import com.agendamentos.equadras.model.entity.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    
    @Query("SELECT n FROM Notificacao n WHERE n.admin.id_usuario = :adminId AND n.excluida = false ORDER BY n.dataCriacao DESC")
    Page<Notificacao> findByAdminIdAndExcluidaFalseOrderByDataCriacaoDesc(@Param("adminId") Long adminId, Pageable pageable);

    @Query("SELECT n FROM Notificacao n WHERE n.admin.id_usuario = :adminId AND n.excluida = false ORDER BY n.dataCriacao DESC")
    List<Notificacao> findByAdminIdAndExcluidaFalseOrderByDataCriacaoDesc(@Param("adminId") Long adminId);
    
    @Query("SELECT COUNT(n) FROM Notificacao n WHERE n.admin.id_usuario = :adminId AND n.lida = false AND n.excluida = false")
    long countByAdminIdAndLidaFalse(@Param("adminId") Long adminId);

    @Modifying
    @Query("UPDATE Notificacao n SET n.lida = true WHERE n.admin.id_usuario = :adminId AND n.lida = false AND n.excluida = false")
    void marcarTodasComoLidas(@Param("adminId") Long adminId);

    @Modifying
    @Query("UPDATE Notificacao n SET n.excluida = true WHERE n.admin.id_usuario = :adminId AND n.excluida = false")
    void marcarTodasComoExcluidas(@Param("adminId") Long adminId);
}
