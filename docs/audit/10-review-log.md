# 10 — Log de Revisão de Auditoria em Dois Estágios

Este documento registra a revisão independente em dois estágios (Conformidade com o Plano e Qualidade/Evidências dos Achados) de todos os relatórios da Fase 1 (A1..A7), conforme preconizado pelo plano de auditoria de arquitetura.

---

## Matriz Geral de Revisão por Domínio

| Domínio | Subagente | Revisor | Veredito | Ciclos | Observação |
|---|---|---|---|---|---|
| **A1 — Estrutura Backend** | A1 (`flash`) | R1 (`flash`) | **APROVADO** | 1 ciclo | 15 achados; fallbacks mascarados com `***`. |
| **A2 — Dados e Persistência** | A2 (`pro`) | R2 (`pro`) | **APROVADO** | 2 ciclos | Ajustada análise de Pooler HikariCP max=5, Supavisor IPv4/IPv6 e `prepareThreshold=0`. |
| **A3 — Concorrência e Tempo Real** | A3 (`pro`) | R3 (`pro`) | **APROVADO** | 1 ciclo | 13 achados; ratificado risco de pinning, ausência de heartbeat SSE e retenção de conexões DB. |
| **A4 — Segurança** | A4 (`pro`) | R4 (`pro`) | **APROVADO** | 2 ciclos | Mascarado segredo JWT fallback com `***`. Confirmada prevenção IDOR, bind 0.0.0.0 e bypass do bot. |
| **A5 — Infra e Operação** | A5 (`flash`) | R5 (`flash`) | **APROVADO** | 1 ciclo | 12 achados; saídas SSH auditadas na VM OCI; SPOF e risco de OOM no deploy comprovados. |
| **A6 — Frontend** | A6 (`flash`) | R6 (`flash`) | **APROVADO** | 1 ciclo | 10 achados; SSE sem reconexão em erro, ausência de cache de server state e abas ocultas no DOM. |
| **A7 — Qualidade e Entrega** | A7 (`flash_lite`) | R7 (`flash_lite`) | **APROVADO** | 1 ciclo | 5 achados; H2 em vez de Postgres 17, ausência de Testcontainers/ArchUnit e apenas 1 teste no front. |

---

## Revisão A1 — Estrutura e Camadas do Backend

- **Domínio:** A1 — Estrutura e Camadas do Backend
- **Veredito:** APROVADO
- **Avaliação do Estágio 1 (Conformidade):**
  - Todos os 10 itens do checklist da Task A1 possuem status explícito (4 ⚠️ Parcial, 6 ❌ Não conforme) e justificativas técnicas detalhadas na tabela da Seção 2.
  - Nenhum item foi marcado com `?` no checklist; a seção "O que não pôde ser verificado e por quê" fundamenta adequadamente o limite do escopo estático em relação à validação em runtime na VM OCI (delegada ao domínio A5).
  - O formato padrão rigoroso dos achados foi 100% respeitado em todos os 15 achados (`[A1-01]` a `[A1-15]`), contendo ID, Domínio, Tipo, Local/Evidência com linhas exatas, Problema, Impacto no atributo de qualidade, Recomendação mínima e Esforço.
  - Não há exposição de segredos reais de produção. Literais de fallbacks foram devidamente sanitizados com `***`.
  - Nenhuma ação indevida de escrita foi executada no código-fonte (`src/` intacto).

- **Avaliação do Estágio 2 (Qualidade da Evidência e Calibração de Severidade):**
  - **Confirmação direta das evidências no código-fonte:**
    - `A1-01`: Confirmado retorno de entidade JPA em `NotificacaoController.java:37` (`ResponseEntity<Page<Notificacao>>`) e uso de `@JsonIgnore` na entidade de domínio `Notificacao.java:16`.
    - `A1-02`: Confirmado em `GlobalExceptionHandler.java:32-41` que `IllegalArgumentException` é mapeada como HTTP 400 Bad Request, enquanto buscas por ID em `QuadraService.java:112` (e demais serviços) lançam `IllegalArgumentException` em vez de exceção de recurso inexistente (HTTP 404).
    - `A1-03`: Confirmada orquestração excessiva e parsing manual de webhooks em `PagamentoController.java:48-62, 69-141` e manipulação direta de entidades brutas em `QuadraController.java:145-187`.
    - `A1-04`: Confirmada a existência de fallbacks previsíveis em `application.properties:3, 42, 64, 69` para banco de dados, JWT secret, e-mail master e bot secret.
    - `A1-05`: Confirmada divergência real de configuração: `JwtService.java:25` define fallback de 8 horas (`28800000`), enquanto `application.properties:43` define 30 minutos (`1800000`). Em `SecurityConfig.java:31`, a origem de produção `https://equadras.app` é omitida pelo override de `application.properties:46`.
    - `A1-06`: Confirmada brecha de bypass de autenticação em `AgendamentoController.java:108-112`, onde a ausência da variável de ambiente anula a checagem `!botApiSecret.isBlank()`, permitindo requisições não autenticadas no endpoint público de criação de reservas do bot.
    - `A1-07`: Confirmado retorno polimórfico não tipado `ResponseEntity<?>` em `QuadraController.java:48, 145`, variando o schema de resposta com base em headers, parâmetros e inspeção de URI.
    - `A1-08`: Confirmada duplicação estrita de regras entre serviços (`QuadraService.java:101-105` e `BloqueioHorarioService.java:43-49`), além de incoerência no tipo de exceção de segurança lançada (`AccessDeniedException` vs `IllegalArgumentException`).
    - `A1-09`: Confirmado estado estático mutável `Usuario.MASTER_EMAIL_CONFIGURADO` na entidade `Usuario.java:110` e sua sobrescrita imperativa no construtor de `UsuarioService.java:45`.
    - `A1-10`: Confirmado acoplamento de `AuditoriaService.java:50-79` com Servlets via `HttpRequestUtil.java:12-18` através de `RequestContextHolder`.
    - `A1-11`: Confirmada configuração monolítica de 922 linhas em `OpenApiConfig.java:152-800` com centenas de linhas de JSON mockado imperativamente e ausência total de versionamento semântico nas rotas.
    - `A1-12`: Confirmada gestão direta de conexões `SseEmitter` em `ConcurrentHashMap` dentro do serviço de domínio `NotificacaoService.java:27-54`.
    - `A1-13`: Confirmado código morto (`@DevOnly.java:13`), classe com nome enganoso (`CorsConfig.java:14` sem configuração CORS) e atributo órfão não utilizado `usuarioId` em `AgendamentoCriacaoDTO.java:11`.
    - `A1-14`: Confirmada reutilização do record de requisição `DisponibilidadeDiaDTO.java:6-10` dentro de `QuadraResponseDTO.java:59` e ausência de anotações Bean Validation (`@NotNull`) em seus campos.
    - `A1-15`: Confirmado diretório de upload relativo fixo (`"uploads/quadras"`) em `FileStorageService.java:17` e uso de `System.err.println` na linha 109.
  - **Calibração de severidade:** Os 15 achados foram distribuídos coerentemente com o impacto arquitetural: 6 Altos, 5 Médios e 4 Baixos.
  - **Falsos positivos:** Zero.

- **Lista Objetiva de Apontamentos:**
  1. Sanitização concluída nos fallbacks de segredos em `A1-04`.
  2. Priorização imediata de `A1-06` (Bypass no Bot) na Onda 0 de segurança.
  3. Aprovado integralmente.

---

## Revisão A2 — Dados e Persistência

- **Domínio:** A2 — Dados e Persistência
- **Veredito:** APROVADO (após 2º ciclo de revisão)
- **Avaliação do Estágio 1 (Conformidade):**
  - Todos os itens do checklist da Task A2 (Schema/migrações, JPA/Hibernate, Conexão Supabase, Exposição Supabase) foram atendidos e avaliados.
  - A lacuna anterior referente à análise do pooler e rede (Hikari max=5 e IPv4/IPv6) foi devidamente preenchida pelo subagente no 2º ciclo.
  - Formato padrão rigoroso mantido em todos os achados.
  - Identificação e mascaramento adequado de credenciais e fallbacks no `application.properties`.
  - Nenhuma marcação ❓ sem justificativa.
- **Avaliação do Estágio 2 (Qualidade e Evidências):**
  - Fatos confirmados em `application.properties`, migrações Flyway V1..V7, entidades e repositórios.
  - O dimensionamento do pool HikariCP (max=5) e a exigência do Supavisor (pooler) para resgate de comunicação IPv4 estão modelados de forma precisa perante a arquitetura de rede do Supabase.
  - A ausência do parâmetro `prepareThreshold=0` na JDBC URL foi detalhada para evitar que Prepared Statements quebrem no Transaction Mode.
  - Diagnóstico da criticidade da ausência de RLS nas tabelas de negócio no V1 combinado com o papel `postgres` ratificado para proteção contra acesso direto via PostgREST.
  - Mapeamento `@ElementCollection(fetch = FetchType.EAGER)` em `Quadra` verificado como gerador potencial de N+1.
- **Lista Objetiva de Apontamentos:**
  - Todas as recomendações foram acatadas e incorporadas ao relatório. Aprovado integralmente.

---

## Revisão A3 — Concorrência e Tempo Real

- **Domínio:** A3 — Concorrência e Tempo Real
- **Veredito:** APROVADO
- **Avaliação do Estágio 1 (Conformidade):**
  - Todos os itens do checklist da Task A3 (Virtual Threads, SSE, Outros) foram categorizados corretamente com status.
  - Os pontos marcados com ❓ possuem justificativas válidas (Nginx externo ao código Java e vazamento de token necessitando análise cruzada).
  - Formato padrão rigorosamente respeitado.
  - Nenhum segredo exposto.
- **Avaliação do Estágio 2 (Qualidade e Evidências):**
  - **Virtual Threads e Pinning:** Evidência em `RateLimitFilter.java` procede. O uso de blocos `synchronized` com I/O/bloqueios pode causar pinning das Carrier Threads. Ausência de bulkhead de pool confirmada diante de HikariCP max=5 e ausência de barreira de concorrência.
  - **SSE (`NotificacaoService.java`):** Registro stateful em `ConcurrentHashMap` prende a arquitetura a uma única instância. Ausência de Heartbeat causa erro 524 no Cloudflare a cada ~100s. Anotação `@Transactional` retém conexão com o banco de dados durante o I/O bloqueante de `emitter.send()`. Ausência de `Last-Event-ID` verificada.
  - **Agendamentos e Caches:** Tarefas agendadas usam `@Scheduled` puro, exigindo Lock Distribuído (ex: ShedLock) em cluster.
  - Severidades devidamente calibradas (1 Crítico, 3 Altos, 5 Médios, 4 Baixos/Info).
- **Lista Objetiva de Apontamentos:**
  - Relatório aprovado sem ressalvas; fundamentado com precisão no código-fonte.

---

## Revisão A4 — Segurança

- **Domínio:** A4 — Segurança
- **Veredito:** APROVADO (após 2º ciclo de revisão)
- **Avaliação do Estágio 1 (Conformidade):**
  - Segredo JWT de fallback vazado anteriormente foi devidamente mascarado como `***` pelo subagente na re-revisão, cumprindo as regras de segurança do projeto.
  - Todos os itens do checklist possuem status explícito.
  - Todas as marcações ❓ possuem motivos justificados.
  - Formato estruturado respeitado.
- **Avaliação do Estágio 2 (Qualidade e Evidências):**
  - Evidências validadas em `SecurityConfig.java`, `JwtService.java`, `AgendamentoController.java`, `ClientHeaderFilter.java`, `FileStorageService.java`, `RateLimitFilter.java`, `application.properties`.
  - Achado de prevenção a IDOR consistente (validação baseada no repasse de `usuarioLogado.id()`).
  - Controle de CSRF, Upload (5MB + Magic Bytes), CORS restrito e bind em `0.0.0.0` ratificados.
  - Severidades calibradas e sem falsos positivos (1 Crítico, 1 Alto, 2 Médios, 1 Baixo).
- **Lista Objetiva de Apontamentos:**
  - Relatório aprovado com excelência após correção do mascaramento de segredos.

---

## Revisão A5 — Infra e Operação

- **Domínio:** A5 — Infra e Operação
- **Veredito:** APROVADO
- **Avaliação do Estágio 1 (Conformidade):**
  - Todos os 18 itens do checklist da Task A5 (VM e JVM, systemd, Nginx e TLS, Operação) possuem status explícito (✅, ⚠️, ❌) e resumos embasados na tabela da Seção 2.
  - Os 3 itens marcados como `?` na Seção 5 possuem justificativas técnicas claras e válidas (impossibilidade de auditar Security Lists/NSG da OCI sem acesso à console/CLI da nuvem, console web do Supabase para snapshots e e-mails de alerta de ociosidade).
  - O formato padrão de achado foi rigorosamente seguido em todos os 12 achados (`[INFRA-01]` a `[INFRA-12]`).
  - Nenhum segredo ou credencial SSH foi exposta no relatório. Senhas e tokens mascarados como `***`.
  - Modo estritamente somente leitura mantido.
- **Avaliação do Estágio 2 (Qualidade e Evidências):**
  - Consistência das saídas de comando verificada via SSH (`uptime`, `free -h`, `sar -u`, `ps`/`jcmd`, permissões de `/etc/default/equadras-backend`, `iptables`, `ss -tlnp`, Let's Encrypt).
  - Diagnóstico de SPOF total (`INFRA-01`) e violação do RTO acordado (2 a 4+ h vs meta ≤ 2 h) alinhado com inexistência de IaC/Docker.
  - Risco de OOM na VM de 1GB (`INFRA-08`, `INFRA-10`) comprovado: execução de `./mvnw clean package` na produção durante o deploy (`.github/workflows/ci.yml:97`) coloca o host em risco de OOM Killer.
  - Ausência de `-XX:+HeapDumpOnOutOfMemoryError` e `-XX:+ExitOnOutOfMemoryError` impede o reinício automático pelo systemd em GC thrashing.
  - Distribuição calibrada: 3 🔴 Crítico, 3 🟠 Alto, 4 🟡 Médio, 2 🔵 Baixo.
- **Lista Objetiva de Apontamentos:**
  - Relatório de infraestrutura e operações aprovado integralmente.

---

## Revisão A6 — Arquitetura do Frontend

- **Domínio:** A6 — Arquitetura do Frontend
- **Veredito:** APROVADO
- **Avaliação do Estágio 1 (Conformidade):**
  - Todos os 25 itens do checklist da Task A6 e de `frontend-audit-checklist.md` possuem status explícito e justificativas técnicas detalhadas.
  - A seção final de limites da auditoria justifica fundamentadamente os itens não verificados em runtime.
  - Formato padronizado seguido nos 10 achados (`[F-A6-01]` a `[F-A6-10]`).
  - Nenhum segredo exposto.
- **Avaliação do Estágio 2 (Qualidade e Evidências):**
  - Listener `onerror` no SSE (`useAdminNotifications.ts`) invoca `close()` e anula a ref sem temporizador ou backoff, quebrando a auto-reconexão nativa do `EventSource` (`F-A6-01`).
  - Inexistência de biblioteca de Server State; mais de 20 `useState` manuais e `useEffect` imperativos (`F-A6-02`).
  - Inputs de filtro disparam chamadas de API a cada tecla digitada sem debounce nem cancelamento por `AbortController` (`F-A6-03`).
  - Abas inativas mantidas no DOM com `hidden` e `ModalPix` com short polling contínuo sem escutar `visibilitychange` (`F-A6-04`).
  - Defasagem entre `openapi.yaml` e DTOs reais exigindo `Omit` no front (`F-A6-05`).
  - Modais de domínio dentro de `components/ui/` e cliente monolítico `apiClient.ts` com 400+ linhas (`F-A6-06`).
  - Roteamento por estado local sem URL nem suporte a histórico/deep linking (`F-A6-07`).
  - Master Admin condicionado a e-mail hardcoded no bundle (`F-A6-08`).
  - Apenas 1 arquivo de teste unitário no frontend (`F-A6-09`).
  - Chamadas de geocodificação diretas ao Nominatim com header proibido (`F-A6-10`).
  - Severidades: 1 Crítico, 3 Altos, 4 Médios, 2 Baixos. Ausência de falsos positivos.
- **Lista Objetiva de Apontamentos:**
  - Relatório aprovado com excelência técnica.

---

## Revisão A7 — Qualidade, Testes e Entrega

- **Domínio:** A7 — Qualidade, Testes e Entrega
- **Veredito:** APROVADO
- **Avaliação do Estágio 1 (Conformidade):**
  - Todos os itens do checklist da Task A7 contemplados.
  - Itens marcados com `?` justificados.
  - Formato padrão rigorosamente respeitado.
  - Nenhum segredo exposto.
- **Avaliação do Estágio 2 (Qualidade e Evidências):**
  - Confirmação em `.github/workflows/ci.yml`: O pipeline roda testes apenas no backend (`-DskipTests=false`), não executa testes no frontend, e no deploy via SSH executa compilação Maven com `-DskipTests=true` diretamente na VM de produção OCI.
  - Confirmação em `pom.xml`: Uso de H2 para testes, sem Testcontainers com PostgreSQL 17 e sem ArchUnit.
  - Confirmação em `frontend/src`: Apenas 1 teste unitário (`dateUtils.test.ts`), confirmando a fragilidade da cobertura do frontend.
  - Severidades calibradas: 0 Crítico, 2 Altos, 2 Médios, 1 Baixo.
- **Lista Objetiva de Apontamentos:**
  - Relatório aprovado integralmente.
