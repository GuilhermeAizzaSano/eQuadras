# A6 — Arquitetura do Frontend

## Escopo coberto

Auditoria arquitetural completa do subsistema frontend da plataforma eQuadras (`frontend/`), cobrindo:
- **Baseline automatizado:** execução de tipagem TypeScript (`tsc`), testes unitários (`vitest`), auditoria de segurança de dependências (`npm audit`) e análise de build de produção (`vite build`).
- **Estrutura e modularização:** topologia de pastas, separação por features vs camadas técnicas, acoplamento entre módulos e pureza da camada compartilhada (`components/ui`).
- **Gerenciamento de estado:** separação entre Server State, UI State e Global State, estratégia de cache, prop drilling e contextos.
- **Camada de rede e contratos:** cliente HTTP (`apiClient.ts`), integração e sincronização com OpenAPI (`openapi-typescript`), tratamento padronizado de erros e tipagem estrita (zero `any`).
- **Comunicação em tempo real:** cliente SSE (`useAdminNotifications.ts`), ciclo de vida de conexões, resiliência/reconexão e tratamento em abas em segundo plano.
- **Autenticação e sessão:** ciclo do token JWT, armazenamento (cookies `HttpOnly` vs `localStorage`), proteção de rotas (UX guards), tratamento de 401 e logout.
- **Roteamento e performance:** code-splitting com `React.lazy`, retenção de abas em memória, re-renderizações e debouncing.

---

## Seção 0 — Varredura Automática & Baseline

Resultados obtidos diretamente no diretório `frontend/`:

### 0.1. Verificação Estrita de Tipos (`npx tsc --noEmit`)
```text
Comando: npx tsc --noEmit (em frontend/)
Status: Sucesso (Código 0)
Erros detectados: 0
```

### 0.2. Execução da Suíte de Testes (`npm test`)
```text
Comando: npm test (vitest run)
Status: Sucesso (Código 0)
Duração: 16.59s

 ✓ src/utils/dateUtils.test.ts (3 tests) 16ms

 Test Files  1 passed (1)
      Tests  3 passed (3)
```
> [!WARNING]
> Apenas 1 arquivo de teste (`dateUtils.test.ts`) existe em todo o frontend, contendo exclusivamente 3 testes de funções puras de data. Nenhum componente, hook, contexto ou fluxo de integração é testado.

### 0.3. Auditoria de Segurança de Dependências (`npm audit --omit=dev`)
```text
Comando: npm audit --omit=dev
Status: Sucesso (Código 0)
Vulnerabilidades encontradas: 0
```

### 0.4. Análise de Dependências de Produção (`package.json`)
```json
{
  "dependencies": {
    "clsx": "^2.1.1",
    "lucide-react": "^0.436.0",
    "qrcode.react": "^4.2.0",
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "tailwind-merge": "^3.7.0"
  }
}
```
> **Observação:** O frontend possui uma árvore de dependências direta minimalista (apenas 6 pacotes de produção). Não há bibliotecas externas de roteamento (`react-router-dom`), nem de gerenciamento de cache de servidor (`@tanstack/react-query`, `swr`).

### 0.5. Build de Produção e Análise de Chunks (`npm run build`)
```text
Comando: npm run build (tsc && vite build)
Status: Sucesso (Código 0) em 6.15s

Artefatos gerados:
dist/index.html                            1.00 kB │ gzip:  0.52 kB
dist/assets/login-bg-Drw_uoKI.jpg        214.61 kB
dist/assets/index-BxU5Motq.css            51.85 kB │ gzip:  9.00 kB
dist/assets/AuthPage-D01DlJ_j.js           4.24 kB │ gzip:  1.67 kB
dist/assets/dateUtils-B8yGxZyb.js         12.39 kB │ gzip:  3.98 kB
dist/assets/icons-D_aSkkgA.js             32.44 kB │ gzip:  7.51 kB
dist/assets/ClientDashboard-DUfZY9Tm.js   46.64 kB │ gzip: 12.48 kB
dist/assets/index-Ds0cjtEL.js            100.69 kB │ gzip: 30.00 kB
dist/assets/vendor-EBnPtp4H.js           133.93 kB │ gzip: 43.12 kB
dist/assets/AdminDashboard-FJqWabIv.js   146.40 kB │ gzip: 32.81 kB
```

---

## Itens do checklist

| Item | Status | Justificativa Resumida |
|---|:---:|---|
| **0. Baseline e Varredura Automática** | ✅ | `tsc`, `test`, `audit` e `build` executam com sucesso e sem falhas. |
| **1.1. Organização por Feature** | ❌ | Projeto estruturado por tipo de arquivo técnico (`components/admin`, `components/client`, `pages`, `hooks`) com "God Components" de 600 a 800 linhas. |
| **1.2. Fronteiras e Camada Compartilhada (`ui`)** | ❌ | `components/ui/` contém modais de domínio (`BookingModal`, `ModalPix`, `ApiKeyModal`, etc.) com regras de negócio e acoplamento a DTOs. |
| **1.3. Co-locação de Código** | ❌ | Hooks, tipos e componentes filhos residem em diretórios globais distantes do domínio de negócio. |
| **1.4. Acoplamento entre Módulos** | ⚠️ | Setters de estado de UI do componente pai são repassados a custom hooks (`useCourtBlocks`, `useCourtForm`) e prop drilling excessivo (15 props em `BookingModal`). |
| **2.1. Separação de Estados (Server/UI/Global)** | ❌ | Inexistência de biblioteca de Server State; dados de API misturados com estado de UI em mais de 20 `useState` manuais por tela. |
| **2.2. Evitar Duplicação de Estado** | ⚠️ | Há usos pontuais de `useMemo`, porém sincronizações manuais de flags e listas geram risco de descompasso. |
| **2.3. Prop Drilling vs Context** | ⚠️ | `AuthContext` é enxuto, mas a ausência de contextos por feature sobrecarrega componentes raiz com dezenas de props repassadas. |
| **3.1. Contrato Único & Tipado de API** | ⚠️ | Cliente centralizado em `apiClient.ts`, porém monolítico, com URLs manuais e rotas com prefixo inconsistente (`/api/...` vs `/...`). |
| **3.2. Sincronização com OpenAPI** | ⚠️ | Schema gerado (`api-schema.d.ts`) baseia-se em `openapi.yaml` desatualizado, exigindo patches manuais com `Omit` em `src/types/index.ts`. |
| **3.3. Tratamento de Falhas na Camada de Rede** | ✅ | `apiFetch` trata status HTTP (400, 401, 403, 404, 409, 422, 429, 500), RFC 7807 problem details e validações de formulário. |
| **3.4. Tipagem Estrita (Zero `any`)** | ⚠️ | `tsc` passa, mas há usos pontuais de `any` em `apiClient.ts:67`, `ClientDashboard.tsx:291` e em blocos `catch (err: any)`. |
| **4.1. Conexão SSE Centralizada** | ❌ | SSE embutido pontualmente em `useAdminNotifications.ts`; cliente não tem SSE (depende de polling no Pix). |
| **4.2. Ciclo de Vida & Cleanup do SSE** | ⚠️ | Conexão é fechada no cleanup, mas dependências instáveis de callbacks no `useEffect` podem causar reconexões desnecessárias. |
| **4.3. Autenticação em SSE** | ✅ | Conecta via `{ withCredentials: true }` utilizando cookie `HttpOnly`; token JWT não vaza em query string da URL. |
| **4.4. Resiliência & Reconexão do SSE** | 🔴 | O listener `onerror` fecha a conexão permanentemente (`close()`) e anula a ref, destruindo a reconexão nativa sem qualquer backoff. |
| **4.5. Comportamento em Abas em Background** | ❌ | Sem tratamento de `document.hidden` ou `visibilitychange`; SSE e polling contínuo do Pix (3.5s) rodam em abas minimizadas. |
| **5.1. Armazenamento de Tokens** | ✅ | JWT é armazenado em cookie `HttpOnly` pelo backend (`credentials: 'include'`). Apenas o perfil do usuário fica no `localStorage`. |
| **5.2. Ciclo de Expiração & 401 Unauthorized** | ⚠️ | Requisições HTTP tratam 401 via callback para deslogar, mas não há refresh token e o SSE 401 falha silenciosamente. E-mail hardcoded no front. |
| **5.3. Proteção de Rotas** | ✅ | Roteamento condicional em `App.tsx` atua adequadamente como UX guard, com segurança real assegurada pelo Spring Security. |
| **5.4. Higienização de Entradas (XSS)** | ✅ | Inexistência de `dangerouslySetInnerHTML`; React escapa dados renderizados. |
| **5.5. Exposição de Segredos** | ✅ | Sem arquivos `.env` com chaves no build; apenas `VITE_API_URL` com fallback limpo para `window.location.origin`. |
| **6.1. Code Splitting & Lazy Loading** | ✅ | Páginas principais (`AuthPage`, `ClientDashboard`, `AdminDashboard`) carregadas via `React.lazy` e `Suspense`. |
| **6.2. Gerenciamento de Re-renders** | ⚠️ | Uso de `useMemo` pontual, mas estados massivos no topo do componente disparam re-renderizações amplas em cascata. |
| **6.3. Estratégia de Abas Ocultas** | ❌ | No `ClientDashboard`, abas inativas são mantidas vivas no DOM com CSS `hidden` (`display: none`), retendo nós e imagens na memória. |
| **6.4. Feedback Visual de Estado** | ✅ | Estados visuais consistentes com spinners, overlays de carregamento, banners de feedback e modais de confirmação. |

---

## Achados

### [F-A6-01] 🔴 Crítico: SSE fecha conexão permanentemente em erro sem reconexão nem backoff
- **Domínio:** A6
- **Tipo:** Fato
- **Local/Evidência:** `frontend/src/hooks/useAdminNotifications.ts:74-79`
  ```typescript
  eventSourceRef.current.onerror = () => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }
  };
  ```
- **Problema:** A API padrão `EventSource` do navegador possui um mecanismo nativo de reconexão automática ao sofrer quedas temporárias de rede. Entretanto, o hook `useAdminNotifications` intercepta o evento `onerror` e invoca imediatamente `.close()`, atribuindo `null` à referência. Não existe qualquer temporizador, contagem de retentativas ou backoff exponencial implementado.
- **Impacto:** Confiabilidade e Tempo Real. Qualquer reinicialização rápida da aplicação no servidor, queda temporária de Wi-Fi ou timeout de conexão persistente de borda (ex.: erro HTTP 524 da Cloudflare após 100 segundos se um heartbeat falhar) encerra de forma irreversível o stream de eventos. O administrador para de receber notificações de novos agendamentos e cancelamentos sem qualquer indicação visual de desconexão até recarregar manualmente a página inteira.
- **Recomendação:** Remover a invocação incondicional de `.close()` no `onerror` para permitir que o navegador execute a reconexão automática nativa, ou implementar um gerenciador de SSE com reconexão resiliente baseada em backoff exponencial (ex: 1s, 2s, 5s, 15s) e indicação de status de conexão na UI.
- **Esforço:** P
- **Relacionados:** A3, A4, A5

---

### [F-A6-02] 🟠 Alto: Ausência de camada de gerenciamento de Server State e cache de dados
- **Domínio:** A6
- **Tipo:** Fato
- **Local/Evidência:** `frontend/src/pages/ClientDashboard.tsx:25-46` e `frontend/src/pages/AdminDashboard.tsx:35-50`
  ```typescript
  // Exemplo em ClientDashboard.tsx:
  const [quadras, setQuadras] = useState<Quadra[]>([]);
  const [horarios, setHorarios] = useState<HorarioDisponivel[]>([]);
  const [meusAgendamentos, setMeusAgendamentos] = useState<Agendamento[]>([]);
  const [bloqueiosQuadra, setBloqueiosQuadra] = useState<BloqueioHorario[]>([]);
  ```
- **Problema:** A aplicação não utiliza nenhuma biblioteca especializada de Server State (como TanStack Query / React Query ou SWR). O estado remoto é gerenciado por dezenas de chamadas imperativas `useState` + `useEffect` isoladas dentro dos componentes de tela.
- **Impacto:** Manutenibilidade e Desempenho. Não há desduplicação de requisições idênticas em trânsito, não há cache compartilhado entre telas, não há revalidação automática em foco de janela e a invalidação pós-mutação exige encadeamento manual de callbacks prop-drilled (`onSuccessAction: () => carregarDados()`).
- **Recomendação:** Introduzir `@tanstack/react-query` para abstrair fetches, chaves de consulta (`queryKey`), cache em memória, mutações e invalidação declarativa de dados do backend.
- **Esforço:** M
- **Relacionados:** A1, A7

---

### [F-A6-03] 🟠 Alto: Ausência de debounce em buscas com risco de sobrecarga e race conditions
- **Domínio:** A6
- **Tipo:** Fato
- **Local/Evidência:** `frontend/src/pages/ClientDashboard.tsx:330-332` e `frontend/src/components/client/CourtSearchBar.tsx:118-132`
  ```typescript
  // ClientDashboard.tsx
  useEffect(() => {
    carregarQuadras(paginaAtual);
  }, [paginaAtual, filtroEsporte, buscaNome, buscaEndereco, modoBusca, coordsAtivas]);
  ```
- **Problema:** Os inputs de texto de filtro de quadra (`buscaNome` e `buscaEndereco`) atualizam o estado pai diretamente no evento `onChange` (`setBuscaNome(e.target.value)`). O `useEffect` dispara uma chamada `quadraApi.listarPaginado` a cada caractere digitado pelo usuário, sem qualquer janela de debounce e sem abortar requisições pendentes via `AbortController`.
- **Impacto:** Desempenho e Consistência. Digitar uma palavra de 15 caracteres dispara 15 requisições HTTP simultâneas contra o backend/banco de dados. Além da sobrecarga de rede, respostas assíncronas que cheguem fora de ordem sobrescreverão a grade com resultados defasados (condição de corrida clássica de autocomplete).
- **Recomendação:** Criar um hook `useDebounce` (300ms a 400ms) para as variáveis de texto de busca antes de disparar o fetch e utilizar o `AbortSignal` já suportado em `quadraApi.listarPaginado`.
- **Esforço:** P
- **Relacionados:** A1, A2

---

### [F-A6-04] 🟠 Alto: Abas ocultas retidas em memória no DOM e polling contínuo sem verificação de visibilidade
- **Domínio:** A6
- **Tipo:** Fato
- **Local/Evidência:** `frontend/src/pages/ClientDashboard.tsx:541,578` e `frontend/src/components/ui/ModalPix.tsx:83-105`
  ```typescript
  // ClientDashboard.tsx
  {/* ABA 1: EXPLORAR QUADRAS */}
  <div className={activeTab === 'QUADRAS' ? 'space-y-6' : 'hidden'}>
    <CourtSearchBar ... />
    <CourtCardGrid ... />
  </div>

  {/* ABA 2: MINHAS RESERVAS */}
  <div className={activeTab === 'AGENDAS' ? 'max-w-4xl mx-auto space-y-6' : 'hidden'}>
    <ClientBookingsList ... />
  </div>
  ```
  ```typescript
  // ModalPix.tsx
  const pollInterval = setInterval(checarStatus, 3500);
  ```
- **Problema:** A navegação interna do cliente não desmonta as abas inativas, apenas oculta-as via classe Tailwind `hidden` (`display: none`). Além disso, o componente `ModalPix` executa sondagem (short polling) a cada 3,5 segundos via `setInterval` sem escutar o evento `visibilitychange` da Page Visibility API.
- **Impacto:** Desempenho e Consumo de Recursos. A aba oculta mantém todos os nós de DOM, imagens de quadras e estados associados na memória do navegador. Em dispositivos móveis ou máquinas com baixa RAM, manter árvores inteiras ocultas gera degradação de renderização. O polling de pagamento continua requisitando o servidor mesmo com o navegador em background ou minimizado.
- **Recomendação:** Desmontar condicionalmente as abas (`{activeTab === 'QUADRAS' && <CourtCardGrid ... />}`) e suspender intervalos de polling no `ModalPix` quando `document.hidden === true`.
- **Esforço:** P
- **Relacionados:** A3, A5

---

### [F-A6-05] 🟡 Médio: Desalinhamento entre contrato OpenAPI e DTOs reais do backend gerando tipagem manual
- **Domínio:** A6
- **Tipo:** Fato
- **Local/Evidência:** `frontend/src/types/index.ts:16-94` vs `docs/api/openapi.yaml` vs `src/main/java/com/agendamentos/equadras/dto/response/UsuarioResponseDTO.java:30`
  ```typescript
  // index.ts: necessidade de override manual com Omit
  export type Usuario = Omit<Schemas['UsuarioResponseDTO'], 'id_usuario' | 'nome_usuario' | 'email_usuario' | 'phone_usuario' | 'role'> & {
    ...
    masterAdmin?: boolean; // Não constava no schema original
  };

  export type Agendamento = Omit<Schemas['AgendamentoResponseDTO'], ...> & {
    transacaoPagamentoId?: string;
    pixCopiaECola?: string;
    qrCodeBase64?: string;
  };
  ```
- **Problema:** O arquivo estático `docs/api/openapi.yaml` que alimenta o comando `npm run generate:api` está defasado em relação aos DTOs Java do Spring Boot (faltam atributos como `masterAdmin`, `transacaoPagamentoId`, `pixCopiaECola`, etc.). Para contornar os erros de compilação, o frontend introduziu overrides manuais com `Omit<Schemas[...]>` em `src/types/index.ts`.
- **Impacto:** Manutenibilidade e Integridade de Contratos. Anula o benefício de ter tipos gerados automaticamente a partir do OpenAPI. Qualquer alteração ou renomeação no backend pode introduzir quebras em runtime que o TypeScript não detectará.
- **Recomendação:** Configurar a geração do `openapi.json` no build Maven do backend e automatizar a execução do `openapi-typescript` na esteira de integração contínua (CI).
- **Esforço:** M
- **Relacionados:** A1, A7

---

### [F-A6-06] 🟡 Médio: Arquitetura técnica monolítica e poluição da camada UI compartilhada com modais de domínio
- **Domínio:** A6
- **Tipo:** Fato
- **Local/Evidência:** Diretório `frontend/src/components/ui/` e `frontend/src/api/apiClient.ts`
- **Problema:** A pasta `components/ui/` mistura primitivos agnósticos de design system (`Button`, `Card`, `Badge`, `Input`) com modais altamente especializados contendo regras de negócio e acoplamento direto aos tipos da aplicação (`BookingModal.tsx`, `ModalPix.tsx`, `ApiKeyModal.tsx`, `CourtDetailsModal.tsx`, `ChangePasswordModal.tsx`). Além disso, `apiClient.ts` é um monolito de 409 linhas unindo 7 domínios em um único arquivo, e as páginas `AdminDashboard.tsx` (803 linhas) e `ClientDashboard.tsx` (666 linhas) concentram orquestrações excessivas.
- **Impacto:** Manutenibilidade e Escalabilidade. Dificulta manutenção paralela, reaproveitamento de componentes e testes unitários isolados.
- **Recomendação:** Adotar arquitetura baseada em features (`features/courts`, `features/bookings`, `features/admin`, `features/auth`), restringindo `components/ui` apenas a componentes puros de apresentação sem dependência de domínio. Dividir `apiClient.ts` em módulos por recurso (`api/courtApi.ts`, `api/bookingApi.ts`, etc.).
- **Esforço:** M
- **Relacionados:** A1

---

### [F-A6-07] 🟡 Médio: Ausência de roteamento baseado em URL e quebra do histórico do navegador
- **Domínio:** A6
- **Tipo:** Fato
- **Local/Evidência:** `frontend/src/App.tsx:23-24, 46-65`
  ```typescript
  const [clientTab, setClientTab] = useState<'QUADRAS' | 'AGENDAS'>('QUADRAS');
  const [adminTab, setAdminTab] = useState<'RELATORIOS' | 'GESTAO_QUADRAS' | 'USUARIOS' | 'AUDITORIA'>('RELATORIOS');
  ```
- **Problema:** Não existe biblioteca de roteamento client-side (como `react-router-dom` ou `@tanstack/react-router`) e nem uso manual da `History API`. Toda a navegação entre visões e abas é efetuada puramente por alteração de estado no componente raiz `App.tsx`.
- **Impacto:** Experiência do Usuário (UX). A URL no navegador permanece estática (`/`). O usuário não pode utilizar os botões "Avançar" ou "Voltar" do navegador (fazer isso causa saída da aplicação); ao pressionar F5 (reload), a navegação é reiniciada para a aba padrão (`QUADRAS` ou `RELATORIOS`); e deep linking para quadras específicas, reservas ou auditoria é impossível.
- **Recomendação:** Introduzir roteamento por URL com rotas bem definidas (`/quadras`, `/reservas`, `/admin/dashboard`, `/admin/quadras`, `/admin/auditoria`), preservando guards apenas como proteção de UX.
- **Esforço:** M
- **Relacionados:** A7

---

### [F-A6-08] 🟡 Médio: Regra de permissão administrativa com e-mail hardcoded no cliente
- **Domínio:** A6
- **Tipo:** Fato
- **Local/Evidência:** `frontend/src/contexts/AuthContext.tsx:82`
  ```typescript
  const isMasterAdmin = user?.role === 'ADMIN' && (user?.masterAdmin === true || user?.email_usuario?.toLowerCase() === 'gui@gmail.com');
  ```
- **Problema:** A condição que concede privilégios de "Master Admin" no frontend inclui uma verificação explícita do endereço de e-mail `'gui@gmail.com'` em código-fonte aberto no bundle distribuído ao navegador.
- **Impacto:** Manutenibilidade e Segurança. Embora o backend valide regras de acesso em endpoints sensíveis, regras de negócio baseadas em identificadores pessoais hardcoded no cliente violam boas práticas, vazam informações de administradores e geram vulnerabilidade operacional caso o e-mail seja alterado.
- **Recomendação:** Remover o fallback com string de e-mail e confiar unicamente no atributo booleano `user.masterAdmin` (ou claims do token) emitido pelo backend.
- **Esforço:** P
- **Relacionados:** A4

---

### [F-A6-09] 🔵 Baixo: Cobertura de testes do frontend praticamente nula
- **Domínio:** A6
- **Tipo:** Fato
- **Local/Evidência:** Execução de `npm test` e diretório `frontend/src/`
- **Problema:** A suíte de testes do frontend possui apenas 1 arquivo (`src/utils/dateUtils.test.ts`) com 3 asserções sobre funções de data. Não há testes de componentes, hooks, contextos ou integração via Mock Service Worker (MSW).
- **Impacto:** Confiabilidade e Risco de Regressão. Qualquer refatoração ou alteração em componentes complexos como a timeline de agendamentos (`AdminDailyTimelineGrid`) ou modal de pagamento (`ModalPix`) depende de validação manual.
- **Recomendação:** Estabelecer testes de integração para componentes críticos usando `@testing-library/react` e mock de chamadas HTTP.
- **Esforço:** M
- **Relacionados:** A7

---

### [F-A6-10] 🔵 Baixo: Acoplamento direto com serviços de terceiros de geocodificação no cliente
- **Domínio:** A6
- **Tipo:** Fato
- **Local/Evidência:** `frontend/src/pages/ClientDashboard.tsx:220-252`
  ```typescript
  const res = await fetch(
    `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=1`,
    { headers: { 'User-Agent': 'eQuadras-App/1.0' }, signal }
  );
  ```
- **Problema:** O frontend faz requisições diretas à API pública do OpenStreetMap Nominatim a partir do navegador do cliente e tenta injetar o header proibido `'User-Agent': 'eQuadras-App/1.0'` (cabeçalho não permitido em navegadores por especificação de segurança do `fetch`, sendo ignorado silenciosamente pelo browser).
- **Impacto:** Confiabilidade e Resiliência. A política de uso do Nominatim proíbe requisições de clientes sem identificação adequada e exige caching rigoroso. Os usuários podem sofrer bloqueios HTTP 403 / rate limit imprevisíveis diretamente no navegador.
- **Recomendação:** Centralizar a geocodificação em um endpoint no backend Spring Boot (`/api/geocoding/cep`), aplicando cache local e política de respeito às diretrizes do Nominatim.
- **Esforço:** P
- **Relacionados:** A1, A5

---

## Sinais para outros domínios

- **Para A1 (Estrutura e Camadas do Backend):**
  - Mapeamento duplo de rotas: o frontend consome endpoints com `/api/` (`/api/admin/auditoria`) e outros sem `/api/` (`/quadras`, `/agendamentos`, `/usuarios/me`). Recomenda-se unificar o padrão de prefixo no Spring Boot.
  - O contrato OpenAPI exportado (`openapi.yaml`) está defasado em relação aos DTOs anotados do backend, gerando esforço de manutenção no front.
- **Para A3 (Concorrência e Tempo Real):**
  - O frontend fecha permanentemente o SSE no `onerror`. Falhas temporárias de rede desconectam os administradores para sempre.
  - Clientes não usam SSE; a tela de pagamento Pix executa short-polling a cada 3,5 segundos (`/pagamentos/{id}/status`), gerando carga periódica no Tomcat/banco de dados.
- **Para A4 (Segurança):**
  - A autenticação via cookies `HttpOnly` com `credentials: 'include'` foi verificada com sucesso, evitando vazamento de JWT no `localStorage` e na URL do SSE.
  - Há um e-mail de administrador hardcoded em `AuthContext.tsx` (`gui@gmail.com`).
- **Para A5 (Infra e Operação):**
  - A estabilidade da conexão SSE depende da configuração de `proxy_buffering off` e timeouts no Nginx, além de evitar que a Cloudflare feche a conexão por inatividade com erro 524.
- **Para A7 (Qualidade, Testes e Entrega):**
  - A base de testes do frontend é nula para componentes e regras de tela (apenas 1 arquivo de teste de utilitário). Falta automação de geração de contratos OpenAPI no CI.

---

## O que não pôde ser verificado e por quê

- **Comportamento dinâmico de reconexão do SSE sob timeout real 524 da Cloudflare:** Não foi executado teste de longa duração (mais de 100 segundos ocioso) contra o domínio público da Cloudflare para não gerar tráfego desnecessário em produção; o achado de falha de reconexão foi comprovado estaticamente pela instrução `eventSource.close()` no listener `onerror` de `useAdminNotifications.ts:74-79`.
- **Consumo de memória (Heap Snapshots do Chrome) em sessões longas:** A auditoria operou em ambiente headless/CLI. O impacto de retenção de abas foi avaliado pela análise estática da estrutura do DOM no JSX (`display: none` / `hidden`).
