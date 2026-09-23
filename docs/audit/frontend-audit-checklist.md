# Checklist de Auditoria Arquitetural de Frontend (eQuadras)

Este checklist serve de guia estruturado e critérios de conformidade para o subagente **A6 (Arquitetura do Frontend)**.

---

## 0. Varredura Automática & Baseline
- [ ] Compilação TypeScript estrita (`npx tsc --noEmit` sem erros)
- [ ] Execução da suíte de testes (`npm test` / Vitest)
- [ ] Auditoria de vulnerabilidades de dependências (`npm audit --omit=dev`)
- [ ] Análise de dependências desnecessárias ou duplicadas (`package.json`)
- [ ] Build de produção (`npm run build`) e estimativa de tamanho de bundle/chunks

---

## 1. Estrutura e Organização Modular
- [ ] **Organização por Feature:** Módulos isolados por domínio funcional (ex: autenticação, quadras, agendamentos, pagamentos, perfil).
- [ ] **Fronteiras e Camada Compartilhada (`shared`/`components`/`common`):** Componentes comuns reutilizáveis são puros e não possuem dependências circulares com features específicas.
- [ ] **Co-locação de Código:** Componentes, hooks locais, tipos locais e utilitários residem próximos de suas respectivas features.
- [ ] **Acoplamento:** Não há imports cruzados caóticos entre features irmãs sem abstração ou serviço mediador.

---

## 2. Gerenciamento de Estado & Dados
- [ ] **Separação de Estados:** Separação explícita entre:
  - **Server State:** Cache de requisições HTTP e sincronização com backend.
  - **UI / Client State:** Estado efêmero de interface (modais abertos, dropdowns, inputs de formulário).
  - **Global / Auth State:** Sessão do usuário logado, permissões e token.
- [ ] **Evitar Duplicação de Estado:** Ausência de dados derivados armazenados redundantemente em múltiplos estados React.
- [ ] **Prop Drilling vs Context:** Contextos React possuem escopo delimitado para evitar re-renderizações em cascata desnecessárias.

---

## 3. Camada de API & Tipagem
- [ ] **Contrato Único & Tipado:** Consumo da API através de cliente HTTP padronizado e centralizado.
- [ ] **Sincronização com OpenAPI:** Tipos de requisição e resposta alinhados com o OpenAPI do backend (`openapi.json` / `openapi.yaml` via `openapi-typescript` ou DTOs manuais estritos).
- [ ] **Tratamento de Falhas na Camada de Rede:** Tratamento uniforme de status codes HTTP (400, 401, 403, 404, 500, network offline).
- [ ] **Tipagem Estrita (Zero `any`):** Ausência de types frouxos (`any`, type assertions cegas `as unknown as X`) nas fronteiras de rede.

---

## 4. Concorrência, Tempo Real & SSE (Server-Sent Events)
- [ ] **Conexão SSE Centralizada:** Uso de um único listener/gerenciador para o stream de eventos, evitando múltiplas conexões concorrentes desnecessárias na mesma sessão.
- [ ] **Ciclo de Vida & Cleanup:** Encerramento explícito (`close()`) de conexões `EventSource` em desmonte de hooks/componentes para evitar vazamento de memória e sockets abertos.
- [ ] **Autenticação em SSE:** Como o token é fornecido ao SSE (query param vs cookies vs fetch polyfill) e se há exposição de credenciais.
- [ ] **Resiliência & Reconexão:** Tratamento de quedas de rede e erro 524 da Cloudflare com backoff exponencial ou limite de tentativas.
- [ ] **Comportamento em Abas em Background:** Tratamento de abas minimizadas ou inativas (limpeza de buffer, descarte ou pausamento de polling).

---

## 5. Segurança, Sessão & Autenticação
- [ ] **Armazenamento de Tokens:** Onde o JWT é mantido (`localStorage`, `sessionStorage`, cookie `HttpOnly`) e vetores de risco XSS.
- [ ] **Ciclo de Expiração & 401 Unauthorized:** Redirecionamento automático e limpeza graciosa de sessão ao expirar o token ou receber 401 da API/SSE.
- [ ] **Proteção de Rotas:** Guards de rota implementados para boa UX, com validação de credenciais preservada no backend.
- [ ] **Higienização de Entradas:** Sanitização contra injeção de HTML/XSS em campos renderizados dinamicamente.
- [ ] **Exposição de Segredos:** Nenhuma chave secreta ou credencial de backend vazada no bundle estático (`.env`, `VITE_` prefix).

---

## 6. Performance, Renderização & UX
- [ ] **Code Splitting & Lazy Loading:** Rotas pesadas carregadas via `React.lazy` / `Suspense` para otimizar First Contentful Paint (FCP).
- [ ] **Gerenciamento de Re-renders:** Uso criterioso de `useMemo`, `useCallback`, e `React.memo` em componentes de renderização intensiva (ex: grade de horários/agenda).
- [ ] **Estratégia de Abas Ocultas:** Abas de navegação internas descarregam ou pausam execuções pesadas quando fora da viewport.
- [ ] **Feedback Visual de Estado:** Estados claros de carregamento (skeletons/spinners), estados vazios (empty states) e mensagens de erro amigáveis para o usuário.
