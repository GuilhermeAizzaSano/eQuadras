package com.agendamentos.equadras.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/agendamentos");
    }

    @Test
    @DisplayName("Deve mapear SQLState 23P01 (Exclusion Violation / GiST) para 409 Conflict com código HORARIO_INDISPONIVEL")
    void deveMapear23P01ParaHorarioIndisponivel() {
        SQLException sqlEx = new SQLException("exclusion violation", "23P01");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("conflito", sqlEx);

        ProblemDetail problem = exceptionHandler.handleDataIntegrityViolation(dive, request);

        assertNotNull(problem);
        assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
        assertEquals("Conflito de Horário", problem.getTitle());
        assertEquals("HORARIO_INDISPONIVEL", problem.getProperties().get("code"));
        assertEquals("O horário selecionado conflita com outro agendamento já existente para esta quadra.", problem.getDetail());
    }

    @Test
    @DisplayName("Deve mapear SQLState 23505 (Unique Violation) para 409 Conflict com código REGISTRO_DUPLICADO")
    void deveMapear23505ParaRegistroDuplicado() {
        SQLException sqlEx = new SQLException("unique violation", "23505");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("duplicado", sqlEx);

        ProblemDetail problem = exceptionHandler.handleDataIntegrityViolation(dive, request);

        assertNotNull(problem);
        assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
        assertEquals("Registro Duplicado", problem.getTitle());
        assertEquals("REGISTRO_DUPLICADO", problem.getProperties().get("code"));
        assertEquals("A operação não pôde ser concluída pois já existe um registro com os mesmos dados identificadores.", problem.getDetail());
    }

    @Test
    @DisplayName("Deve mapear outras violações de integridade genéricas para CONFLITO_INTEGRIDADE")
    void deveMapearIntegridadeGenerica() {
        SQLException sqlEx = new SQLException("foreign key violation", "23503");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("fk", sqlEx);

        ProblemDetail problem = exceptionHandler.handleDataIntegrityViolation(dive, request);

        assertNotNull(problem);
        assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
        assertEquals("Conflito de Integridade de Dados", problem.getTitle());
        assertEquals("CONFLITO_INTEGRIDADE", problem.getProperties().get("code"));
    }
}
