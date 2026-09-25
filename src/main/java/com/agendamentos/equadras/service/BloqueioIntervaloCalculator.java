package com.agendamentos.equadras.service;

import com.agendamentos.equadras.model.entity.BloqueioHorario;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class BloqueioIntervaloCalculator {

    public record IntervaloResidual(LocalTime inicio, LocalTime fim, String motivo) {}

    public List<IntervaloResidual> calcularResiduosDesbloqueio(
            List<BloqueioHorario> sobrepostos,
            LocalTime slotInicio,
            LocalTime slotFim,
            LocalTime quadraAbertura,
            LocalTime quadraFechamento) {

        if (sobrepostos == null || sobrepostos.isEmpty()) {
            return List.of();
        }

        boolean jaProcessouDiaInteiro = false;
        List<IntervaloResidual> novosBloqueios = new ArrayList<>();

        for (BloqueioHorario b : sobrepostos) {
            boolean isDiaInteiro = (b.getHoraInicio() == null || b.getHoraFim() == null);
            if (isDiaInteiro) {
                if (jaProcessouDiaInteiro) {
                    continue; // previne duplicatas residuais
                }
                jaProcessouDiaInteiro = true;
            }

            LocalTime bInicio;
            LocalTime bFim;
            if (isDiaInteiro) {
                bInicio = slotInicio.isBefore(quadraAbertura) ? slotInicio : quadraAbertura;
                bFim = (slotFim.isAfter(quadraFechamento) && !slotFim.equals(LocalTime.of(23, 59, 59))) ? slotFim : quadraFechamento;
            } else {
                bInicio = b.getHoraInicio();
                bFim = b.getHoraFim();
            }

            // Intervalo residual anterior ao slot
            if (bInicio.isBefore(slotInicio)) {
                novosBloqueios.add(new IntervaloResidual(bInicio, slotInicio, b.getMotivo()));
            }

            // Intervalo residual posterior ao slot
            if (slotFim.isBefore(bFim)) {
                novosBloqueios.add(new IntervaloResidual(slotFim, bFim, b.getMotivo()));
            }
        }

        return novosBloqueios;
    }
}
