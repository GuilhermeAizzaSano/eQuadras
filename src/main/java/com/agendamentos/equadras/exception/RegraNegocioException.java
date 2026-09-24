package com.agendamentos.equadras.exception;

/**
 * Lançada quando uma invariante ou regra de negócio é violada (HTTP 400).
 */
public class RegraNegocioException extends DomainException {

    private static final String DEFAULT_CODE = "REGRA_NEGOCIO_VIOLADA";

    public RegraNegocioException(String message) {
        super(DEFAULT_CODE, message);
    }

    public RegraNegocioException(String code, String message) {
        super(code != null ? code : DEFAULT_CODE, message);
    }

    public RegraNegocioException(String code, String message, Throwable cause) {
        super(code != null ? code : DEFAULT_CODE, message, cause);
    }
}
