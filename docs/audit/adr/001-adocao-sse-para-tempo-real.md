# ADR 001: Adoção de Server-Sent Events (SSE) para Notificações e Tempo Real

- **Status:** Proposto — retroativo
- **Data:** 2026-09-23
- **Decisores:** Time de Desenvolvimento eQuadras
- **Domínios Afetados:** A1 (Backend Structure), A3 (Concurrency/Realtime), A4 (Security), A5 (Infra), A6 (Frontend)

---

## 1. Contexto

A plataforma eQuadras necessita informar os usuários (administradores e clientes) sobre novos agendamentos, confirmações de pagamento via Pix, cancelamentos e expiração de horários em tempo real, sem exigir que o cliente execute polling contínuo via HTTP.

As opções consideradas para tempo real incluíam:
1. **Short Polling / Long Polling HTTP**: Requisições repetidas em intervalos fixos.
2. **WebSocket**: Conexão bidirecional full-duplex sobre TCP.
3. **Server-Sent Events (SSE)**: Stream unidirecional do servidor para o cliente sobre HTTP padrão (`text/event-stream`).

## 2. Decisão

Adotou-se **Server-Sent Events (SSE)** implementado via `SseEmitter` do Spring MVC no endpoint `/notificacoes/stream`, consumido pela API nativa `EventSource` no frontend React (`useAdminNotifications.ts`).

## 3. Consequências e Riscos Identificados

### Positivas
- **Simplicidade de Protocolo:** Opera nativamente sobre HTTP/HTTPS (portas 80/443), sem necessidade de handshake de upgrade ou bibliotecas pesadas de WebSocket.
- **Suporte Nativo nos Navegadores:** A API `EventSource` gerencia conexão unidirecional de texto simples.
- **Integração com Spring MVC:** Suporte nativo via `SseEmitter`.

### Negativas / Riscos Revelados na Auditoria
- **Acoplamento a Instância Única (SPOF):** O registro de emissores ativos é mantido em memória local (`ConcurrentHashMap` em `NotificacaoService`). Caso a aplicação seja escalada horizontalmente para múltiplas instâncias, eventos gerados em uma instância não alcançarão clientes conectados a outra, exigindo a introdução de Pub/Sub externo (PostgreSQL `LISTEN/NOTIFY` ou Redis).
- **Encerramento Silencioso pelo Proxy (Cloudflare 524):** A Cloudflare encerra requisições HTTP sem tráfego após ~100 segundos. A ausência de um mecanismo de *heartbeat* periódico (comentários SSE `:` a cada 15-30s) faz com que a conexão caia continuamente.
- **Tratamento Inadequado no Cliente (A6):** O hook `useAdminNotifications.ts` chama `close()` imediatamente no evento `onerror`, destruindo a reconexão automática e mantendo o administrador desconectado silenciosamente.
- **Retenção de Conexão com Banco de Dados (A3):** O método `enviarNotificacao` foi anotado com `@Transactional`, fazendo com que a transação e a conexão JDBC do HikariCP fiquem presas durante o I/O de rede com todos os clientes SSE conectados.
