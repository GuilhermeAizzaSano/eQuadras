package com.agendamentos.equadras.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DataFlexivelUtilTest {

    @Test
    @DisplayName("Deve retornar null quando data for nula ou vazia")
    void deveRetornarNullQuandoVazio() {
        assertNull(DataFlexivelUtil.resolverData(null));
        assertNull(DataFlexivelUtil.resolverData(""));
        assertNull(DataFlexivelUtil.resolverData("   "));
    }

    @Test
    @DisplayName("Deve resolver termos relativos como hoje e amanha")
    void deveResolverTermosRelativos() {
        LocalDate hoje = LocalDate.now(DataFlexivelUtil.ZONE_BRASIL);
        assertEquals(hoje, DataFlexivelUtil.resolverData("hoje"));
        assertEquals(hoje.plusDays(1), DataFlexivelUtil.resolverData("amanhã"));
        assertEquals(hoje.plusDays(1), DataFlexivelUtil.resolverData("amanha"));
        assertEquals(hoje.plusDays(2), DataFlexivelUtil.resolverData("depois de amanhã"));
    }

    @Test
    @DisplayName("Deve resolver dias da semana para o próximo dia correspondente")
    void deveResolverDiasDaSemana() {
        LocalDate resultadoSexta = DataFlexivelUtil.resolverData("sexta");
        assertNotNull(resultadoSexta);
        assertEquals(DayOfWeek.FRIDAY, resultadoSexta.getDayOfWeek());

        LocalDate resultadoSabado = DataFlexivelUtil.resolverData("sábado");
        assertNotNull(resultadoSabado);
        assertEquals(DayOfWeek.SATURDAY, resultadoSabado.getDayOfWeek());

        LocalDate resultadoSegunda = DataFlexivelUtil.resolverData("segunda-feira");
        assertNotNull(resultadoSegunda);
        assertEquals(DayOfWeek.MONDAY, resultadoSegunda.getDayOfWeek());
    }

    @Test
    @DisplayName("Deve resolver múltiplos formatos numéricos de data")
    void deveResolverFormatosNumericos() {
        assertEquals(LocalDate.of(2026, 9, 15), DataFlexivelUtil.resolverData("2026-09-15"));
        assertEquals(LocalDate.of(2026, 9, 15), DataFlexivelUtil.resolverData("15/09/2026"));
        assertEquals(LocalDate.of(2026, 9, 15), DataFlexivelUtil.resolverData("15-09-2026"));

        LocalDate dataSemAno = DataFlexivelUtil.resolverData("25/12");
        assertNotNull(dataSemAno);
        assertEquals(12, dataSemAno.getMonthValue());
        assertEquals(25, dataSemAno.getDayOfMonth());
    }
}
