package com.agendamentos.equadras.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Trata erros de regras de negócio (ex: e-mail duplicado, ID não encontrado, conflitos)
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Requisição Inválida");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/bad-request"));
        return problemDetail;
    }

    // Trata erros de anotações dos DTOs (@NotBlank, @Email, @Size)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, 
                "Erro de validação nos campos informados."
        );
        problemDetail.setTitle("Erro de Validação");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/validacao"));
        
        List<ErroCampoDTO> erros = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new ErroCampoDTO(err.getField(), err.getDefaultMessage()))
                .toList();

        problemDetail.setProperty("camposIncorretos", erros);
        return problemDetail;
    }
}
