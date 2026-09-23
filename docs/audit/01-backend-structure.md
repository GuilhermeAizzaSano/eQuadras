# A1 — Estrutura e Camadas do Backend

## Escopo coberto
- Estrutura de pacotes e convenções arquiteturais em `src/main/java/com/agendamentos/equadras/` (67 classes Java).
- Camada de apresentação e controllers HTTP em `controller/` (7 controllers REST).
- Camada de serviço, regras transacionais e orquestração em `service/` (9 serviços).
- Camada de persistência e entidades JPA em `model/entity/` (8 entidades) e `repository/` (7 repositórios).
- Camada de transferência de dados em `dto/request/` (10 DTOs) e `dto/response/` (12 DTOs).
- Tratamento global de erros e exceções em `exception/` (ProblemDetail RFC 7807).
- Arquivos de configuração, profiles e propriedades em `src/main/resources/` (`application.properties`, `application-perf.properties`) e `config/`.
- Documentação pública OpenAPI/Swagger em `config/OpenApiConfig.java` e `pom.xml`.
- Classes utilitárias e segurança em `util/` e `security/`.

---

## Itens do checklist (com status: ✅, ⚠️, ❌, ➖, ❓)

| Item | Status | Justificativa Sintética |
|---|:---:|---|
| Organização por feature ou por camada? Consistente em todo o código? | ⚠️ | Arquitetura 100% organizada por camada (*package-by-layer*). Consistente no projeto todo, mas gera alto acoplamento horizontal e visibilidade pública irrestrita entre classes. |
| Direção das dependências: controller → service → repository, sem ciclos e sem controller acessando repository diretamente | ⚠️ | Controllers não acessam repositórios diretamente, mas serviços cruzam fronteiras de agregados injetando múltiplos repositórios de outros domínios diretamente. `AdminMasterInitializer` (em config) acessa repositório diretamente. |
| Controllers finos: sem regra de negócio, sem @Transactional, sem manipulação direta de entidade | ❌ | Controllers não usam `@Transactional`, porém executam regras de negócio, parsing de webhooks, fallbacks heurísticos de parâmetros, controle manual de cookies e checagens de autorização. |
| Entidades JPA nunca expostas na API: DTOs de entrada e saída separados, mapeamento explícito (MapStruct ou manual) | ❌ | A entidade `Notificacao` é exposta diretamente no endpoint `GET /notificacoes/admin`. `DisponibilidadeDiaDTO` de entrada é reutilizado em DTO de saída. Mapeamento manual com acoplamento reverso nos DTOs (`fromEntity`). |
| Modelo de domínio: regras nas entidades ou serviços "deus" com toda a lógica? Duplicação de regra entre serviços | ❌ | Modelo de domínio anêmico (*Anemic Domain*). Entidades sem comportamento e serviços "Deus" (`AgendamentoService` com 659 linhas, `QuadraService` com 464 linhas). Regras de autorização e checagem de admin duplicadas entre 4 serviços. Estado mutável estático em entidade (`Usuario.MASTER_EMAIL_CONFIGURADO`). |
| Validação: Bean Validation nos DTOs de entrada (@Valid) e regras de negócio no service | ⚠️ | A maioria dos DTOs usa Bean Validation e controllers aplicam `@Valid`. Porém, `DisponibilidadeDiaDTO` não possui nenhuma validação interna em seus campos, e `AgendamentoCriacaoDTO` aceita um `usuarioId` que é completamente ignorado. |
| Tratamento de erro central (@RestControllerAdvice) com formato único (ProblemDetail / RFC 7807) e sem stack trace na resposta | ⚠️ | `GlobalExceptionHandler` adota RFC 7807 e não vaza stack traces. Todavia, a ausência de exceções de domínio customizadas faz com que entidades não encontradas lancem `IllegalArgumentException`, gerando HTTP 400 Bad Request em vez de HTTP 404 Not Found. |
| Configuração: profiles (dev/prod), @ConfigurationProperties tipado, nada hardcoded, segredos via env | ❌ | Não existem profiles segregados (`application-dev`, `application-prod`). Zero classes com `@ConfigurationProperties`. Parâmetros dispersos via `@Value` com valores conflitantes. Segredos e senhas com fallbacks triviais hardcoded. |
| Contrato público documentado (springdoc) e versionamento de API definido | ⚠️ | Documentação rica com springdoc 2.8.5, mas implementada através de um arquivo de configuração de 922 linhas (`OpenApiConfig.java`) com JSON mockado programaticamente. Versionamento formal de API inexistente. Endpoints com retornos polimórficos (`ResponseEntity<?>`). |
| Código morto, classes utilitárias genéricas demais (Utils, Helper) e acoplamento a detalhes de framework no domínio | ❌ | Anotação `@DevOnly` declarada e nunca utilizada. Nomes enganosos (`CorsConfig` não configura CORS). `AuditoriaService` acoplado a Servlets HTTP via `HttpRequestUtil`. `NotificacaoService` acoplado a `SseEmitter` do Spring MVC. Entidade `Notificacao` acoplada ao Jackson (`@JsonIgnore`). |

---

## Achados

### [A1-01] [ALTA] Entidade JPA exposta diretamente na API e acoplamento a framework de serialização no domínio
- **Domínio:** A1
- **Tipo:** Fato
- **Local/Evidência:** `src/main/java/com/agendamentos/equadras/controller/NotificacaoController.java:37` e `src/main/java/com/agendamentos/equadras/model/entity/Notificacao.java:16`
- **Problema:** O método `listarPorAdmin` em `NotificacaoController` retorna `ResponseEntity<Page<Notificacao>>`, devolvendo instâncias diretas da entidade JPA `Notificacao`. Para contornar a serialização da relação `@ManyToOne Usuario admin` ou evitar lazy loading exceptions, foi adicionada a anotação `@com.fasterxml.jackson.annotation.JsonIgnore` no atributo `admin` dentro da entidade JPA de domínio.
- **Impacto:** Viola a barreira entre a camada de persistência e a camada de transporte HTTP. Qualquer alteração ou nova coluna no schema do banco vaza automaticamente para a API pública, além de poluir a entidade de domínio com anotações de serialização Jackson.
- **Recomendação:** Criar `NotificacaoResponseDTO(Long id, String mensagem, boolean lida, LocalDateTime dataCriacao)`, mapear a lista na camada de serviço ou via factory method, alterar o retorno do controller para `ResponseEntity<Page<NotificacaoResponseDTO>>` e remover `@JsonIgnore` da entidade `Notificacao`.
- **Esforço:** P
- **Relacionados:** (vazio)

---

### [A1-02] [ALTA] Mapeamento genérico de exceções de busca causa retorno de HTTP 400 em vez de HTTP 404
- **Domínio:** A1
- **Tipo:** Fato
- **Local/Evidência:** `src/main/java/com/agendamentos/equadras/exception/GlobalExceptionHandler.java:32-41` e `src/main/java/com/agendamentos/equadras/service/QuadraService.java:112`
- **Problema:** Não existe hierarquia de exceções de negócio/domínio no backend. Em todas as operações de busca por ID inexistente (em `QuadraService`, `AgendamentoService`, `UsuarioService` e `BloqueioHorarioService`), o código lança `IllegalArgumentException` (ex: `orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + id))`). O `GlobalExceptionHandler` intercepta `IllegalArgumentException` e retorna estritamente `HttpStatus.BAD_REQUEST` (HTTP 400) com o título "Requisição Inválida".
- **Impacto:** Violação semântica do protocolo HTTP e padrão REST. Quando um cliente ou o frontend busca um ID inexistente (`/quadras/999` ou `/agendamentos/999`), recebe 400 Bad Request em vez de 404 Not Found. Isso quebra comportamentos padrão de navegadores, bibliotecas de cache (React Query/SWR) e ferramentas de monitoramento.
- **Recomendação:** Criar uma exceção de domínio dedicada (ex: `ResourceNotFoundException` ou `EntidadeNaoEncontradaException`), criar um `@ExceptionHandler` correspondente no `GlobalExceptionHandler` retornando `HttpStatus.NOT_FOUND` (404) com ProblemDetail RFC 7807, e substituir os `orElseThrow` de busca nos serviços.
- **Esforço:** M
- **Relacionados:** (vazio)

---

### [A1-03] [ALTA] Controllers executando orquestração de negócio e parsing manual de payloads complexos
- **Domínio:** A1
- **Tipo:** Fato
- **Local/Evidência:** `src/main/java/com/agendamentos/equadras/controller/PagamentoController.java:48-62`, `src/main/java/com/agendamentos/equadras/controller/PagamentoController.java:69-141` e `src/main/java/com/agendamentos/equadras/controller/QuadraController.java:145-187`
- **Problema:** Controllers assumem responsabilidades de orquestração de negócio e parsing procedural que deveriam pertencer à camada de serviço:
  1. `PagamentoController.webhook` (linhas 69-141): possui 73 linhas dedicadas a varrer query parameters (`paramId`, `data.id`), navegar em nós de `Map<String, Object>`, converter strings em IDs numéricos, invocar a API externa do Mercado Pago, capturar exceções com múltiplos blocos try/catch e chamar conciliação de agendamentos.
  2. `PagamentoController.consultarStatus` (linhas 48-62): avalia se o agendamento está em estado `PENDENTE`, se possui identificador de transação, consulta o gateway externo e confirma o pagamento por webhook no controller.
  3. `QuadraController.consultarFotos` (linhas 145-187): recebe entidades brutas de `quadraService.filtrarQuadrasEntidades(...)`, calcula se há 1 ou mais quadras e formata `LinkedHashMap` manualmente.
- **Impacto:** Controllers gordos e difíceis de manter, impossibilidade de testar regras de conciliação de pagamento isoladamente sem instanciar mocks HTTP/Web, e violação do princípio de responsabilidade única (SRP).
- **Recomendação:** Encapsular toda a lógica de parsing e conciliação em métodos de serviço dedicados (ex: `PagamentoService.processarNotificacaoWebhook(payload, params)`), deixando os métodos dos controllers com apenas 2 a 5 linhas delegando para os serviços.
- **Esforço:** M
- **Relacionados:** (vazio)

---

### [A1-04] [ALTA] Fallbacks com segredos e credenciais padrão hardcoded em arquivos de configuração
- **Domínio:** A1
- **Tipo:** Fato
- **Local/Evidência:** `src/main/resources/application.properties:3, 42, 64, 69`
- **Problema:** O arquivo principal de configuração define valores padrão (*fallbacks*) inseguros e previsíveis caso as variáveis de ambiente não sejam injetadas:
  - Linha 3: `spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:***}`
  - Linha 42: `jwt.secret=${JWT_SECRET:***}`
  - Linha 64: `admin.master.email=${ADMIN_MASTER_EMAIL:***}`
  - Linha 69: `equadras.bot.api-secret=${BOT_API_SECRET:***}`
- **Impacto:** Se o processo da aplicação em produção for iniciado sem definir explicitamente as variáveis de ambiente (por falha de configuração de systemd, Docker ou deploy), a aplicação sobe silenciosamente vulnerável, permitindo forja de tokens JWT com a chave pública do repositório, acesso administrativo total via credenciais padrão e uso não autorizado da API do bot.
- **Recomendação:** Segregar propriedades de desenvolvimento e produção em profiles explícitos. Em produção, remover quaisquer fallbacks de credenciais e segredos; se as variáveis não existirem, a aplicação deve falhar no startup (*fail-fast*).
- **Esforço:** P
- **Relacionados:** (vazio)

---

### [A1-05] [ALTA] Dispersão de @Value sem validação tipada e com valores padrão conflitantes entre código e properties
- **Domínio:** A1
- **Tipo:** Fato
- **Local/Evidência:** `src/main/java/com/agendamentos/equadras/security/JwtService.java:24-25` e `src/main/resources/application.properties:43`
- **Problema:** Não existe nenhuma classe anotada com `@ConfigurationProperties` no projeto. Toda a configuração é injetada pontualmente via `@Value` espalhado em controllers, serviços e classes de configuração. Isso provocou divergências reais no código:
  1. No `application.properties:43`, o tempo de expiração do JWT é definido como `jwt.expiracao-ms=${JWT_EXPIRACAO_MS:1800000}` (30 minutos). Já no construtor de `JwtService.java:25`, a anotação define `@Value("${jwt.expiracao-ms:28800000}")` (8 horas como fallback).
  2. Em `SecurityConfig.java:31`, as origens CORS permitidas incluem `https://equadras.app` no default do `@Value`, mas em `application.properties:46` a propriedade `equadras.cors.origens-permitidas` omite as URLs de produção.
- **Impacto:** Dificuldade em rastrear todas as configurações disponíveis no sistema, ausência de validação estática de tipos no boot da JVM e comportamento imprevisível dependendo de como as classes são instanciadas em testes unitários versus runtime.
- **Recomendação:** Centralizar as propriedades da aplicação em classes tipadas anotadas com `@ConfigurationProperties(prefix = "equadras")` (ex: `JwtProperties`, `CorsProperties`, `BotProperties`, `MasterAdminProperties`), com suporte a validação via Jakarta Validation.
- **Esforço:** M
- **Relacionados:** (vazio)

---

### [A1-06] [ALTA] Bypass de autenticação da integração do Bot por validação manual no controller
- **Domínio:** A1
- **Tipo:** Fato
- **Local/Evidência:** `src/main/java/com/agendamentos/equadras/controller/AgendamentoController.java:108-112`
- **Problema:** A autenticação das requisições externas do bot WhatsApp é realizada manualmente dentro do método `agendarViaBot`:
  ```java
  if (botApiSecret != null && !botApiSecret.isBlank() && !botApiSecret.equals(botSecret)) {
      throw new AccessDeniedException("Acesso não autorizado para integração bot.");
  }
  ```
  Se a variável `equadras.bot.api-secret` estiver vazia ou nula no ambiente, `botApiSecret.isBlank()` será verdadeiro, o `if` não será acionado e **qualquer requisição externa sem cabeçalho `X-Bot-Secret` será autorizada** a criar agendamentos no sistema.
- **Impacto:** Brecha de autenticação crítica condicionada à configuração, além de desvio arquitetural por realizar autorização de infraestrutura/segurança imperativamente dentro do método de negócio do controller.
- **Recomendação:** Extrair a proteção do endpoint `/agendamentos/bot` para um filtro do Spring Security (`BotAuthenticationFilter` ou `SecurityFilterChain`), tratando o secret como credencial estrita: se não configurado ou se o cabeçalho não bater, responder HTTP 401 Unauthorized imediatamente.
- **Esforço:** P
- **Relacionados:** (vazio)

---

### [A1-07] [MÉDIA] Endpoints públicos com retorno polimórfico não tipado (ResponseEntity<?>)
- **Domínio:** A1
- **Tipo:** Fato
- **Local/Evidência:** `src/main/java/com/agendamentos/equadras/controller/QuadraController.java:48` e `src/main/java/com/agendamentos/equadras/controller/QuadraController.java:145`
- **Problema:** O método `QuadraController.listarTodas` possui a assinatura `public ResponseEntity<?> listarTodas(...)` e retorna três tipos diferentes de payload dependendo de cabeçalhos (`X-Client`, `X-View`), do parâmetro `resumido`, de parâmetros de paginação (`page`, `size`) e de inspeção da URI da requisição (`uri.contains("/api/")`):
  1. `List<QuadraResumoResponseDTO>` (quando chamado via `/api/quadras` ou `resumido=true`);
  2. `Page<QuadraResponseDTO>` (quando informado `page`);
  3. `List<QuadraResponseDTO>` (quando chamado via `/quadras` sem `page`).
  Adicionalmente, `consultarFotos` retorna `ResponseEntity<?>`, devolvendo ora um `Map<String, Object>`, ora uma `List<Map<String, Object>>`.
- **Impacto:** Viola o princípio de previsibilidade de APIs REST. Torna a documentação OpenAPI dependente de interceptadores imperativos complexos e obriga os clientes (frontend, bots) a implementarem guardas de tipagem em tempo de execução para verificar o formato do JSON recebido.
- **Recomendação:** Separar em rotas explícitas com contratos estritos:
  - `GET /quadras`: retorna sempre `Page<QuadraResponseDTO>`;
  - `GET /quadras/resumo`: retorna sempre `List<QuadraResumoResponseDTO>`;
  - Eliminar o uso de `ResponseEntity<?>` em favor de DTOs fortemente tipados.
- **Esforço:** M
- **Relacionados:** (vazio)

---

### [A1-08] [MÉDIA] Modelo de domínio anêmico com serviços concentrando regras procedurais e duplicação de lógica
- **Domínio:** A1
- **Tipo:** Fato
- **Local/Evidência:** `src/main/java/com/agendamentos/equadras/service/QuadraService.java:101-105` e `src/main/java/com/agendamentos/equadras/service/BloqueioHorarioService.java:43-49`
- **Problema:** As entidades JPA (`Usuario`, `Quadra`, `Agendamento`, `BloqueioHorario`) funcionam como meros repositórios de dados (*Anemic Domain Model*), sem encapsulamento comportamental. Toda a lógica de transição de status, cálculo de valores e checagem de propriedade foi colocada na camada de serviço. Isso levou a duplicações diretas de código:
  - A checagem de permissão administrativa sobre uma quadra (`podeGerenciarQuadra` e `podeGerenciarBloqueio`) foi escrita identicamente tanto em `QuadraService` quanto em `BloqueioHorarioService`:
    ```java
    if (admin.isMasterAdmin()) return true;
    return quadra.getAdmin() != null && quadra.getAdmin().getId_usuario().equals(adminId);
    ```
  - Regra similar de titularidade é repetida manualmente nos métodos `confirmarPagamento`, `cancelar` e `listarPorQuadra` de `AgendamentoService`.
  - Inconsistência nos tipos de erro lançados: `QuadraService` lança ora `AccessDeniedException` (linha 115) ora `IllegalArgumentException` (linha 452) para a mesma violação de permissão.
- **Impacto:** Código procedural extenso (`AgendamentoService` possui 659 linhas), risco de divergência de regras caso uma validação seja alterada em um serviço e mantida obsoleta em outro, e baixa testabilidade unitária.
- **Recomendação:** Transferir regras inerentes às entidades para seus próprios métodos (ex: `quadra.pertenceAoAdmin(adminId)`, `agendamento.cancelar(executor)`), ou extrair um validador/serviço compartilhado de autorização de domínio (`QuadraSecurityValidator`).
- **Esforço:** M
- **Relacionados:** (vazio)

---

### [A1-09] [MÉDIA] Estado mutável global e acoplamento estático dentro da entidade JPA Usuario
- **Domínio:** A1
- **Tipo:** Fato
- **Local/Evidência:** `src/main/java/com/agendamentos/equadras/model/entity/Usuario.java:110-128` e `src/main/java/com/agendamentos/equadras/service/UsuarioService.java:45`
- **Problema:** A entidade JPA `Usuario` define o campo estático:
  ```java
  public static volatile String MASTER_EMAIL_CONFIGURADO = "gui@gmail.com";
  ```
  No construtor de `UsuarioService.java:45`, esse valor estático é sobrescrito via Spring:
  ```java
  Usuario.MASTER_EMAIL_CONFIGURADO = this.masterAdminEmail;
  ```
  E a própria entidade consulta essa variável estática no método `isMasterAdmin()`.
- **Impacto:** Conecta o ciclo de vida do framework Spring (criação de beans) com a definição estática da classe de entidade JPA. Quebra o isolamento de testes (testes paralelos modificando estado estático compartilhado), viola os princípios de Clean Architecture e mantém e-mail pessoal de desenvolvedor hardcoded no domínio.
- **Recomendação:** Remover o campo e métodos estáticos de `Usuario`. A verificação de Master Admin deve ser realizada por um serviço Spring dedicado (`MasterAdminService` ou `SecurityUtils`) injetado nos componentes que necessitam dessa validação.
- **Esforço:** P
- **Relacionados:** (vazio)

---

### [A1-10] [MÉDIA] Acoplamento indevido do serviço de auditoria com a camada de transporte Servlet (HttpRequestUtil)
- **Domínio:** A1
- **Tipo:** Fato
- **Local/Evidência:** `src/main/java/com/agendamentos/equadras/service/AuditoriaService.java:50-79` e `src/main/java/com/agendamentos/equadras/util/HttpRequestUtil.java:12-18`
- **Problema:** Em seus métodos de registro de auditoria, `AuditoriaService` invoca chamadas estáticas a `HttpRequestUtil.extrairClientIp(null)` e `HttpRequestUtil.extrairUserAgent(null)`. A classe utilitária, por sua vez, obtém a requisição através de `RequestContextHolder.getRequestAttributes()`.
- **Impacto:** Caso a auditoria seja disparada a partir de jobs agendados (`@Scheduled`), processamentos assíncronos (`@Async`), virtual threads sem propagação de contexto ou listeners em segundo plano, `RequestContextHolder` retorna `null`, gerando dados inconsistentes ("127.0.0.1", "desconhecido"). Ademais, acopla a camada de serviço ao servlet container HTTP.
- **Recomendação:** Os metadados de requisição (IP, User-Agent, Correlation-ID) devem ser capturados na camada web (Filtro HTTP ou Controller) e passados explicitamente como parâmetros de DTO ou via um `AuditContext` desacoplado de Servlets.
- **Esforço:** P
- **Relacionados:** (vazio)

---

### [A1-11] [MÉDIA] Configuração manual de 922 linhas de OpenAPI e inexistência de versionamento de API
- **Domínio:** A1
- **Tipo:** Fato
- **Local/Evidência:** `src/main/java/com/agendamentos/equadras/config/OpenApiConfig.java:152-800`
- **Problema:** A documentação da API em `OpenApiConfig.java` possui 922 linhas porque programa imperativamente centenas de strings JSON mockadas dentro de um switch-case gigante para cada endpoint (`enrichOperationExamples`), além de remover rotas via manipulação direta de `openApi.getPaths()`. Adicionalmente, **não existe qualquer mecanismo de versionamento de API** (as rotas misturam `/quadras` com `/api/quadras` sem prefixos de versão como `/api/v1/quadras`).
- **Impacto:** Manutenção extremamente onerosa: qualquer modificação em um DTO ou regra de negócio exige alteração manual em dezenas de linhas de strings JSON no `OpenApiConfig`. A falta de versionamento formal impede a evolução da API sem quebrar clientes existentes.
- **Recomendação:** 
  1. Utilizar as anotações declarativas do springdoc nos próprios controllers e DTOs (`@Operation`, `@ApiResponse`, `@Schema(example = ...)`), eliminando o switch procedural de 700 linhas em `OpenApiConfig`.
  2. Padronizar o prefixo de rotas com versionamento semântico explícito (ex: `/api/v1/...`).
- **Esforço:** M
- **Relacionados:** (vazio)

---

### [A1-12] [BAIXA] Acoplamento da gestão de conexões SSE (SseEmitter) dentro do serviço de domínio NotificacaoService
- **Domínio:** A1
- **Tipo:** Fato
- **Local/Evidência:** `src/main/java/com/agendamentos/equadras/service/NotificacaoService.java:27-54`
- **Problema:** `NotificacaoService` armazena e gerencia diretamente instâncias de `SseEmitter` do Spring MVC em um mapa em memória (`private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>()`).
- **Impacto:** A camada de serviço, responsável pela regra transacional de negócio (persistência de notificações no banco), fica acoplada à tecnologia de transporte HTTP SSE. Caso o sistema adote WebSockets, push móvel ou mensageria externa, o serviço precisa ser reescrito.
- **Recomendação:** Isolar a gestão das conexões SSE em um componente de infraestrutura web (ex: `SseConnectionRegistry` no pacote `config` ou `controller`), emitindo notificações via eventos do Spring (`ApplicationEventPublisher`) desacoplados do protocolo HTTP.
- **Esforço:** P
- **Relacionados:** (vazio)

---

### [A1-13] [BAIXA] Código morto, convenções confusas de nomenclatura e atributos órfãos em DTOs
- **Domínio:** A1
- **Tipo:** Fato
- **Local/Evidência:** `src/main/java/com/agendamentos/equadras/config/DevOnly.java:13`, `src/main/java/com/agendamentos/equadras/config/CorsConfig.java:14` e `src/main/java/com/agendamentos/equadras/dto/request/AgendamentoCriacaoDTO.java:11`
- **Problema:**
  1. `DevOnly.java`: anotação customizada declarada em `config` e nunca utilizada em nenhuma classe do projeto ou testes.
  2. `CorsConfig.java`: implementa `WebMvcConfigurer`, registra manipuladores de upload de fotos e argument resolvers de autenticação, mas **não configura CORS** (a configuração real de CORS está em `SecurityConfig.java`). O nome é enganoso.
  3. `AgendamentoCriacaoDTO.java:11`: declara o campo `Long usuarioId` com anotação `@Schema(description = "ID do usuário...")`, porém esse campo nunca é lido nem utilizado em nenhum serviço do backend (o ID do usuário é obtido estritamente do token de sessão autenticado).
- **Impacto:** Poluição do código, confusão para manutenções futuras e risco de induzir clientes da API a passarem IDs de terceiros acreditando que surtirá efeito.
- **Recomendação:** Excluir `@DevOnly`, renomear `CorsConfig` para `WebMvcConfig`, e remover o atributo `usuarioId` de `AgendamentoCriacaoDTO`.
- **Esforço:** P
- **Relacionados:** (vazio)

---

### [A1-14] [BAIXA] Reutilização de DTO de entrada em modelo de saída e ausência de Bean Validation em DTO aninhado
- **Domínio:** A1
- **Tipo:** Fato
- **Local/Evidência:** `src/main/java/com/agendamentos/equadras/dto/request/DisponibilidadeDiaDTO.java:6-10` e `src/main/java/com/agendamentos/equadras/dto/response/QuadraResponseDTO.java:3, 59`
- **Problema:** O record `DisponibilidadeDiaDTO` reside no pacote `dto.request`, mas é importado e utilizado internamente em `QuadraResponseDTO` no pacote `dto.response`. Além disso, embora seja referenciado como `@Valid DisponibilidadeDiaDTO` em `QuadraCriacaoDTO:49`, o próprio `DisponibilidadeDiaDTO` não possui nenhuma restrição Bean Validation (`@NotNull`) em seus atributos (`diaSemana`, `horaInicio`, `horaFim`).
- **Impacto:** Permite o envio de disponibilidades com valores nulos que passam silenciosamente pela validação do Spring, e cria acoplamento indevido entre a estrutura de dados de requisição e a de resposta.
- **Recomendação:** Adicionar validações Bean Validation nos campos de `DisponibilidadeDiaDTO` e separar em DTOs específicos de entrada e saída.
- **Esforço:** P
- **Relacionados:** (vazio)

---

### [A1-15] [BAIXA] Diretório de upload relativo fixo e saída de log em console de erro padrão
- **Domínio:** A1
- **Tipo:** Fato
- **Local/Evidência:** `src/main/java/com/agendamentos/equadras/service/FileStorageService.java:17, 20, 109`
- **Problema:** A constante `private static final String UPLOAD_DIR = "uploads/quadras";` define um caminho de disco relativo ao diretório atual de execução do processo Java, sem possibilidade de customização externa. Além disso, no método `excluirArquivo` (linha 109), falhas de I/O são impressas via `System.err.println` em vez de utilizar o logger SLF4J configurado.
- **Impacto:** Em produção na VM ou contêineres, o caminho das imagens fica vulnerável ao diretório de trabalho a partir do qual o systemd/jar foi iniciado. O uso de `System.err` impede a formatação estruturada de logs e a correlação com IDs de rastreio.
- **Recomendação:** Externalizar o caminho do diretório de upload através de propriedade (`equadras.storage.upload-dir`) e substituir `System.err` por chamadas a `log.warn(...)`.
- **Esforço:** P
- **Relacionados:** (vazio)

---

## Sinais para outros domínios

- **Para A2 (Dados e Persistência):**
  - O listener `AgendamentoAuditoriaListener.java:33` acessa a navegação `agendamento.getQuadra().getNome()` em fase `AFTER_COMMIT` com `spring.jpa.open-in-view=false`. Se a entidade `Agendamento` não tiver inicializado o proxy da quadra na transação principal, ocorrerá `LazyInitializationException`. Note que `AgendamentoNotificacaoListener.java:48` contornou isso reconsultando via query com fetch graph; o listener de auditoria não o fez.
  - A entidade `Quadra.java:63` utiliza `@ElementCollection(fetch = FetchType.EAGER)` na coleção de fotos, provocando joins desnecessários e risco de problemas N+1 em listagens.
  - Há alto acoplamento horizontal entre serviços e repositórios: `AgendamentoService` injeta `BloqueioHorarioRepository`, e `BloqueioHorarioService` injeta `AgendamentoRepository`.
- **Para A3 (Concorrência e Tempo Real):**
  - O mapa `emitters` em `NotificacaoService.java:27` é estritamente em memória (`ConcurrentHashMap`), impedindo que múltiplos nós da aplicação recebam ou sincronizem eventos SSE em tempo real.
  - `FileStorageService.java:72` realiza I/O síncrono e bloqueante de arquivos em disco local dentro do fluxo da requisição web.
- **Para A4 (Segurança):**
  - A checagem de segredo da rota `/agendamentos/bot` no `AgendamentoController.java:110` possui bypass quando a propriedade não é informada no ambiente.
  - Os segredos `jwt.secret` e `equadras.bot.api-secret` possuem valores padrão pré-definidos no repositório (`application.properties:42, 69`).
  - `Usuario.java:110` expõe publicamente o e-mail do Master Admin (`gui@gmail.com`) via constante estática.
- **Para A5 (Infra e Operação):**
  - O projeto não possui arquivos de perfil dedicados (`application-prod.properties`), dependendo inteiramente de substituições de variáveis de ambiente em um único arquivo `application.properties`.
  - O diretório relativo `uploads/quadras` depende do diretório corrente de execução da JVM.

---

## O que não pôde ser verificado e por quê
- **Validação de execução em produção:** A inspeção foi estritamente estática (código-fonte e arquivos de configuração versionados). A confirmação se as variáveis de ambiente de produção na VM da Oracle Cloud realmente sobrepõem os fallbacks inseguros (como `JWT_SECRET` e `SPRING_DATASOURCE_PASSWORD`) não compete ao domínio A1 e deve ser auditada no domínio A5 (Infra e Operação).
