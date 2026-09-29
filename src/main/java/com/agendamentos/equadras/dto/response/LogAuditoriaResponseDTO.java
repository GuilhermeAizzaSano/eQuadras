package com.agendamentos.equadras.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.agendamentos.equadras.model.entity.LogAuditoria;
import com.agendamentos.equadras.model.enums.CategoriaAuditoria;
import com.agendamentos.equadras.model.enums.TipoExecutor;

import java.time.Instant;

public record LogAuditoriaResponseDTO(
        @Schema(description = "ID do registro de auditoria", example = "981")
        Long id,
        @Schema(description = "ID do usuário que executou a ação", example = "10")
        Long usuarioId,
        @Schema(description = "E-mail do executor no momento da ação", example = "arthur.prado@email.com")
        String usuarioEmail,
        @Schema(description = "Nome do executor no momento da ação", example = "Arthur Prado")
        String usuarioNome,
        @Schema(description = "Categoria do evento: AUTENTICACAO, AGENDAMENTO, QUADRA, USUARIO, BLOQUEIO ou API_KEY", example = "AUTENTICACAO")
        CategoriaAuditoria categoria,
        @Schema(description = "Código da ação executada", example = "LOGOUT")
        String acao,
        @Schema(description = "Entidade afetada", example = "USUARIO")
        String entidade,
        @Schema(description = "ID do recurso afetado", example = "10")
        String recursoId,
        @Schema(description = "Tipo do executor da ação (ex.: MASTER_ADMIN, ADMIN_QUADRA, CLIENTE)", example = "CLIENTE")
        TipoExecutor tipoExecutor,
        @Schema(description = "Detalhes em texto livre", example = "Logout efetuado com sucesso.")
        String detalhes,
        @Schema(description = "Endereço IP de origem", example = "203.0.113.10")
        String ip,
        @Schema(description = "User-Agent do cliente", example = "Mozilla/5.0")
        String userAgent,
        @Schema(description = "Instante do registro (UTC)", example = "2026-09-29T15:40:00Z")
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
