package com.agendamentos.equadras.dto.response;

import java.time.Instant;

public record ApiKeyInfoDTO(
        boolean possuiChave,
        String last4,
        Instant criadaEm,
        Instant ultimoUsoEm
) {}
