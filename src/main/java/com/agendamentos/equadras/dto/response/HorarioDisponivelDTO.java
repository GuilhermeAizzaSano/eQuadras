package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.model.enums.StatusHorario;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

@Schema(description = "Representação detalhada de cada slot de horário de uma quadra no dia")
public record HorarioDisponivelDTO(
        @Schema(description = "Horário de início do slot", type = "string", format = "time", example = "14:00:00")
        LocalTime inicio,

        @Schema(description = "Horário de término do slot", type = "string", format = "time", example = "15:00:00")
        LocalTime fim,

        @Schema(description = "Indica se o horário pode ser agendado no momento", example = "true")
        boolean disponivel,

        @Schema(description = "Status detalhado: DISPONIVEL, BLOQUEADO, AGENDADO ou INDISPONIVEL", example = "DISPONIVEL")
        StatusHorario status,

        @Schema(description = "Justificativa ou motivo do status", example = "Disponível")
        String motivo
) {
    // Construtor de compatibilidade para código ou testes que não passam o StatusHorario explicitamente
    public HorarioDisponivelDTO(LocalTime inicio, LocalTime fim, boolean disponivel, String motivo) {
        this(
                inicio,
                fim,
                disponivel,
                disponivel ? StatusHorario.DISPONIVEL :
                        (motivo != null && motivo.toLowerCase().contains("bloque") ? StatusHorario.BLOQUEADO :
                                (motivo != null && motivo.toLowerCase().contains("ocupad") ? StatusHorario.AGENDADO : StatusHorario.INDISPONIVEL)),
                motivo
        );
    }
}

