package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.model.entity.BloqueioHorario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record BloqueioHorarioResponseDTO(
        Long id,
        Long quadraId,
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        String motivo,
        LocalDateTime criadoEm
) {
    public static BloqueioHorarioResponseDTO fromEntity(BloqueioHorario b) {
        return new BloqueioHorarioResponseDTO(
                b.getId(),
                b.getQuadra() != null ? b.getQuadra().getId_quadra() : null,
                b.getData(),
                b.getHoraInicio(),
                b.getHoraFim(),
                b.getMotivo(),
                b.getCriadoEm()
        );
    }
}
