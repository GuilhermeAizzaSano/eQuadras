package com.agendamentos.equadras.dto.response;

import java.time.LocalTime;

public record HorarioDisponivelDTO(
        LocalTime inicio,
        LocalTime fim,
        boolean disponivel,
        String motivo
) {}
