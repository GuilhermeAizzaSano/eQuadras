# 04 — Arquitetura de Segurança (Auditoria)

## Resumo Executivo
A aplicação apresenta bons controles de autenticação stateless e prevenção de CSRF customizada, mas falha em configurações vitais de infraestrutura (origem exposta) e gestão de segredos padrão no repositório. 

## Avaliação do Checklist

### 1. Autenticação (JWT)
- ❌ **Segredo HMAC carregado de variável de ambiente sem hardcode**
  - **Achado**: O arquivo `application.properties` define um fallback hardcoded para o segredo (`jwt.secret=${JWT_SECRET:***}`). Isso expõe um vetor de ataque se a variável de ambiente não for definida.
  - **Evidência**: `src/main/resources/application.properties:42`
  - **Severidade**: Alta
- ✅ **Algoritmo fixado na validação**
  - **Achado**: A validação no `JwtService.java` fixa a checagem usando `Jwts.parser().verifyWith(chave)`, prevenindo ataques de alg=none.
  - **Evidência**: `src/main/java/com/agendamentos/equadras/security/JwtService.java:55`
- ❓ **Validação de exp, iat, iss e aud; tolerância de clock skew**
  - **Motivo**: O framework valida `exp` automaticamente, mas `iss` (Issuer) e `aud` (Audience) não estão sendo registrados nem validados. Não há configuração explícita de clock skew, utilizando-se o padrão zero da biblioteca.
- ✅ **TTL do access token curto e Estratégia de revogação**
  - **Achado**: O TTL é de 30 minutos por padrão. A revogação stateless é realizada adequadamente através do claim `ver` (token version) verificado a cada request contra a base de dados.
  - **Evidência**: `src/main/java/com/agendamentos/equadras/security/JwtAuthenticationFilter.java:167`
- ❌ **Refresh token armazenado com hash no banco**
  - **Achado**: Não há implementação de Refresh Token. O sistema estende a duração máxima via cookie e exige reautenticação.
  - **Severidade**: Baixa (Trade-off de UX)

### 2. Autorização
- ✅ **IDOR (Insecure Direct Object Reference)**
  - **Achado**: Endpoints com ID na rota (ex: `QuadraController` e `AgendamentoController`) validam a pertinência dos recursos passando o `usuarioLogado.id()` para as camadas de serviço, garantindo isolamento por tenant/usuário.
  - **Evidência**: `src/main/java/com/agendamentos/equadras/controller/AgendamentoController.java:55`
- ✅ **Regras centralizadas**
  - **Achado**: Centralização adequada utilizando `SecurityFilterChain` e anotações `@PreAuthorize` isoladas onde necessário.
  - **Evidência**: `src/main/java/com/agendamentos/equadras/config/SecurityConfig.java:91`
- ✅ **permitAll revisado item a item**
  - **Achado**: As rotas públicas estão restritas a propósitos lógicos (login, logout, webhook, bot), sendo as APIs mutantes protegidas mesmo na isenção.
- ✅ **Actuator protegido**
  - **Achado**: Não há exposição do Actuator (dependência não inclusa publicamente).

### 3. Superfície HTTP
- ✅ **CORS com origens explícitas**
  - **Achado**: A configuração CORS rejeita `*` e exige a definição estrita na propriedade `origensPermitidas`.
  - **Evidência**: `src/main/java/com/agendamentos/equadras/config/SecurityConfig.java:42`
- ✅ **CSRF**
  - **Achado**: O filtro `ClientHeaderFilter` força a exigência do header customizado `X-Client: frontend` para métodos mutantes, blindando o uso de cookies.
  - **Evidência**: `src/main/java/com/agendamentos/equadras/security/ClientHeaderFilter.java:47`
- ❌ **Headers de segurança (CSP, HSTS, X-Content-Type-Options)**
  - **Achado**: Ausentes. Respostas da borda não contêm HSTS nem bloqueios de content sniffing.
  - **Evidência**: Teste real via `curl -sI https://equadras.app` não trouxe os referidos cabeçalhos.
  - **Severidade**: Média
- ✅ **Limite de tamanho de upload**
  - **Achado**: `FileStorageService` bloqueia uploads maiores que 5MB e realiza verificação estrita de magic bytes.
  - **Evidência**: `src/main/java/com/agendamentos/equadras/service/FileStorageService.java:20` e `:84`
- ❓ **Logs sem dado sensível**
  - **Motivo**: Não há evidência explícita de interceptores mascarando credenciais no payload transacional ou MDC. Necessário auditar regras específicas de logback.

### 4. Borda e origem
- ❌ **Origem acessível sem Cloudflare**
  - **Achado**: A VM responde a requisições HTTPS diretas no IP, permitindo contornar o WAF (Cloudflare).
  - **Evidência**: `curl -sk -o /dev/null -w "%{http_code}\n" --connect-timeout 5 https://137.131.163.62/ -H "Host: equadras.app"` retornou status `200`.
  - **Severidade**: Crítica
- ❓ **Modo SSL/TLS da Cloudflare em Full (strict)**
  - **Motivo**: A verificação exige acesso ao painel da Cloudflare. Como a origem atende com HTTPS, o uso de Full é suportado, mas Strict exige certificação do lado do Cloudflare que não podemos confirmar.
- ❌ **Porta Java (8080) escutando em todas as interfaces**
  - **Achado**: Configuração de bind no Tomcat definida para `0.0.0.0`, permitindo o acesso à porta Java contornando as regras locais do proxy (Nginx).
  - **Evidência**: `src/main/resources/application.properties:18` (`server.address=0.0.0.0`)
  - **Severidade**: Média
- ✅ **Rate limiting em rotas sensíveis**
  - **Achado**: O sistema dispõe do `RateLimitFilter` (token bucket) aplicando controles eficientes no login (10 req/min) e nas APIs gerais (120 req/min).
  - **Evidência**: `src/main/java/com/agendamentos/equadras/security/RateLimitFilter.java:98`

### 5. Dependências
- ❓ **Vulnerabilidades conhecidas**
  - **Motivo**: Requer execução de ferramentas como npm audit ou OWASP Dependency-Check (DCA), que fogem do escopo de inspeção apenas no código.
