package com.agendamentos.equadras.exception;

public record ErroCampoDTO(
        String campo,
        String mensagem
) {}