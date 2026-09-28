package com.agendamentos.equadras.dto.request;

import com.agendamentos.equadras.model.enums.TipoEsporte;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuadraCriacaoDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private QuadraCriacaoDTO dto(String estado, Double lat, Double lng, BigDecimal valor, List<String> fotos,
                                 List<DisponibilidadeDiaDTO> disps) {
        return new QuadraCriacaoDTO("Quadra Teste", TipoEsporte.FUTSAL, valor, null, null, null, null,
                estado, lat, lng, null, null, fotos, disps);
    }

    private boolean valido(QuadraCriacaoDTO d) {
        return validator.validate(d).isEmpty();
    }

    @Test
    @DisplayName("M5/M6: rejeita os valores aceitos indevidamente no DB_AUDIT")
    void rejeitaInvalidos() {
        BigDecimal dez = BigDecimal.TEN;
        assertFalse(valido(dto("1x", null, null, dez, null, null)), "UF");
        assertFalse(valido(dto(null, 999.0, null, dez, null, null)), "latitude");
        assertFalse(valido(dto(null, null, -181.0, dez, null, null)), "longitude");
        assertFalse(valido(dto(null, null, null, new BigDecimal("10.123"), null, null)), "casas decimais");
        assertFalse(valido(dto(null, null, null, new BigDecimal("100000000"), null, null)), "valor máximo");
        assertFalse(valido(dto(null, null, null, dez, List.of("h".repeat(256)), null)), "URL da foto");
        assertFalse(valido(dto(null, null, null, dez, null,
                List.of(new DisponibilidadeDiaDTO(DayOfWeek.MONDAY, LocalTime.of(20, 0), LocalTime.of(8, 0))))), "horário invertido");
        assertFalse(valido(dto(null, null, null, dez, null, List.of(
                new DisponibilidadeDiaDTO(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                new DisponibilidadeDiaDTO(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(14, 0))))), "dia repetido");
    }

    @Test
    @DisplayName("M5/M6: aceita valores no limite")
    void aceitaLimites() {
        assertTrue(valido(dto("SP", -90.0, 180.0, new BigDecimal("99999999.99"), List.of("h".repeat(255)),
                List.of(new DisponibilidadeDiaDTO(DayOfWeek.MONDAY, LocalTime.of(6, 0), LocalTime.of(23, 0))))));
        assertTrue(valido(dto(null, null, null, BigDecimal.TEN, null, null)));
    }
}
