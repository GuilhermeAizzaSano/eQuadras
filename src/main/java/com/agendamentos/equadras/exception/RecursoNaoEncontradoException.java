package com.agendamentos.equadras.exception;

/**
 * Lançada quando um recurso ou entidade não é localizado no sistema (HTTP 404).
 */
public class RecursoNaoEncontradoException extends DomainException {

    private static final String DEFAULT_CODE = "RECURSO_NAO_ENCONTRADO";

    public RecursoNaoEncontradoException(String message) {
        super(DEFAULT_CODE, message);
    }

    public RecursoNaoEncontradoException(String code, String message) {
        super(code != null ? code : DEFAULT_CODE, message);
    }
}
