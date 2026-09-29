package com.agendamentos.equadras.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ApiKeyCriadaDTO(
        @Schema(description = "Chave em texto plano (eq_...). Só aparece nesta resposta", example = "eq_9f8e7d6c5b4a39281706f5e4d3c2b1a0")
        String apiKey,
        @Schema(description = "Quatro últimos caracteres da chave", example = "b1a0")
        String last4,
        @Schema(description = "Instante de criação da chave (UTC)", example = "2026-09-29T14:30:00Z")
        Instant criadaEm
) {}
