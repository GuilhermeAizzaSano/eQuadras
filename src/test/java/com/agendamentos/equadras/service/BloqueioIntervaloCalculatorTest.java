package com.agendamentos.equadras.service;

import com.agendamentos.equadras.model.entity.BloqueioHorario;
import com.agendamentos.equadras.model.entity.Quadra;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BloqueioIntervaloCalculatorTest {

    private BloqueioIntervaloCalculator calculator;
    private Quadra quadra;
    private LocalDate data;

    @BeforeEach
    void setUp() {
        calculator = new BloqueioIntervaloCalculator();
        quadra = Quadra.builder().id_quadra(1L).nome("Quadra Teste").build();
        data = LocalDate.now().plusDays(2);
    }

    @Test
    @DisplayName("Deve retornar vazio quando a lista de sobrepostos for nula ou vazia")
    void deveRetornarVazioQuandoListaNulaOuVazia() {
        assertTrue(calculator.calcularResiduosDesbloqueio(null, LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(6, 0), LocalTime.of(23, 0)).isEmpty());
        assertTrue(calculator.calcularResiduosDesbloqueio(List.of(), LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(6, 0), LocalTime.of(23, 0)).isEmpty());
    }

    @Test
    @DisplayName("Deve calcular resíduo posterior para desbloqueio no início de um dia inteiro")
    void deveCalcularResiduoPosteriorParaInicioDiaInteiro() {
        BloqueioHorario diaTodo = new BloqueioHorario(quadra, data, null, null, "Manutenção");
        LocalTime slotInicio = LocalTime.of(6, 0);
        LocalTime slotFim = LocalTime.of(7, 0);
        LocalTime quadraAbertura = LocalTime.of(6, 0);
        LocalTime quadraFechamento = LocalTime.of(23, 59, 59);

        var residuos = calculator.calcularResiduosDesbloqueio(
                List.of(diaTodo), slotInicio, slotFim, quadraAbertura, quadraFechamento
        );

        assertEquals(1, residuos.size());
        assertEquals(LocalTime.of(7, 0), residuos.get(0).inicio());
        assertEquals(LocalTime.of(23, 59, 59), residuos.get(0).fim());
        assertEquals("Manutenção", residuos.get(0).motivo());
    }

    @Test
    @DisplayName("Deve calcular resíduos anterior e posterior para desbloqueio intermediário em dia inteiro")
    void deveCalcularResiduosAnteriorEPosteriorParaDiaInteiro() {
        BloqueioHorario diaTodo = new BloqueioHorario(quadra, data, null, null, "Torneio");
        LocalTime slotInicio = LocalTime.of(12, 0);
        LocalTime slotFim = LocalTime.of(13, 0);
        LocalTime quadraAbertura = LocalTime.of(6, 0);
        LocalTime quadraFechamento = LocalTime.of(23, 59, 59);

        var residuos = calculator.calcularResiduosDesbloqueio(
                List.of(diaTodo), slotInicio, slotFim, quadraAbertura, quadraFechamento
        );

        assertEquals(2, residuos.size());
        assertEquals(LocalTime.of(6, 0), residuos.get(0).inicio());
        assertEquals(LocalTime.of(12, 0), residuos.get(0).fim());
        assertEquals(LocalTime.of(13, 0), residuos.get(1).inicio());
        assertEquals(LocalTime.of(23, 59, 59), residuos.get(1).fim());
    }

    @Test
    @DisplayName("Deve calcular resíduo anterior para desbloqueio no final do dia")
    void deveCalcularResiduoAnteriorParaFinalDoDia() {
        BloqueioHorario diaTodo = new BloqueioHorario(quadra, data, null, null, "Evento");
        LocalTime slotInicio = LocalTime.of(22, 0);
        LocalTime slotFim = LocalTime.of(23, 59, 59);
        LocalTime quadraAbertura = LocalTime.of(6, 0);
        LocalTime quadraFechamento = LocalTime.of(23, 59, 59);

        var residuos = calculator.calcularResiduosDesbloqueio(
                List.of(diaTodo), slotInicio, slotFim, quadraAbertura, quadraFechamento
        );

        assertEquals(1, residuos.size());
        assertEquals(LocalTime.of(6, 0), residuos.get(0).inicio());
        assertEquals(LocalTime.of(22, 0), residuos.get(0).fim());
    }

    @Test
    @DisplayName("Deve ignorar duplicações de dia inteiro na mesma lista de sobrepostos")
    void deveIgnorarDuplicatasDeDiaInteiro() {
        BloqueioHorario diaTodo1 = new BloqueioHorario(quadra, data, null, null, "Motivo 1");
        BloqueioHorario diaTodo2 = new BloqueioHorario(quadra, data, null, null, "Motivo 2");

        var residuos = calculator.calcularResiduosDesbloqueio(
                List.of(diaTodo1, diaTodo2),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                LocalTime.of(6, 0),
                LocalTime.of(23, 0)
        );

        // Apenas o primeiro é processado gerando 2 resíduos, ignorando duplicata
        assertEquals(2, residuos.size());
    }

    @Test
    @DisplayName("Deve recortar bloqueio pontual corretamente")
    void deveRecortarBloqueioPontual() {
        BloqueioHorario pontual = new BloqueioHorario(quadra, data, LocalTime.of(10, 0), LocalTime.of(16, 0), "Treino");

        var residuos = calculator.calcularResiduosDesbloqueio(
                List.of(pontual),
                LocalTime.of(12, 0),
                LocalTime.of(14, 0),
                LocalTime.of(6, 0),
                LocalTime.of(23, 0)
        );

        assertEquals(2, residuos.size());
        assertEquals(LocalTime.of(10, 0), residuos.get(0).inicio());
        assertEquals(LocalTime.of(12, 0), residuos.get(0).fim());
        assertEquals(LocalTime.of(14, 0), residuos.get(1).inicio());
        assertEquals(LocalTime.of(16, 0), residuos.get(1).fim());
    }
}
