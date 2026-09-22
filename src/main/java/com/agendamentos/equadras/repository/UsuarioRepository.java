package com.agendamentos.equadras.repository;

import com.agendamentos.equadras.model.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("SELECT COUNT(u) > 0 FROM Usuario u WHERE u.email_usuario = :email")
    boolean existsByEmail_usuario(@Param("email") String email);

    @Query("SELECT u FROM Usuario u WHERE u.email_usuario = :email")
    Optional<Usuario> findByEmail_usuario(@Param("email") String email);

    @Query("SELECT u FROM Usuario u WHERE u.phone_usuario = :phone")
    Optional<Usuario> findByPhone_usuario(@Param("phone") String phone);

    Optional<Usuario> findByApiKeyHash(String apiKeyHash);

    java.util.List<Usuario> findByRole(com.agendamentos.equadras.model.enums.Role role);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Usuario u SET u.apiKeyUltimoUsoEm = :agora WHERE u.id_usuario = :id AND (u.apiKeyUltimoUsoEm IS NULL OR u.apiKeyUltimoUsoEm < :limite)")
    int atualizarUltimoUsoComThrottling(@Param("id") Long id, @Param("agora") java.time.Instant agora, @Param("limite") java.time.Instant limite);
}