package com.agendamentos.equadras.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status do horário/slot da quadra: DISPONIVEL, BLOQUEADO, AGENDADO ou INDISPONIVEL")
public enum StatusHorario {
    DISPONIVEL,
    BLOQUEADO,
    AGENDADO,
    INDISPONIVEL
}
