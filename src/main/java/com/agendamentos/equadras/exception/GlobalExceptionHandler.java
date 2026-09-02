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

    // Trata regras de negócio em estado inválido (ex: exclusão de quadra com agendamentos)
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Operação Não Permitida");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/operacao-invalida"));
        return problemDetail;
    }

    // Trata violações de integridade referencial do banco de dados (ex: Foreign Keys)
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        String msg = "Não é possível excluir este registro pois ele possui agendamentos ou vínculos no banco de dados. Utilize a opção de inativar a quadra.";
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, msg);
        problemDetail.setTitle("Conflito de Integridade de Dados");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/conflito-integridade"));
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
