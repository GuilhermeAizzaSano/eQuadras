package com.agendamentos.equadras.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

@Embeddable
public class DisponibilidadeDia {

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 20)
    private DayOfWeek diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    public DisponibilidadeDia() {}

    public DisponibilidadeDia(DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFim) {
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
    }

    public DayOfWeek getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(DayOfWeek diaSemana) {
        this.diaSemana = diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }

    public void setHoraFim(LocalTime horaFim) {
        this.horaFim = horaFim;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DisponibilidadeDia that = (DisponibilidadeDia) o;
        return diaSemana == that.diaSemana &&
                Objects.equals(horaInicio, that.horaInicio) &&
                Objects.equals(horaFim, that.horaFim);
    }

    @Override
    public int hashCode() {
        return Objects.hash(diaSemana, horaInicio, horaFim);
    }
}
