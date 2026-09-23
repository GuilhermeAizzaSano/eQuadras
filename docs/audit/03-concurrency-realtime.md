# Auditoria de Arquitetura - A3: Concorrência e Tempo Real

## 1. Virtual Threads e Concorrência

* **[INFO] Ativação de Virtual Threads**
  * **Arquivo:** `src/main/resources/application.properties` (linha 32)
  * **Evidência:** `spring.threads.virtual.enabled=true`
  * **Observação:** Ativo globalmente para o Tomcat no Spring Boot 4.
* **[MÉDIO] Pinning de Virtual Threads com `synchronized`**
  * **Arquivo:** `src/main/java/com/agendamentos/equadras/security/RateLimitFilter.java` (linhas 37, 55, 61, 66)
  * **Evidência:** Uso de métodos `synchronized` (`tryConsume`, `secondsUntilAvailable`, etc.).
  * **Observação:** O uso de `synchronized` pode causar pinning da virtual thread à carrier thread do sistema operacional (embora o risco seja maior quando há I/O bloqueante no bloco). É recomendado migrar para `ReentrantLock`.
* **[ALTO] Gargalo do Pool de Conexões (Ausência de Bulkhead)**
  * **Arquivo:** `src/main/resources/application.properties` / Ausência de limitador no nível de controller.
  * **Evidência:** Virtual threads removem o limite de concorrência natural do Tomcat, mas as requisições param no HikariCP. Não foi encontrado um semáforo (Semaphore) ou rate limit global antes do acesso ao banco, o que pode esgotar as conexões e gerar timeouts.
* **[INFO] Ausência de `ThreadLocal` Inadequado**
  * **Evidência:** Pesquisa por `ThreadLocal` não retornou usos explícitos na base de código.
* **[INFO] Timeouts definidos em chamadas externas**
  * **Arquivo:** `src/main/java/com/agendamentos/equadras/service/PagamentoService.java` (linhas 110, 156)
  * **Evidência:** Chamadas ao Mercado Pago com `RestClient` possuem `.timeout(Duration.ofMillis(4000))` explicitamente configurado.

## 2. SSE (Server-Sent Events)

* **[ALTO] Registro de `SseEmitter` em Memória (Stateful)**
  * **Arquivo:** `src/main/java/com/agendamentos/equadras/service/NotificacaoService.java` (linha 27)
  * **Evidência:** `private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();`
  * **Observação:** O registro em memória prende o sistema a uma única instância. Se houver escalabilidade horizontal, é necessário pub/sub.
* **[INFO] Limpeza e Ciclo de Vida do Emitter**
  * **Arquivo:** `src/main/java/com/agendamentos/equadras/service/NotificacaoService.java` (linhas 46-51)
  * **Evidência:** Instanciação de `new SseEmitter(3600000L)` e chamadas a `onCompletion`, `onTimeout` e `onError` removendo os registros do map.
  * **Observação:** Timeout está definido e os event listeners garantem a limpeza, prevenindo vazamentos de memória.
* **[MÉDIO] Ausência de Heartbeat**
  * **Arquivo:** `src/main/java/com/agendamentos/equadras/service/NotificacaoService.java`
  * **Evidência:** Nenhuma tarefa ou envio periódico (ping/comentário).
  * **Observação:** Sem heartbeat, o proxy da Cloudflare encerrará as conexões inativas (erro 524) a cada ~100s.
* **[CRÍTICO] Envio para Emitters dentro de Transação do Banco**
  * **Arquivo:** `src/main/java/com/agendamentos/equadras/service/NotificacaoService.java` (linhas 56-75)
  * **Evidência:** O método `enviarNotificacao` possui `@Transactional` e dispara `emitter.send(...)` internamente.
  * **Observação:** Se o cliente for lento, a operação de envio bloqueará a thread, segurando a conexão com o banco de dados por mais tempo do que o necessário.
* **[MÉDIO] Reentrega (Ausência de Last-Event-ID)**
  * **Arquivo:** `src/main/java/com/agendamentos/equadras/service/NotificacaoService.java` (linha 75)
  * **Evidência:** `emitter.send(SseEmitter.event().name("notificacao").data(json));`
  * **Observação:** O evento é enviado sem um `id:`. Se houver desconexão, o cliente perderá eventos, pois a especificação SSE exige IDs para o `Last-Event-ID`.
* **[MÉDIO] Limite estrito de conexões (1 por usuário)**
  * **Arquivo:** `src/main/java/com/agendamentos/equadras/service/NotificacaoService.java` (linhas 37-43)
  * **Evidência:** Ao fazer `.put(usuarioId, emitter)`, o anterior é recuperado e fechado (`antigoEmitter.complete()`).
  * **Observação:** Um admin não conseguirá manter o painel aberto em duas abas simultaneamente.

## 3. Caches, Agendamentos e Outros

* **[ALTO] Agendamentos `@Scheduled` sem Lock Distribuído**
  * **Arquivo:** `src/main/java/com/agendamentos/equadras/service/AgendamentoService.java` (linha 644) / `ApiKeyRateLimiter.java` (linha 100) / `LoginRateLimiter.java` (linha 103)
  * **Evidência:** Uso de `@Scheduled(fixedRate = 60000)` para expirar reservas e limpar rate limitings.
  * **Observação:** Se escalado horizontalmente, essas tarefas rodarão simultaneamente em todas as instâncias. Deve-se usar ShedLock ou Quartz.
* **[MÉDIO] Caches Inbound sem Capacidade Máxima (Risco de OOM)**
  * **Arquivo:** `src/main/java/com/agendamentos/equadras/security/ApiKeyRateLimiter.java` (linhas 40-41) / `LoginRateLimiter.java` (linha 42)
  * **Evidência:** Utiliza `ConcurrentHashMap` para Rate Limiting.
  * **Observação:** Sem a definição de um limite de chaves, um ataque pode estourar a memória (OOM). Sugerido o uso de `Caffeine` com `maximumSize`.
* **[INFO] Idempotência**
  * **Arquivo:** `src/main/java/com/agendamentos/equadras/service/AgendamentoService.java` (linha 156)
  * **Evidência:** O webhook tem checagem `if (agendamento.getStatus() == StatusAgendamento.CONFIRMADO)` no fluxo de `confirmarPagamentoPorWebhook`.
  * **Observação:** A lógica protege contra processamento duplicado de eventos externos.

## 4. Itens Pendentes de Confirmação Externa

* ❓ **Nginx (Configuração de SSE):** A configuração para SSE (proxy_buffering off, proxy_read_timeout, HTTP/1.1) não está presente nos arquivos Java. Deve ser checada diretamente no arquivo `.conf` do Nginx no servidor.
* ❓ **Autenticação de SSE (Risco de Vazamento):** A rota de stream (`/notificacoes/stream`) utiliza a anotação `@UsuarioLogado`. No entanto, como `EventSource` no front-end não passa headers, a passagem do JWT provavelmente ocorre via Query String. Isso requer análise cruzada (A4/A6) para validar se o token não está sendo registrado nos logs de acesso HTTP.
