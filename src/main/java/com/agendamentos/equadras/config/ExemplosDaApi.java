package com.agendamentos.equadras.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro central dos exemplos e erros documentados no OpenAPI. Chave: "MÉTODO /path"
 * (o springdoc emite uma única operação por path+método). Cada domínio contribui com um
 * mapa; chave duplicada é erro de programação e falha na inicialização.
 */
final class ExemplosDaApi {

    static final String JSON = "application/json";
    static final String PROBLEM_JSON = "application/problem+json";
    static final String SSE = "text/event-stream";

    private static final Map<String, ExemploOperacao> REGISTRO = montar();

    private ExemplosDaApi() {
    }

    static String chave(String metodo, String path) {
        return metodo.toUpperCase() + " " + path;
    }

    static ExemploOperacao buscar(String metodo, String path) {
        return REGISTRO.get(chave(metodo, path));
    }

    static Map<String, ExemploOperacao> todos() {
        return REGISTRO;
    }

    static Builder operacao() {
        return new Builder();
    }

    private static Map<String, ExemploOperacao> montar() {
        Map<String, ExemploOperacao> todos = new LinkedHashMap<>();
        List<Map<String, ExemploOperacao>> partes = List.of(
                ExemplosUsuarios.exemplos(),
                ExemplosQuadras.exemplos(),
                ExemplosBloqueios.exemplos(),
                ExemplosAgendamentos.exemplos(),
                ExemplosPagamentos.exemplos(),
                ExemplosNotificacoes.exemplos(),
                ExemplosAuditoria.exemplos());
        for (Map<String, ExemploOperacao> parte : partes) {
            parte.forEach((chave, exemplo) -> {
                if (todos.put(chave, exemplo) != null) {
                    throw new IllegalStateException("Exemplo OpenAPI duplicado no registro: " + chave);
                }
            });
        }
        return Map.copyOf(todos);
    }

    /**
     * Uma resposta de sucesso. {@code schema}: null (mantém o do springdoc), "Nome" (referência) ou
     * "array:Nome" (lista de referência). {@code conteudo} null = resposta sem corpo.
     */
    record RespostaExemplo(String status, String nome, String descricao, String conteudo, String mediaType,
                           String schema, Map<String, String> cabecalhos) {
    }

    enum Formato { HANDLER, VALIDACAO, FILTRO, SIMPLES }

    record ErroExemplo(int status, Formato formato, String slug, String titulo, String code, String detalhe,
                       Map<String, String> cabecalhos) {
    }

    record ExemploOperacao(String requestJson, List<RespostaExemplo> respostas, List<ErroExemplo> erros) {
    }

    static final class Builder {
        private String request;
        private final List<RespostaExemplo> respostas = new ArrayList<>();
        private final List<ErroExemplo> erros = new ArrayList<>();

        Builder request(String json) {
            this.request = json;
            return this;
        }

        Builder ok(String status, String descricao, String json) {
            respostas.add(new RespostaExemplo(status, null, descricao, json, JSON, null, Map.of()));
            return this;
        }

        Builder okComCabecalhos(String status, String descricao, String json, Map<String, String> cabecalhos) {
            respostas.add(new RespostaExemplo(status, null, descricao, json, JSON, null, cabecalhos));
            return this;
        }

        /** Vários exemplos nomeados para o mesmo status; {@code schema} pode ser null. */
        Builder okNomeado(String status, String nome, String descricao, String json, String schema) {
            respostas.add(new RespostaExemplo(status, nome, descricao, json, JSON, schema, Map.of()));
            return this;
        }

        Builder semCorpo(String status, String descricao) {
            respostas.add(new RespostaExemplo(status, null, descricao, null, JSON, null, Map.of()));
            return this;
        }

        Builder semCorpoComCabecalhos(String status, String descricao, Map<String, String> cabecalhos) {
            respostas.add(new RespostaExemplo(status, null, descricao, null, JSON, null, cabecalhos));
            return this;
        }

        Builder stream(String status, String descricao, String texto) {
            respostas.add(new RespostaExemplo(status, null, descricao, texto, SSE, null, Map.of()));
            return this;
        }

        Builder erro(ErroExemplo erro) {
            erros.add(erro);
            return this;
        }

        ExemploOperacao build() {
            return new ExemploOperacao(request, List.copyOf(respostas), List.copyOf(erros));
        }
    }

    /** Fábrica de erros: cada método espelha um caminho real do GlobalExceptionHandler ou dos filtros. */
    static final class Erros {
        private Erros() {
        }

        /** IllegalArgumentException (ex.: "Quadra não encontrada", conflito de bloqueio). */
        static ErroExemplo requisicaoInvalida(String detalhe) {
            return new ErroExemplo(400, Formato.HANDLER, "bad-request", "Requisição Inválida", "REQUISICAO_INVALIDA", detalhe, Map.of());
        }

        /** IllegalStateException (ex.: excluir quadra com histórico). */
        static ErroExemplo operacaoNaoPermitida(String detalhe) {
            return new ErroExemplo(400, Formato.HANDLER, "operacao-invalida", "Operação Não Permitida", "OPERACAO_NAO_PERMITIDA", detalhe, Map.of());
        }

        /** RegraNegocioException com código fora de CODIGOS_CONFLITO. */
        static ErroExemplo regraNegocio(String code, String detalhe) {
            return new ErroExemplo(400, Formato.HANDLER, "regra-negocio-violada", "Regra de Negócio Violada", code, detalhe, Map.of());
        }

        /** RegraNegocioException com CONFLITO_STATUS, TELEFONE_EM_USO ou CONFLITO_VERSAO. */
        static ErroExemplo conflito(String code, String detalhe) {
            return new ErroExemplo(409, Formato.HANDLER, "regra-negocio-violada", "Regra de Negócio Violada", code, detalhe, Map.of());
        }

        /** DataIntegrityViolationException SQLSTATE 23P01 (exclusão de intervalo de horário). */
        static ErroExemplo horarioIndisponivel() {
            return new ErroExemplo(409, Formato.HANDLER, "horario-indisponivel", "Conflito de Horário", "HORARIO_INDISPONIVEL",
                    "O horário selecionado conflita com outro agendamento já existente ou bloqueado para esta quadra.", Map.of());
        }

        /** RecursoNaoEncontradoException. */
        static ErroExemplo naoEncontrado(String code, String detalhe) {
            return new ErroExemplo(404, Formato.HANDLER, "recurso-nao-encontrado", "Recurso Não Encontrado", code, detalhe, Map.of());
        }

        /** AccessDeniedException lançada em service (dono da quadra, Master Admin, etc.). */
        static ErroExemplo acessoNegado(String detalhe) {
            return new ErroExemplo(403, Formato.HANDLER, "acesso-negado", "Acesso Negado", "ACESSO_NEGADO", detalhe, Map.of());
        }

        /** 403 do filtro de segurança (papel insuficiente na URL): JsonAccessDeniedHandler. */
        static ErroExemplo papelInsuficiente() {
            return new ErroExemplo(403, Formato.FILTRO, null, "Acesso Proibido",
                    null, "Acesso proibido: sua credencial não possui permissão para executar esta operação.", Map.of());
        }

        /** 401 do filtro de segurança: JsonAuthenticationEntryPoint. */
        static ErroExemplo naoAutenticado() {
            return new ErroExemplo(401, Formato.FILTRO, null, "Não Autorizado",
                    null, "Acesso não autorizado: credencial ausente, expirada ou inválida.", Map.of());
        }

        /** MethodArgumentNotValidException: detalhe no formato "Dados inválidos: Campo: mensagem". */
        static ErroExemplo validacao(String detalhe) {
            return new ErroExemplo(422, Formato.VALIDACAO, "validacao", "Dados Inválidos", "ERRO_VALIDACAO", detalhe, Map.of());
        }

        /** HttpMessageNotReadableException (JSON malformado ou campo desconhecido: fail-on-unknown-properties=true). */
        static ErroExemplo corpoIlegivel() {
            return new ErroExemplo(400, Formato.HANDLER, "dados-invalidos", "Formato de Dados Inválido", "DADOS_INVALIDOS",
                    "O formato dos dados enviados está incorreto ou possui campos inválidos. Verifique as informações fornecidas.", Map.of());
        }

        /** MissingServletRequestParameterException. */
        static ErroExemplo parametroAusente(String detalhe) {
            return new ErroExemplo(400, Formato.HANDLER, "parametro-ausente", "Parâmetro Ausente", "PARAMETRO_AUSENTE", detalhe, Map.of());
        }

        /** MethodArgumentTypeMismatchException. */
        static ErroExemplo parametroInvalido(String detalhe) {
            return new ErroExemplo(400, Formato.HANDLER, "parametro-invalido", "Parâmetro Inválido", "PARAMETRO_INVALIDO", detalhe, Map.of());
        }

        /** MaxUploadSizeExceededException. */
        static ErroExemplo arquivoGrande(String detalhe) {
            return new ErroExemplo(413, Formato.HANDLER, "tamanho-arquivo-excedido", "Arquivo Muito Grande", "ARQUIVO_MUITO_GRANDE", detalhe, Map.of());
        }

        /** UsuarioController.login: ProblemDetail.forStatusAndDetail + Retry-After. */
        static ErroExemplo muitasTentativasLogin(String detalhe) {
            return new ErroExemplo(429, Formato.SIMPLES, null, "Too Many Requests", null, detalhe,
                    Map.of("Retry-After", "Segundos até nova tentativa de login para este e-mail."));
        }

        /** UsuarioController.regenerarApiKey: Map {status, title, detail} + Retry-After. */
        static ErroExemplo muitasRegeneracoesApiKey() {
            return new ErroExemplo(429, Formato.FILTRO, null, "Too Many Requests", null,
                    "Limite de 5 regenerações por minuto atingido para esta conta. Aguarde antes de tentar novamente.",
                    Map.of("Retry-After", "Sempre 60 (segundos)."));
        }
    }
}
