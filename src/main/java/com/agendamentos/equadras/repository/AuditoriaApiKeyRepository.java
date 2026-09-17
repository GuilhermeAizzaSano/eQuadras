package com.agendamentos.equadras.repository;

import com.agendamentos.equadras.model.entity.AuditoriaApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaApiKeyRepository extends JpaRepository<AuditoriaApiKey, Long> {
    List<AuditoriaApiKey> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
}
