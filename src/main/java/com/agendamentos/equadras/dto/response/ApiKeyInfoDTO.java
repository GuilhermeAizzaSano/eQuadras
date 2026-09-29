package com.agendamentos.equadras.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ApiKeyInfoDTO(
        @Schema(description = "Indica se a conta tem uma chave de API ativa", example = "true")
        boolean possuiChave,
        @Schema(description = "Quatro últimos caracteres da chave ativa", example = "a1b2")
        String last4,
        @Schema(description = "Instante de criação da chave (UTC)", example = "2026-09-20T12:00:00Z")
        Instant criadaEm,
        @Schema(description = "Instante do último uso da chave (UTC)", example = "2026-09-28T18:45:10Z")
        Instant ultimoUsoEm
) {}
