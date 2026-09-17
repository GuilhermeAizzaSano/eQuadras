package com.agendamentos.equadras.dto.response;

import java.time.Instant;

public record ApiKeyCriadaDTO(
        String apiKey,
        String last4,
        Instant criadaEm
) {}
