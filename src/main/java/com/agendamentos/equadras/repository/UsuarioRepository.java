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
}