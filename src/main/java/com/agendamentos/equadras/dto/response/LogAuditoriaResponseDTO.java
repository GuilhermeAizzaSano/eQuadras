package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.model.entity.LogAuditoria;
import com.agendamentos.equadras.model.enums.CategoriaAuditoria;
import com.agendamentos.equadras.model.enums.TipoExecutor;

import java.time.Instant;

public record LogAuditoriaResponseDTO(
        Long id,
        Long usuarioId,
        String usuarioEmail,
        String usuarioNome,
        CategoriaAuditoria categoria,
        String acao,
        String entidade,
        String recursoId,
        TipoExecutor tipoExecutor,
        String detalhes,
        String ip,
        String userAgent,
        Instant criadoEm
) {
    public static LogAuditoriaResponseDTO fromEntity(LogAuditoria log) {
        return new LogAuditoriaResponseDTO(
                log.getId(),
                log.getUsuarioId(),
                log.getUsuarioEmail(),
                log.getUsuarioNome(),
                log.getCategoria(),
                log.getAcao(),
                log.getEntidade(),
                log.getRecursoId(),
                log.getTipoExecutor(),
                log.getDetalhes(),
                log.getIp(),
                log.getUserAgent(),
                log.getCriadoEm()
        );
    }
}
