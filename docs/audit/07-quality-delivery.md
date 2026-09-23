# 07 — Qualidade, Testes e Entrega (CI/CD)

## 1. Sumário Executivo
Este relatório apresenta a auditoria de qualidade, pirâmide de testes, segurança, automação de build e pipeline de CI/CD da plataforma **eQuadras**. O ecossistema possui uma cobertura robusta de testes unitários e de integração de serviços no backend (Spring Boot), com testes de segurança via MockMvc cobrindo autenticação, autorização, rate limiting e IDOR. No entanto, há lacunas críticas na suíte de testes do frontend (apenas 1 teste unitário), ausência total de Testcontainers (utiliza H2 em memória para testes) e ArchUnit, ausência de ambiente de homologação (staging), e um pipeline de CI/CD que compila o artefato diretamente na VM de produção (`-DskipTests=true`) sem versionamento formal de JAR/container.

---

## 2. Pirâmide de Testes no Backend
- **Testes Unitários:** Ampla cobertura em `src/test/java/com/agendamentos/equadras/service/` (ex: `AgendamentoServiceTest`, `QuadraServiceTest`, `BloqueioHorarioServiceTest`, `ApiKeyServiceTest`).
- **Testes de Integração e Concorrência:** `AgendamentoConcorrenciaIntegrationTest`, `AgendamentoNotificacaoIntegrationTest`, `ContractSafetyNetIntegrationTest`, `QueryCountSafetyNetIntegrationTest`.
- **Banco de Dados em Testes:** Os testes rodam sobre **H2 Database em memória** (`jdbc:h2:mem:testdb`), configurado em `pom.xml` (linhas 73-76). **Testcontainers com PostgreSQL 17 não é utilizado**, o que gera um risco de divergência de dialeto SQL e comportamento de concorrência (ex: restrições GiST / exclusão mútua de horários do Postgres vs H2).
- **Testes de Controller:** Realizados via `MockMvc` integrado ao Spring Security (`SecurityConfigIntegrationTest`, `ApiKeySecurityIntegrationTest`, `AuditoriaSecurityIntegrationTest`).

---

## 3. Testes de Segurança & IDOR
- **Autenticação & Autorização:** Validado rigorosamente em `SecurityConfigIntegrationTest.java`. Rotas protegidas sem token/cookie retornam `401 Unauthorized`. Permissões incorretas de papel (`CLIENT` tentando acessar rotas administrativas como `POST /quadras` ou `DELETE /notificacoes/todas`) retornam `403 Forbidden`.
- **IDOR (Insecure Direct Object Reference):** Coberto em recursos críticos. O teste `getUsuariosOutroIdComClienteDeveRetornar403` em `SecurityConfigIntegrationTest.java` (linhas 197-202) valida que um usuário cliente tentando acessar dados de outro ID (ex: admin) recebe `403 Forbidden`.

---

## 4. Testes de Arquitetura (ArchUnit)
- **Status:** **Ausente.**
- Nenhuma dependência do ArchUnit está presente no `pom.xml`, e nenhum teste de arquitetura estática (verificação de camadas, acoplamento entre service e controller, dependências cíclicas) existe em `src/test/java`.

---

## 5. Qualidade e Testes no Frontend
- **Status de Cobertura:** **Crítico / Insuficiente.**
- Embora o projeto utilize `vitest` e `@testing-library/react` (conforme `frontend/package.json`), foi encontrado **apenas 1 arquivo de teste unitário** em todo o diretório `frontend/src`: `frontend/src/utils/dateUtils.test.ts`.
- **MSW & E2E:** Não há Mock Service Worker (MSW) configurado, nem testes End-to-End (Cypress / Playwright) para os fluxos principais (login, reserva de quadras, pagamento Pix, painel administrativo).

---

## 6. Rastreabilidade Spec -> Teste
- Os planos técnicos em `docs/superpowers/plans/` (ex: `2026-09-23-remediacao-sql-e-logica-negocio.md`) especificam correções de concorrência e segurança.
- Há correspondência direta com testes implementados (ex: `AgendamentoConcorrenciaIntegrationTest` para double-booking e `SecurityConfigIntegrationTest` para rate limit e IDOR). Contudo, a rastreabilidade é mantida manualmente, sem tags ou relatórios automatizados de requisitos x testes.

---

## 7. Pipeline de CI/CD & Deploy
- **Inspecionando `.github/workflows/ci.yml`:**
  - O pipeline executa `backend` (`mvn clean verify`) e `frontend` (`npm ci`, `npm run build`).
  - **Lacunas no CI:** Não executa linting estático, análise de dependências vulneráveis (Snyk/Dependency-Check) nem verificação de contrato OpenAPI.
  - **Deploy Automático no Push (`main`):** Dispara `appleboy/ssh-action` na VM OCI (`137.131.163.62`).
  - **Compilação na Produção:** O script de deploy executa `mvn clean package -DskipTests=true` diretamente na VM, seguido de `sudo systemctl restart equadras-backend.service`. **Os testes são pulados no deploy em produção**, e o JAR em execução não possui versionamento formal de artefato rastreável a uma tag/registry de container.

---

## 8. Ambientes e Gestão de Artefatos
- **Ambientes:** Existe apenas **Produção** (na VM OCI `137.131.163.62`) e **Desenvolvimento Local**. **Não existe ambiente de Homologação (Staging)** separado.
- **Banco de Dados:** Testes usam H2 em memória; Produção usa Supabase PostgreSQL 17 (AWS `sa-east-1`). Não há banco de homologação dedicado.
- **Artefatos:** O JAR gerado em produção é compilado diretamente no servidor via Maven (`target/equadras-0.0.1-SNAPSHOT.jar`), sem artefato imutável versionado em repositório de artefatos (GitHub Packages / Nexus).

---

## 9. Achados Consolidados

### [HIGH] Ausência de Testcontainers com PostgreSQL 17 nos Testes de Integração
- **Severidade:** ALTA
- **Evidência:** `pom.xml` (linhas 73-76) usa H2 (`com.h2database:h2`) para testes.
- **Descrição:** Os testes rodam com H2 em vez do banco real de produção (PostgreSQL 17). Isso cria um risco de falsos positivos/negativos devido a diferenças de dialeto SQL, funções de data e restrições de concorrência (como índices GiST).

### [HIGH] Cobertura Quase Nula de Testes no Frontend
- **Severidade:** ALTA
- **Evidência:** `frontend/src/` possui apenas 1 teste unitário (`dateUtils.test.ts`).
- **Descrição:** Falta total de testes de componentes React, hooks, estado de autenticação e fluxos E2E (Cypress/Playwright), elevando o risco de regressões visuais e de navegação na SPA.

### [MEDIUM] Compilação de Produção Direta na VM com Testes Pulados (`-DskipTests=true`)
- **Severidade:** MÉDIA
- **Evidência:** `.github/workflows/ci.yml` (linha 97).
- **Descrição:** O pipeline de CD compila o projeto diretamente na VM de produção pulando a execução de testes, o que reduz as garantias de integridade do artefato deployado.

### [MEDIUM] Ausência de Testes de Arquitetura (ArchUnit)
- **Severidade:** MÉDIA
- **Evidência:** Ausência de dependência ArchUnit em `pom.xml` e de testes em `src/test/java`.
- **Descrição:** Não há validação automatizada de regras arquiteturais (ex: Controllers não chamarem repositories diretamente, separação estrita de camadas).

### [LOW] Ausência de Ambiente de Homologação (Staging)
- **Severidade:** BAIXA
- **Evidência:** Arquitetura mapeada em `docs/audit/00-context.md` (apenas Produção na VM OCI e ambiente local).
- **Descrição:** Mudanças na `main` vão direto para a VM de produção sem uma validação em ambiente pré-produção idêntico.

---

## 10. Conclusão e Itens ❓
- **Itens ❓ / Abertos:**
  - `?`: Há planos para introduzir Testcontainers no pipeline para garantir testes contra Postgres 17 real sem depender de H2? *(Motivo: Depende de configuração de Docker no runner de CI e ambiente local).*
  - `?`: Existe a intenção de implementar pipeline de testes E2E (Playwright) para validar o fluxo crítico de agendamento e pagamento Pix? *(Motivo: Esforço vs benefício em projeto acadêmico/inicial).*
