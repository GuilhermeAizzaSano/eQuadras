package com.agendamentos.equadras.repository;

import com.agendamentos.equadras.model.entity.Quadra;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuadraRepository extends JpaRepository<Quadra, Long> {
    @EntityGraph(attributePaths = {"admin", "fotos"})
    @Query("SELECT q FROM Quadra q WHERE q.admin.id_usuario = :adminId")
    List<Quadra> findByAdminId(@Param("adminId") Long adminId);
    
    @EntityGraph(attributePaths = {"admin", "fotos"})
    @Query("SELECT q FROM Quadra q WHERE q.id_quadra = :id")
    Optional<Quadra> findByIdWithAdmin(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"admin", "fotos"})
    @Query("SELECT q FROM Quadra q WHERE q.id_quadra = :id")
    Optional<Quadra> buscarComLockParaAgendamento(@Param("id") Long id);

    @EntityGraph(attributePaths = {"fotos"})
    List<Quadra> findByAtivaTrue();

    @EntityGraph(attributePaths = {"admin", "fotos"})
    Optional<Quadra> findById(Long id);

    @Query(value = "SELECT q.* FROM quadras q WHERE q.ativa = true AND q.latitude IS NOT NULL AND q.longitude IS NOT NULL " +
           "AND (6371 * acos(LEAST(1.0, GREATEST(-1.0, cos(radians(:latitude)) * cos(radians(q.latitude)) * " +
           "cos(radians(q.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
           "sin(radians(q.latitude)))))) <= :raioKm " +
           "ORDER BY (6371 * acos(LEAST(1.0, GREATEST(-1.0, cos(radians(:latitude)) * cos(radians(q.latitude)) * " +
           "cos(radians(q.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
           "sin(radians(q.latitude)))))) ASC", nativeQuery = true)
    List<Quadra> findByAtivaTrueAndProximidadeMenorQue(
            @Param("latitude") Double latitude, 
            @Param("longitude") Double longitude, 
            @Param("raioKm") Double raioKm);
}