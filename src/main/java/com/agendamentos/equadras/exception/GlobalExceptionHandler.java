package com.agendamentos.equadras.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Trata recurso/entidade não encontrada (HTTP 404)
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Recurso Não Encontrado");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/recurso-nao-encontrado"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", ex.getCode());
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata violações explícitas de regras de negócio de domínio (HTTP 400)
    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail handleRegraNegocio(RegraNegocioException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Regra de Negócio Violada");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/regra-negocio-violada"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", ex.getCode());
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata erros de requisição inválida legados / argumentos incorretos (Fallback)
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, traduzirMensagemNegocio(ex.getMessage()));
        problemDetail.setTitle("Requisição Inválida");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/bad-request"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "REQUISICAO_INVALIDA");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata regras de negócio em estado inválido (ex: cancelamento de reserva já cancelada ou realizada)
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, traduzirMensagemNegocio(ex.getMessage()));
        problemDetail.setTitle("Operação Não Permitida");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/operacao-invalida"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "OPERACAO_NAO_PERMITIDA");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata acesso negado (permissão insuficiente de perfil ou recurso de outro usuário)
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String msg = ex.getMessage();
        if (msg == null || msg.equalsIgnoreCase("Access is denied") || msg.contains("Access Denied")) {
            msg = "Você não possui permissão para acessar ou modificar este recurso.";
        }
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, msg);
        problemDetail.setTitle("Acesso Negado");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/acesso-negado"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "ACESSO_NEGADO");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata falha de autenticação
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Sessão expirada ou credenciais inválidas. Por favor, realize o login novamente."
        );
        problemDetail.setTitle("Não Autorizado");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/nao-autorizado"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "NAO_AUTORIZADO");
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
            title = "Conflito de Horário";
            code = "HORARIO_INDISPONIVEL";
            msg = "O horário selecionado conflita com outro agendamento já existente ou bloqueado para esta quadra.";
        } else if ("23505".equalsIgnoreCase(sqlState)) {
            title = "Registro Duplicado";
            code = "REGISTRO_DUPLICADO";
            msg = "Já existe um registro cadastrado com essas informações no sistema (ex: e-mail ou identificador já existente).";
        } else if ("23503".equalsIgnoreCase(sqlState)) {
            title = "Registro em Uso";
            code = "REGISTRO_EM_USO";
            msg = "Não é possível remover ou modificar este registro, pois existem agendamentos ou dados associados a ele.";
        } else {
            title = "Conflito de Dados";
            code = "CONFLITO_INTEGRIDADE";
            msg = "A operação não pôde ser concluída devido a regras de integridade do banco de dados.";
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, msg);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("https://api.equadras.com/erros/" + code.toLowerCase().replace('_', '-')));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", code);
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata erros de anotações dos DTOs (@NotBlank, @Email, @Size, @NotNull)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErroCampoDTO> erros = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new ErroCampoDTO(traduzirNomeCampo(err.getField()), err.getDefaultMessage()))
                .toList();

        String resumoErros = erros.stream()
                .map(e -> String.format("%s: %s", e.campo(), e.mensagem()))
                .collect(Collectors.joining("; "));

        String detalhe = resumoErros.isBlank()
                ? "Por favor, preencha todos os campos obrigatórios corretamente."
                : "Dados inválidos: " + resumoErros;

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, detalhe);
        problemDetail.setTitle("Dados Inválidos");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/validacao"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "ERRO_VALIDACAO");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("camposIncorretos", erros);
        return problemDetail;
    }

    // Trata JSON malformado ou campos com tipo incompatível no corpo da requisição
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "O formato dos dados enviados está incorreto ou possui campos inválidos. Verifique as informações fornecidas."
        );
        problemDetail.setTitle("Formato de Dados Inválido");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/dados-invalidos"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "DADOS_INVALIDOS");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata parâmetros com tipos incorretos na URL (ex: ID que deveria ser número enviado como texto)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String detalhe = String.format("O parâmetro '%s' informado possui formato inválido.", ex.getName());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
        problemDetail.setTitle("Parâmetro Inválido");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/parametro-invalido"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "PARAMETRO_INVALIDO");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata parâmetros obrigatórios ausentes na URL
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String detalhe = String.format("O parâmetro obrigatório '%s' não foi informado na requisição.", ex.getParameterName());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
        problemDetail.setTitle("Parâmetro Ausente");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/parametro-ausente"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "PARAMETRO_AUSENTE");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata ordenação inválida ou fora da whitelist permitida
    @ExceptionHandler(com.agendamentos.equadras.shared.pagination.InvalidSortException.class)
    public ProblemDetail handleInvalidSort(com.agendamentos.equadras.shared.pagination.InvalidSortException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Parâmetro de ordenação não permitido.");
        problemDetail.setTitle("Ordenação Inválida");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/bad-request"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "PARAMETRO_INVALIDO");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata método HTTP não suportado (ex: GET em rota de POST)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String detalhe = String.format("O método HTTP %s não é permitido para este endpoint.", ex.getMethod());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, detalhe);
        problemDetail.setTitle("Método Não Permitido");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/metodo-nao-permitido"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "METODO_NAO_PERMITIDO");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata rotas ou recursos não encontrados (404)
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "O endereço ou recurso solicitado não foi encontrado no servidor."
        );
        problemDetail.setTitle("Recurso Não Encontrado");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/nao-encontrado"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "NAO_ENCONTRADO");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata upload com arquivo maior que o limite
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "O arquivo enviado excede o tamanho máximo permitido pelo sistema."
        );
        problemDetail.setTitle("Arquivo Muito Grande");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/tamanho-arquivo-excedido"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "ARQUIVO_MUITO_GRANDE");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Trata Content-Type não suportado
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "O tipo de conteúdo enviado (Content-Type) não é suportado por este endpoint."
        );
        problemDetail.setTitle("Tipo de Mídia Não Suportado");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/tipo-nao-suportado"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "TIPO_MIDIA_NAO_SUPORTADO");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    // Handler global para exceções inesperadas
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Exceção não tratada na requisição [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado no servidor. Por favor, tente novamente em instantes."
        );
        problemDetail.setTitle("Erro Interno do Servidor");
        problemDetail.setType(URI.create("https://api.equadras.com/erros/internal-server-error"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", "ERRO_INTERNO");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

    private String traduzirMensagemNegocio(String mensagemOriginal) {
        if (mensagemOriginal == null || mensagemOriginal.isBlank()) {
            return "Requisição inválida ou dados incorretos.";
        }
        String lower = mensagemOriginal.toLowerCase();
        if (lower.contains("bad credentials") || lower.contains("invalid credentials")) {
            return "E-mail ou senha incorretos. Verifique suas credenciais e tente novamente.";
        }
        if (lower.contains("user not found") || lower.contains("usuário não encontrado")) {
            return "Usuário não encontrado no sistema.";
        }
        if (lower.contains("court not found") || lower.contains("quadra não encontrada")) {
            return "A quadra informada não foi encontrada.";
        }
        if (lower.contains("booking not found") || lower.contains("agendamento não encontrado")) {
            return "O agendamento informado não foi localizado.";
        }
        if (lower.contains("access denied") || lower.contains("permissão insuficiente")) {
            return "Você não tem permissão para realizar esta operação.";
        }
        return mensagemOriginal;
    }

    private String traduzirNomeCampo(String campo) {
        if (campo == null) return "Campo";
        return switch (campo) {
            case "email_usuario", "email" -> "E-mail";
            case "senha_usuario", "senha", "password" -> "Senha";
            case "nome_usuario", "nome", "name" -> "Nome";
            case "phone_usuario", "telefone", "phone" -> "Telefone";
            case "dataHoraInicio", "inicio" -> "Horário de início";
            case "dataHoraFim", "fim" -> "Horário de término";
            case "id_quadra", "quadraId" -> "Quadra";
            case "valorTotal", "preco" -> "Valor total";
            case "role" -> "Tipo de perfil";
            default -> campo;
        };
    }
}
