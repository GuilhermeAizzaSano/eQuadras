package com.agendamentos.equadras.repository;

import com.agendamentos.equadras.model.entity.Quadra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuadraRepository extends JpaRepository<Quadra, Long>, JpaSpecificationExecutor<Quadra> {
    @EntityGraph(attributePaths = {"admin", "fotos"})
    @Query("SELECT q FROM Quadra q WHERE q.admin.id_usuario = :adminId")
    List<Quadra> findByAdminId(@Param("adminId") Long adminId);

    @EntityGraph(attributePaths = {"admin", "fotos"})
    @Query("SELECT q FROM Quadra q")
    List<Quadra> findAllWithAdminEFotos();
    
    @EntityGraph(attributePaths = {"admin", "fotos"})
    @Query("SELECT q FROM Quadra q WHERE q.id_quadra = :id")
    Optional<Quadra> findByIdWithAdmin(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"admin", "fotos"})
    @Query("SELECT q FROM Quadra q WHERE q.id_quadra = :id")
    Optional<Quadra> buscarComLockParaAgendamento(@Param("id") Long id);

    @EntityGraph(attributePaths = {"fotos"})
    Page<Quadra> findByAtivaTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"fotos"})
    Page<Quadra> findAll(Specification<Quadra> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"fotos"})
    List<Quadra> findAll(Specification<Quadra> spec);

    @EntityGraph(attributePaths = {"admin", "fotos"})
    Optional<Quadra> findById(Long id);

    @Query(value = "SELECT q.* FROM quadras q WHERE q.ativa = true AND q.latitude IS NOT NULL AND q.longitude IS NOT NULL " +
           "AND q.latitude BETWEEN :minLat AND :maxLat " +
           "AND q.longitude BETWEEN :minLng AND :maxLng " +
           "AND (6371 * acos(LEAST(1.0, GREATEST(-1.0, cos(radians(:latitude)) * cos(radians(q.latitude)) * " +
           "cos(radians(q.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
           "sin(radians(q.latitude)))))) <= :raioKm " +
           "ORDER BY (6371 * acos(LEAST(1.0, GREATEST(-1.0, cos(radians(:latitude)) * cos(radians(q.latitude)) * " +
           "cos(radians(q.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
           "sin(radians(q.latitude)))))) ASC", nativeQuery = true)
    List<Quadra> findByAtivaTrueAndProximidadeMenorQue(
            @Param("latitude") Double latitude, 
            @Param("longitude") Double longitude, 
            @Param("raioKm") Double raioKm,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng);

    @Query("SELECT COUNT(q), COUNT(CASE WHEN q.ativa = true THEN 1 END) FROM Quadra q WHERE q.admin.id_usuario = :adminId")
    List<Object[]> obterMetricasQuadrasPorAdminId(@Param("adminId") Long adminId);

    @Query("SELECT COUNT(q), COUNT(CASE WHEN q.ativa = true THEN 1 END) FROM Quadra q")
    List<Object[]> obterMetricasQuadrasMasterAdmin();
}