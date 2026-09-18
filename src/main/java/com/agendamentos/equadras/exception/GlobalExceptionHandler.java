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

    // Trata violações de integridade referencial, unique constraints e constraints de exclusão GiST (23P01)
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        String sqlState = null;
        Throwable causa = ex.getMostSpecificCause();
        if (causa instanceof java.sql.SQLException sqlEx) {
            sqlState = sqlEx.getSQLState();
        }

        String msg;
        String code;
        String title;

        if ("23P01".equalsIgnoreCase(sqlState)) {
            // PostgreSQL exclusion_violation (ex: agendamento_sem_sobreposicao via EXCLUDE USING GIST)
            title = "Conflito de Horário";
            code = "HORARIO_INDISPONIVEL";
            msg = "O horário selecionado conflita com outro agendamento já existente para esta quadra.";
        } else if ("23505".equalsIgnoreCase(sqlState)) {
            // PostgreSQL unique_violation (ex: transacaoPagamentoId duplicada, e-mail já existente)
            title = "Registro Duplicado";
            code = "REGISTRO_DUPLICADO";
            msg = "A operação não pôde ser concluída pois já existe um registro com os mesmos dados identificadores.";
        } else {
            title = "Conflito de Integridade de Dados";
            code = "CONFLITO_INTEGRIDADE";
            msg = "Não é possível concluir a operação pois o registro possui vínculos ativos no banco de dados.";
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, msg);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("https://api.equadras.com/erros/" + code.toLowerCase().replace('_', '-')));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", code);
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
