package com.agendamentos.equadras.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Trata erros de regras de negócio (ex: e-mail duplicado, ID não encontrado, conflitos)
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Requisição Inválida");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/bad-request"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "REQUISICAO_INVALIDA");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata regras de negócio em estado inválido (ex: exclusão de quadra com agendamentos)
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Operação Não Permitida");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/operacao-invalida"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "OPERACAO_NAO_PERMITIDA");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata acesso negado (permissão insuficiente / IDOR)
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problemDetail.setTitle("Acesso Negado");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/acesso-negado"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "ACESSO_NEGADO");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata violações de integridade referencial do banco de dados (ex: Foreign Keys)
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        String msg = "Não é possível excluir este registro pois ele possui agendamentos ou vínculos no banco de dados. Utilize a opção de inativar a quadra.";
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, msg);
        problemDetail.setTitle("Conflito de Integridade de Dados");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/conflito-integridade"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "CONFLITO_INTEGRIDADE");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata erros de anotações dos DTOs (@NotBlank, @Email, @Size)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, 
                "Erro de validação nos campos informados."
        );
        problemDetail.setTitle("Erro de Validação");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/validacao"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "ERRO_VALIDACAO");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        
        List<ErroCampoDTO> erros = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new ErroCampoDTO(err.getField(), err.getDefaultMessage()))
                .toList();

        problemDetail.setProperty("camposIncorretos", erros);
        return problemDetail;
    }

    // Handler global para exceções inesperadas
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Exceção não tratada na requisição [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado. Por favor, tente novamente mais tarde."
        );
        problemDetail.setTitle("Erro Interno do Servidor");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/internal-server-error"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "ERRO_INTERNO");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }
}
