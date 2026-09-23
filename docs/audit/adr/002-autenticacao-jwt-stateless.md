# ADR 002: Autenticação Stateless via JSON Web Tokens (JJWT)

- **Status:** Proposto — retroativo
- **Data:** 2026-09-23
- **Decisores:** Time de Desenvolvimento eQuadras
- **Domínios Afetados:** A1 (Backend Structure), A4 (Security), A6 (Frontend Architecture)

---

## 1. Contexto

A plataforma eQuadras necessita autenticar usuários em uma arquitetura desacoplada (SPA React independente do backend Spring Boot), permitindo que requisições HTTP e streams SSE validem a identidade do usuário.

## 2. Decisão

Adotou-se **autenticação puramente stateless via JWT** assinado com HMAC-SHA256 (`io.jsonwebtoken:jjwt-api:0.12.6`), transmitido via cabeçalho `Authorization: Bearer <token>` e mantido no navegador via `localStorage`.

## 3. Consequências e Riscos Identificados

### Positivas
- **Sem Estado no Servidor:** Dispensa tabela de sessões ativas no banco de dados para cada requisição.
- **Fácil Consumo na API:** Validação rápida de assinatura e claims através de `JwtAuthenticationFilter`.

### Negativas / Riscos Revelados na Auditoria
- **Impossibilidade de Revogação Imediata:** Como o token é stateless e não há denylist nem versionamento de token no banco, um token comprometido permanece válido até sua expiração natural.
- **Divergência de TTL entre Código e Configuração (A1/A4):** `JwtService.java:25` define fallback de 8 horas (`28800000 ms`), enquanto `application.properties:43` define 30 minutos (`1800000 ms`).
- **Armazenamento no `localStorage` (A4/A6):** O token armazenado em `localStorage` fica vulnerável a exfiltração caso ocorra qualquer brecha de Cross-Site Scripting (XSS).
- **Fallbacks com Segredo Inseguro no Código (A1/A4):** Configuração `jwt.secret` com valor default hardcoded no `application.properties`, arriscando forja de tokens caso a variável de ambiente não seja carregada no runtime de produção.
- **Ausência de Refresh Token Seguro:** Não há fluxo rotativo de refresh tokens armazenados com hash em banco e entregues via cookie `HttpOnly`/`Secure`/`SameSite`.
