# Documentação Completa da API REST - eQuadras

Bem-vindo à documentação técnica oficial da API REST do **eQuadras**. Este documento descreve detalhadamente todos os endpoints disponíveis, fluxos de autenticação, estruturas de requisição/resposta, exemplos práticos e regras de negócio.

A documentação interativa com Swagger UI / OpenAPI 3 está disponível nos seguintes ambientes:
- **Produção (HTTPS):** [`https://equadras.app/swagger-ui/index.html`](https://equadras.app/swagger-ui/index.html)
- **OpenAPI JSON Spec (Produção):** [`https://equadras.app/v3/api-docs`](https://equadras.app/v3/api-docs)
- **Execução Local:** `http://localhost:8080/swagger-ui.html` | `http://localhost:8080/v3/api-docs`

---

## 1. Autenticação & Segurança

A plataforma eQuadras adota uma arquitetura de segurança unificada e baseada em papéis (**RBAC**):

### 1.1 Autenticação da API Externa e Integrações
Todas as rotas da API aceitam chamadas autenticadas com a **API-KEY pessoal** gerada no painel do usuário (formato opaco `eq_...` de alta entropia), através de qualquer um dos cabeçalhos HTTP:
```http
X-API-KEY: eq_a1b2c3d4...
```
ou
```http
Authorization: Bearer eq_a1b2c3d4...
```

**Como obter sua API-Key:**
1. Acesse o portal web e efetue login na sua conta.
2. No menu de perfil ou via `POST /api/usuarios/api-key/regenerar`, emita sua chave pessoal de integração.
3. Utilize essa chave em suas chamadas externas, automações e integrações.

**Controle de Acesso por Papel (RBAC):**
- **Atleta (`ROLE_CLIENT`):** Permissão para consultar quadras, fazer agendamentos, consultar suas próprias reservas (`/api/agendamentos`), consultar dados do seu perfil (`/api/usuarios/me`) e gerenciar sua chave de API. Não tem acesso a rotas administrativas.
- **Administrador (`ROLE_ADMIN`):** Permissões completas de gestão de quadras, fotos, bloqueios, notificações, histórico de reservas de suas quadras e acesso à trilha de auditoria (para Master Admin).

**Exemplo de uso via cURL (Produção):**
```bash
# Consultar quadras com sua API-KEY
curl -X GET "https://equadras.app/api/quadras" \
  -H "X-API-KEY: eq_SUA_CHAVE_AQUI"
```

### 1.2 Sessão Web (Frontend)
Na aplicação web oficial, a autenticação ocorre via cookie seguro `HttpOnly` (`equadras_session`), dispensando armazenamento de credenciais no `localStorage`.

---

## 2. Sumário dos Endpoints

| Módulo | Método | Endpoint | Permissão | Descrição |
|---|---|---|---|---|
| **Usuários** | `POST` | `/api/usuarios` | `ROLE_ADMIN` (somente Master Admin) | Cadastro de novo usuário (`CLIENT` ou `ADMIN`) |
| **Usuários** | `POST` | `/api/usuarios/login` | Público | Autenticação por e-mail e senha; a sessão é emitida no cookie `equadras_session` |
| **Usuários** | `GET` | `/api/usuarios` | `ROLE_ADMIN` (somente Master Admin) | Listar usuários (paginado com `?page`) |
| **Usuários** | `GET` | `/api/usuarios/{id}` | Autenticado | Buscar dados de usuário por ID (o próprio ou Master Admin) |
| **Usuários** | `PUT` | `/api/usuarios/{id}` | `ROLE_ADMIN` (somente Master Admin) | Editar usuário |
| **Usuários** | `DELETE` | `/api/usuarios/{id}` | `ROLE_ADMIN` (somente Master Admin) | Excluir usuário |
| **Usuários** | `POST` | `/api/usuarios/logout` | Público | Encerrar sessão (expira o cookie) |
| **Usuários** | `GET` | `/api/usuarios/me` | Autenticado | Dados do usuário da sessão |
| **Usuários** | `PATCH` | `/api/usuarios/minha-senha` | Autenticado | Alterar a própria senha |
| **Usuários** | `GET` | `/api/usuarios/api-key` | Autenticado | Metadados da API-Key da conta |
| **Usuários** | `POST` | `/api/usuarios/api-key/regenerar` | Autenticado | Gerar ou regenerar a API-Key |
| **Usuários** | `DELETE` | `/api/usuarios/api-key` | Autenticado | Revogar a API-Key |
| **Quadras** | `POST` | `/api/quadras` | `ROLE_ADMIN` | Cadastrar nova quadra com horários e data limite |
| **Quadras** | `GET` | `/api/quadras` | Autenticado (`CLIENT` ou `ADMIN`) | Listar quadras ativas (resumido por padrão em `/api`), filtrar e buscar por raio KM |
| **Quadras** | `GET` | `/api/quadras/{id}` | Autenticado (`CLIENT` ou `ADMIN`) | Buscar detalhes completos e horários da quadra |
| **Quadras** | `PUT` | `/api/quadras/{id}` | `ROLE_ADMIN` | Atualizar dados cadastrais, horários e data limite |
| **Quadras** | `DELETE` | `/api/quadras/{id}` | `ROLE_ADMIN` | Excluir quadra sem histórico de reservas |
| **Quadras** | `POST` | `/api/quadras/{id}/fotos` | `ROLE_ADMIN` | Upload de fotos (multipart/form-data) |
| **Quadras** | `DELETE` | `/api/quadras/{id}/fotos` | `ROLE_ADMIN` | Remover foto da galeria por URL |
| **Quadras** | `PATCH` | `/api/quadras/{id}/status` | `ROLE_ADMIN` | Alternar status ativo/inativo |
| **Quadras** | `GET` | `/api/quadras/fotos` e `/api/quadras/{id}/fotos` | Autenticado (`CLIENT` ou `ADMIN`) | Consultar galerias de fotos por ID ou filtros |
| **Bloqueios** | `POST` | `/api/quadras/{id}/bloqueios` | `ROLE_ADMIN` | Criar bloqueio de dia inteiro ou horário pontual |
| **Bloqueios** | `GET` | `/api/quadras/bloqueios` | `ROLE_ADMIN` | Listar todos os bloqueios de todas as quadras do admin em lote |
| **Bloqueios** | `GET` | `/api/quadras/{id}/bloqueios` | Autenticado (`CLIENT` ou `ADMIN`) | Listar bloqueios ativos da quadra |
| **Bloqueios** | `DELETE`| `/api/quadras/{quadraId}/bloqueios/{bloqueioId}` | `ROLE_ADMIN` | Remover bloqueio por ID |
| **Bloqueios** | `POST` | `/api/quadras/{quadraId}/desbloquear` | `ROLE_ADMIN` | Desbloquear horários/dias via corpo da requisição |
| **Agendamentos** | `GET` | `/api/agendamentos/quadra/{quadraId}/horarios-disponiveis` | Autenticado (`CLIENT` ou `ADMIN`) | Listar grade com status detalhado dos slots da quadra |
| **Agendamentos** | `GET` | `/api/agendamentos/dia` | `ROLE_ADMIN` | Listar horários consolidados de todas as quadras do admin para a data em lote |
| **Agendamentos** | `POST` | `/api/agendamentos` | Autenticado | Criar agendamento sob Lock Pessimista e gerar Pix |
| **Agendamentos** | `POST` | `/api/agendamentos/bot` | Público / Bot | Criar agendamento via WhatsApp/Bot (100% público, sem token/secret) |
| **Agendamentos** | `GET` | `/api/agendamentos/horarios-disponiveis` | Autenticado (`CLIENT` ou `ADMIN`) | Consulta consolidada e flexível de grade de horários por data/esporte/quadra |
| **Agendamentos** | `GET` | `/api/agendamentos/quadra/{quadraId}` | `ROLE_ADMIN` | Listar histórico de reservas de uma quadra específica do admin |
| **Agendamentos** | `GET` | `/api/agendamentos` | Autenticado | Listar reservas do atleta/admin, paginado (`?page=0&aba=ATIVOS`); a variante legada sem `page` aceita `?historico=true` |
| **Agendamentos** | `PATCH`| `/api/agendamentos/{id}/cancelar` | Autenticado | Cancelar agendamento ativo |
| **Agendamentos** | `GET` | `/api/agendamentos/{id}` | Autenticado | Buscar agendamento por ID |
| **Agendamentos** | `GET` | `/api/agendamentos/contadores` | Autenticado | Contadores das próprias reservas por aba |
| **Agendamentos** | `GET` | `/api/agendamentos/quadra/{quadraId}/contadores` | `ROLE_ADMIN` | Contadores das reservas de uma quadra |
| **Agendamentos** | `GET` | `/api/agendamentos/agenda` | `ROLE_ADMIN` | Agenda paginada do dia ou intervalo |
| **Agendamentos** | `GET` | `/api/agendamentos/agenda/contadores` | `ROLE_ADMIN` | Contadores da agenda por aba |
| **Agendamentos** | `GET` | `/api/agendamentos/agenda/completa` | `ROLE_ADMIN` | Agenda completa de um dia (sem paginação) |
| **Agendamentos** | `GET` | `/api/agendamentos/agenda/mensal` | `ROLE_ADMIN` | Agenda do mês |
| **Agendamentos** | `GET` | `/api/agendamentos/dashboard/metricas` | `ROLE_ADMIN` | Métricas do dashboard |
| **Pagamentos** | `POST` | `/api/pagamentos/{id}/simular-aprovacao` | Autenticado | Simular aprovação Pix (Ambiente Dev) |
| **Pagamentos** | `GET` | `/api/pagamentos/{id}/status` | Autenticado | Consultar status de pagamento da reserva |
| **Pagamentos** | `POST` | `/api/pagamentos/webhook` | Público | Webhook de notificações de pagamento |
| **Notificações** | `GET` | `/api/notificacoes/stream` | `ROLE_ADMIN` | Iniciar stream SSE em tempo real de novos pagamentos |
| **Notificações** | `GET` | `/api/notificacoes/admin` | `ROLE_ADMIN` | Histórico paginado de notificações (`?page=0&size=5`) |
| **Notificações** | `PUT` | `/api/notificacoes/{id}/ler` | `ROLE_ADMIN` | Marcar notificação individual como lida |
| **Notificações** | `PUT` | `/api/notificacoes/ler-todas` | `ROLE_ADMIN` | Marcar todas as notificações do administrador como lidas |
| **Notificações** | `DELETE`| `/api/notificacoes/todas` | `ROLE_ADMIN` | Excluir todas as notificações (soft delete) |
| **Auditoria** | `GET` | `/api/admin/auditoria` | `ROLE_ADMIN` (somente Master Admin) | Logs de auditoria paginados e filtráveis |
| **Auditoria** | `GET` | `/api/admin/auditoria/estatisticas` | `ROLE_ADMIN` (somente Master Admin) | Contadores de auditoria de hoje |

---

## 3. Módulo de Usuários e Autenticação

### 3.1 Cadastrar Novo Usuário (Apenas Master Admin)
Cria uma nova conta de usuário (Role: `CLIENT` ou `ADMIN`) no sistema. Exige `ROLE_ADMIN` e o service confirma que o chamador é o **Master Admin** (Administrador Geral); qualquer outro administrador recebe 403. Telefone já cadastrado devolve 409 `TELEFONE_EM_USO`.

- **Método:** `POST`
- **URL:** `/api/usuarios`
- **Autenticação:** Obrigatória (`ROLE_ADMIN`, somente Master Admin)

#### Requisição:
```http
POST /api/usuarios HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN_ADMIN>
Content-Type: application/json

{
  "nome_usuario": "Carlos Silva",
  "email_usuario": "carlos.silva@email.com",
  "senha_usuario": "SenhaForte@123",
  "phone_usuario": "(17) 99876-5432",
  "role": "CLIENT"
}
```

#### Resposta de Sucesso (201 Created):
```json
{
  "id_usuario": 14,
  "nome_usuario": "Carlos Silva",
  "email_usuario": "carlos.silva@email.com",
  "phone_usuario": "(17) 99876-5432",
  "role": "CLIENT",
  "criadoEm": "2026-09-02T16:30:00",
  "masterAdmin": false
}
```

#### Resposta de Erro (403 Forbidden - Administrador que não é o Master Admin):
```json
{
  "type": "https://api.equadras.com/erros/acesso-negado",
  "title": "Acesso Negado",
  "status": 403,
  "detail": "Acesso restrito ao Administrador Geral do sistema.",
  "instance": "/api/usuarios",
  "code": "ACESSO_NEGADO",
  "timestamp": "2026-09-29T14:30:00.123456Z"
}
```

> Um usuário sem `ROLE_ADMIN` é barrado antes, pelo filtro de segurança, com um corpo reduzido: `{"status": 403, "title": "Acesso Proibido", "detail": "..."}`. Sem credencial, a resposta é 401.

#### Resposta de Erro (400 Bad Request - E-mail Já Cadastrado):
```json
{
  "type": "https://api.equadras.com/erros/regra-negocio-violada",
  "title": "Regra de Negócio Violada",
  "status": 400,
  "detail": "E-mail já cadastrado no sistema.",
  "instance": "/api/usuarios",
  "code": "EMAIL_DUPLICADO",
  "timestamp": "2026-09-29T14:30:00.123456Z"
}
```

---

### 3.2 Realizar Login
Autentica o usuário por e-mail e senha. O JWT é gravado no cookie `HttpOnly` `equadras_session` (header `Set-Cookie` da resposta) e o corpo devolve apenas o perfil do usuário logado; **o token não vem no corpo**. Após 5 falhas consecutivas para o mesmo e-mail, novas tentativas recebem 429 com o header `Retry-After`.

- **Método:** `POST`
- **URL:** `/api/usuarios/login`
- **Autenticação:** Pública

#### Requisição:
```http
POST /api/usuarios/login HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "email_usuario": "carlos.silva@email.com",
  "senha_usuario": "senha123"
}
```

#### Resposta de Sucesso (200 OK):
```http
HTTP/1.1 200 OK
Set-Cookie: equadras_session=<JWT>; Path=/; HttpOnly; SameSite=Lax
Content-Type: application/json
```
```json
{
  "id_usuario": 14,
  "nome_usuario": "Carlos Silva",
  "email_usuario": "carlos.silva@email.com",
  "phone_usuario": "(17) 99876-5432",
  "role": "CLIENT",
  "criadoEm": "2026-09-02T16:30:00",
  "masterAdmin": false
}
```

#### Resposta de Erro (400 Bad Request - Credenciais Inválidas):
```json
{
  "type": "https://api.equadras.com/erros/regra-negocio-violada",
  "title": "Regra de Negócio Violada",
  "status": 400,
  "detail": "E-mail ou senha incorretos.",
  "instance": "/api/usuarios/login",
  "code": "CREDENCIAIS_INVALIDAS",
  "timestamp": "2026-09-29T14:30:00.123456Z"
}
```

#### Resposta de Erro (429 Too Many Requests - Excesso de Tentativas):
```http
HTTP/1.1 429 Too Many Requests
Retry-After: 300
Content-Type: application/problem+json
```
```json
{
  "type": "about:blank",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Muitas tentativas falhas de login para esta conta. Tente novamente em 300 segundos."
}
```

---

### 3.3 Listar Usuários (Apenas Master Admin)
Retorna os usuários registrados no sistema. Exige `ROLE_ADMIN` e o service confirma que o chamador é o **Master Admin**.

- **Método:** `GET`
- **URL:** `/api/usuarios?page=0&size=10`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`, somente Master Admin)

**Variantes (definidas pela presença do parâmetro `page`):**
- **Com `page`:** resposta paginada `PageResponse<UsuarioResponseDTO>`. `size` padrão 10, máximo 50. A ordenação é fixa por `id_usuario` crescente: o `sort` enviado pelo cliente é ignorado.
- **Sem `page` (variante legada):** lista simples `array<UsuarioResponseDTO>`, limitada a 200 itens no SQL, sem aviso ao cliente.

#### Requisição:
```http
GET /api/usuarios?page=0&size=10 HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN_ADMIN>
```

#### Resposta de Sucesso (200 OK - com `page`):
```json
{
  "content": [
    {
      "id_usuario": 14,
      "nome_usuario": "Carlos Silva",
      "email_usuario": "carlos.silva@email.com",
      "phone_usuario": "(17) 99876-5432",
      "role": "CLIENT",
      "criadoEm": "2026-09-02T16:30:00",
      "masterAdmin": false
    },
    {
      "id_usuario": 1,
      "nome_usuario": "Administrador Geral",
      "email_usuario": "admin@equadras.com",
      "phone_usuario": "(17) 99999-0000",
      "role": "ADMIN",
      "criadoEm": "2026-01-01T10:00:00",
      "masterAdmin": true
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 2,
  "totalPages": 1
}
```

#### Resposta de Sucesso (200 OK - sem `page`, legado):
```json
[
  {
    "id_usuario": 14,
    "nome_usuario": "Carlos Silva",
    "email_usuario": "carlos.silva@email.com",
    "phone_usuario": "(17) 99876-5432",
    "role": "CLIENT",
    "criadoEm": "2026-09-02T16:30:00",
    "masterAdmin": false
  }
]
```

---

### 3.4 Buscar Usuário por ID
- **Método:** `GET`
- **URL:** `/api/usuarios/{id}`
- **Autenticação:** `Bearer <TOKEN>`

#### Requisição:
```http
GET /api/usuarios/14 HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso (200 OK):
```json
{
  "id_usuario": 14,
  "nome_usuario": "Carlos Silva",
  "email_usuario": "carlos.silva@email.com",
  "phone_usuario": "(17) 99876-5432",
  "role": "CLIENT",
  "criadoEm": "2026-09-02T16:30:00",
  "masterAdmin": false
}
```

Apenas o próprio usuário ou o Master Admin podem consultar. Outro usuário recebe 403; ID inexistente, 404 `USUARIO_NAO_ENCONTRADO`.

---

### 3.5 Editar e Excluir Usuário (Apenas Master Admin)
- **Métodos/URLs:** `PUT /api/usuarios/{id}` e `DELETE /api/usuarios/{id}`
- **Autenticação:** `ROLE_ADMIN`, somente Master Admin (outro administrador recebe 403)

O `PUT` recebe `nome_usuario`, `email_usuario`, `phone_usuario`, `role` e, opcionalmente, `nova_senha` (mínimo de 6 caracteres; omitida, a senha atual é mantida). O e-mail do Master Admin não pode ser alterado e a conta dele não pode ser excluída (400 `OPERACAO_NAO_PERMITIDA`). ID inexistente devolve 404.

#### Requisição (PUT):
```http
PUT /api/usuarios/14 HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN_ADMIN>
Content-Type: application/json

{
  "nome_usuario": "Carlos Silva Souza",
  "email_usuario": "carlos.souza@email.com",
  "phone_usuario": "(17) 99876-5432",
  "role": "CLIENT",
  "nova_senha": "NovaSenha@456"
}
```

#### Resposta de Sucesso (200 OK):
```json
{
  "id_usuario": 14,
  "nome_usuario": "Carlos Silva Souza",
  "email_usuario": "carlos.souza@email.com",
  "phone_usuario": "(17) 99876-5432",
  "role": "CLIENT",
  "criadoEm": "2026-09-02T16:30:00",
  "masterAdmin": false
}
```

O `DELETE` responde `204 No Content`.

---

### 3.6 Encerrar Sessão (Logout)
- **Método:** `POST`
- **URL:** `/api/usuarios/logout`
- **Autenticação:** Pública (funciona com ou sem sessão)

Se houver sessão, revoga os tokens do usuário e registra o logout na auditoria. Sempre expira o cookie de sessão.

#### Resposta de Sucesso:
```http
HTTP/1.1 204 No Content
Set-Cookie: equadras_session=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax
```

---

### 3.7 Dados da Minha Sessão
- **Método:** `GET`
- **URL:** `/api/usuarios/me`
- **Autenticação:** Obrigatória (`ROLE_CLIENT` ou `ROLE_ADMIN`)

Devolve o `UsuarioResponseDTO` do usuário autenticado, no mesmo formato da seção 3.4. Sem credencial, 401.

---

### 3.8 Alterar Minha Senha
- **Método:** `PATCH`
- **URL:** `/api/usuarios/minha-senha`
- **Autenticação:** Obrigatória (`ROLE_CLIENT` ou `ROLE_ADMIN`)

#### Requisição:
```http
PATCH /api/usuarios/minha-senha HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "senhaAtual": "SenhaVelha@123",
  "novaSenha": "SenhaSuperSegura@456"
}
```

A nova senha precisa ter no mínimo 6 caracteres, com 1 letra maiúscula, 1 minúscula, 1 número e 1 símbolo (senão 422), e ser diferente da atual (400 `SENHA_REPETIDA`). Senha atual incorreta devolve 400 `SENHA_INCORRETA`.

#### Resposta de Sucesso:
```http
HTTP/1.1 204 No Content
```

---

### 3.9 Gerenciar a Chave de API (API-Key)
- **Autenticação:** Obrigatória (`ROLE_CLIENT` ou `ROLE_ADMIN`)

| Método | URL | Efeito | Resposta |
|---|---|---|---|
| `GET` | `/api/usuarios/api-key` | Consulta os metadados da chave (nunca a chave em texto plano) | `200` com `ApiKeyInfoDTO` |
| `POST` | `/api/usuarios/api-key/regenerar` | Emite uma nova chave `eq_...` e invalida a anterior na hora | `200` com `ApiKeyCriadaDTO` e `Cache-Control: no-store` |
| `DELETE` | `/api/usuarios/api-key` | Revoga a chave sem gerar outra (idempotente) | `204 No Content` |

A chave em texto plano **só aparece na resposta da regeneração**: guarde-a nesse momento. A regeneração é limitada a 5 por minuto por conta; acima disso, 429 com `Retry-After: 60`.

#### Resposta de Sucesso do `GET` (200 OK):
```json
{
  "possuiChave": true,
  "last4": "a1b2",
  "criadaEm": "2026-09-20T12:00:00Z",
  "ultimoUsoEm": "2026-09-28T18:45:10Z"
}
```

#### Resposta de Sucesso da Regeneração (200 OK):
```json
{
  "apiKey": "eq_9f8e7d6c5b4a39281706f5e4d3c2b1a0",
  "last4": "b1a0",
  "criadaEm": "2026-09-29T14:30:00Z"
}
```

---

## 4. Módulo de Quadras

### 4.1 Cadastrar Nova Quadra
Cria uma nova quadra esportiva definindo nome, modalidade, valor/hora, endereço completo com coordenadas geográficas, **data limite de agendamento** (opcional), até 5 fotos e **grade de funcionamento semanal personalizada** (`disponibilidades`).

- **Método:** `POST`
- **URL:** `/api/quadras`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`)

#### Requisição:
```http
POST /api/quadras HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN_ADMIN>
Content-Type: application/json

{
  "nome": "Arena Central Premium",
  "tipoEsporte": "TENIS",
  "valorHora": 120.00,
  "cep": "15703-050",
  "logradouro": "Rua Dezoito, 1920",
  "bairro": "Jardim América",
  "cidade": "Jales",
  "estado": "SP",
  "latitude": -20.2730,
  "longitude": -50.5398,
  "descricao": "Quadra de saibro premium com amortecimento e iluminação LED profissional.",
  "dataLimiteAgendamento": "2026-12-31",
  "fotos": [
    "https://images.unsplash.com/photo-1554068865-24cecd4e34b8"
  ],
  "disponibilidades": [
    { "diaSemana": "MONDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" },
    { "diaSemana": "TUESDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" },
    { "diaSemana": "WEDNESDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" },
    { "diaSemana": "THURSDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" },
    { "diaSemana": "FRIDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" },
    { "diaSemana": "SATURDAY", "horaInicio": "08:00:00", "horaFim": "20:00:00" },
    { "diaSemana": "SUNDAY", "horaInicio": "08:00:00", "horaFim": "14:00:00" }
  ]
}
```

> **Nota sobre `dataLimiteAgendamento`:** Se preenchida (ex: `2026-12-31`), a API impede qualquer agendamento em datas posteriores, marcando os slots como `BLOQUEADO`.
> **Nota sobre `disponibilidades`:** No cadastro, se omitida ou vazia, o sistema aplica o padrão comercial: Segunda a Domingo, das 06:00:00 às 23:00:00. Dias não incluídos na lista são considerados como **FECHADOS**. O campo `versao` é ignorado no cadastro.

#### Resposta de Sucesso (201 Created):
```json
{
  "id_quadra": 11,
  "nome": "Arena Central Premium",
  "tipoEsporte": "TENIS",
  "valorHora": 120.00,
  "ativa": true,
  "cep": "15703-050",
  "logradouro": "Rua Dezoito, 1920",
  "bairro": "Jardim América",
  "cidade": "Jales",
  "estado": "SP",
  "latitude": -20.273,
  "longitude": -50.5398,
  "descricao": "Quadra de saibro premium com amortecimento e iluminação LED profissional.",
  "dataLimiteAgendamento": "2026-12-31",
  "fotos": [
    "https://images.unsplash.com/photo-1554068865-24cecd4e34b8"
  ],
  "disponibilidades": [
    { "diaSemana": "MONDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" },
    { "diaSemana": "TUESDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" },
    { "diaSemana": "WEDNESDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" },
    { "diaSemana": "THURSDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" },
    { "diaSemana": "FRIDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" },
    { "diaSemana": "SATURDAY", "horaInicio": "08:00:00", "horaFim": "20:00:00" },
    { "diaSemana": "SUNDAY", "horaInicio": "08:00:00", "horaFim": "14:00:00" }
  ],
  "versao": 0
}
```

---

### 4.2 Listar Quadras Ativas / Busca por Proximidade
Retorna as quadras ativas, com filtros opcionais e busca por proximidade (`latitude` + `longitude` + `raioKm`, padrão 2.0 km).

- **Método:** `GET`
- **URL:** `/api/quadras` ou `/api/quadras?latitude=-20.2730&longitude=-50.5398&raioKm=5.0`
- **Autenticação:** Obrigatória (`ROLE_CLIENT` ou `ROLE_ADMIN`)

**Filtros opcionais (texto parcial):** `tipoEsporte`, `nome`, `endereco`, `cidade`, `bairro`, `cep`.

**Formato da resposta.** Em `/api/quadras` o padrão é o formato **resumido**. A primeira regra que se aplica, nesta ordem, define o formato:
1. `resumido=true|false`, quando informado;
2. header `X-Client: frontend` ou `X-View: full` → completo;
3. header `X-View: resumo` ou `summary`, ou `X-Client: api` → resumido;
4. nenhuma das anteriores: URL com `/api/` → resumido.

**Variantes da resposta:**

| Variante | Como obter | Resposta |
|---|---|---|
| Resumida | padrão em `/api/quadras`, ou `resumido=true` | `array<QuadraResumoResponseDTO>`. `page` e `size` são **ignorados**. |
| Completa paginada | `resumido=false` com `page` | `Page<QuadraResponseDTO>`. `size` padrão 6, máximo 50. |
| Completa sem paginação | `resumido=false` sem `page` | `array<QuadraResponseDTO>` |

> **Teto de 200 itens:** as listas sem paginação (resumida e completa sem `page`) devolvem no máximo 200 quadras. O limite é aplicado no SQL e a API não avisa quando há mais resultados. Use filtros ou o formato completo com `page` para percorrer tudo.

#### Requisição:
```http
GET /api/quadras?latitude=-20.2730&longitude=-50.5398&raioKm=5.0 HTTP/1.1
Host: localhost:8080
X-API-KEY: eq_SUA_CHAVE_AQUI
```

#### Resposta de Sucesso (200 OK - resumida):
```json
[
  {
    "id_quadra": 11,
    "nome": "Arena Central Premium",
    "tipoEsporte": "TENIS",
    "valorHora": 120.00,
    "endereco": "Rua Dezoito, 1920, Jardim América - Jales",
    "cep": "15703-050"
  }
]
```

#### Resposta de Sucesso (200 OK - completa paginada: `?resumido=false&page=0&size=6`):
```json
{
  "content": [
    {
      "id_quadra": 11,
      "nome": "Arena Central Premium",
      "tipoEsporte": "TENIS",
      "valorHora": 120.00,
      "ativa": true,
      "cep": "15703-050",
      "logradouro": "Rua Dezoito, 1920",
      "bairro": "Jardim América",
      "cidade": "Jales",
      "estado": "SP",
      "latitude": -20.273,
      "longitude": -50.5398,
      "descricao": "Quadra de saibro premium com amortecimento e iluminação LED profissional.",
      "dataLimiteAgendamento": "2026-12-31",
      "fotos": ["/uploads/quadras/3f2a9c1e-7b4d-4e8a-9c21-5d6f7e8a9b0c.jpg"],
      "disponibilidades": [
        { "diaSemana": "MONDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" }
      ],
      "versao": 0
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 6,
    "sort": { "empty": false, "sorted": true, "unsorted": false },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "totalPages": 1,
  "totalElements": 1,
  "size": 6,
  "number": 0,
  "sort": { "empty": false, "sorted": true, "unsorted": false },
  "first": true,
  "numberOfElements": 1,
  "empty": false
}
```

#### Resposta de Sucesso (200 OK - completa sem paginação: `?resumido=false`):
```json
[
  {
    "id_quadra": 11,
    "nome": "Arena Central Premium",
    "tipoEsporte": "TENIS",
    "valorHora": 120.00,
    "ativa": true,
    "cep": "15703-050",
    "logradouro": "Rua Dezoito, 1920",
    "bairro": "Jardim América",
    "cidade": "Jales",
    "estado": "SP",
    "latitude": -20.273,
    "longitude": -50.5398,
    "descricao": "Quadra de saibro premium com amortecimento e iluminação LED profissional.",
    "dataLimiteAgendamento": "2026-12-31",
    "fotos": ["/uploads/quadras/3f2a9c1e-7b4d-4e8a-9c21-5d6f7e8a9b0c.jpg"],
    "disponibilidades": [
      { "diaSemana": "MONDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" }
    ],
    "versao": 0
  }
]
```

---

### 4.3 Buscar Detalhes da Quadra por ID
- **Método:** `GET`
- **URL:** `/api/quadras/{id}`
- **Autenticação:** Obrigatória (`ROLE_CLIENT` ou `ROLE_ADMIN`)

#### Requisição:
```http
GET /api/quadras/11 HTTP/1.1
Host: localhost:8080
X-API-KEY: eq_SUA_CHAVE_AQUI
```

#### Resposta de Sucesso (200 OK):
```json
{
  "id_quadra": 11,
  "nome": "Arena Central Premium",
  "tipoEsporte": "TENIS",
  "valorHora": 120.00,
  "ativa": true,
  "cep": "15703-050",
  "logradouro": "Rua Dezoito, 1920",
  "bairro": "Jardim América",
  "cidade": "Jales",
  "estado": "SP",
  "latitude": -20.273,
  "longitude": -50.5398,
  "descricao": "Quadra de saibro premium com amortecimento e iluminação LED profissional.",
  "dataLimiteAgendamento": "2026-12-31",
  "fotos": [
    "/uploads/quadras/3f2a9c1e-7b4d-4e8a-9c21-5d6f7e8a9b0c.jpg"
  ],
  "disponibilidades": [
    { "diaSemana": "MONDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" },
    { "diaSemana": "TUESDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" }
  ],
  "versao": 0
}
```

---

### 4.4 Atualizar Quadra
Permite atualizar todas as informações cadastrais, horários semanais e a data limite de agendamentos.

- **Método:** `PUT`
- **URL:** `/api/quadras/{id}`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN` - dono da quadra)

#### Requisição:
```http
PUT /api/quadras/11 HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "nome": "Arena Central Premium (Reformada)",
  "tipoEsporte": "TENIS",
  "valorHora": 130.00,
  "cep": "15703-050",
  "logradouro": "Rua Dezoito, 1920",
  "bairro": "Jardim América",
  "cidade": "Jales",
  "estado": "SP",
  "latitude": -20.2730,
  "longitude": -50.5398,
  "descricao": "Quadra reformada com nova iluminação LED e piso saibro premium.",
  "dataLimiteAgendamento": "2026-12-31",
  "fotos": [],
  "disponibilidades": [
    { "diaSemana": "MONDAY", "horaInicio": "07:00:00", "horaFim": "22:00:00" }
  ],
  "versao": 0
}
```

> **Controle de concorrência (`versao`):** envie no corpo a `versao` lida na última consulta da quadra (`GET /api/quadras/{id}`). Sem `versao`, a API responde 400 `VERSAO_OBRIGATORIA`; com uma versão diferente da atual (outra pessoa editou antes), responde 409 `CONFLITO_VERSAO`. A cada edição salva, a `versao` é incrementada.
>
> **Atenção a `disponibilidades` na edição:** o padrão 06:00–23:00 vale só no cadastro. No `PUT`, omitir o campo (ou enviar `null`) mantém os horários atuais; enviar uma lista substitui todos os horários; e enviar lista vazia (`[]`) **apaga todos**, deixando a quadra fechada em todos os dias.

#### Resposta de Sucesso (200 OK):
```json
{
  "id_quadra": 11,
  "nome": "Arena Central Premium (Reformada)",
  "tipoEsporte": "TENIS",
  "valorHora": 130.00,
  "ativa": true,
  "cep": "15703-050",
  "logradouro": "Rua Dezoito, 1920",
  "bairro": "Jardim América",
  "cidade": "Jales",
  "estado": "SP",
  "latitude": -20.273,
  "longitude": -50.5398,
  "descricao": "Quadra reformada com nova iluminação LED e piso saibro premium.",
  "dataLimiteAgendamento": "2026-12-31",
  "fotos": [],
  "disponibilidades": [
    { "diaSemana": "MONDAY", "horaInicio": "07:00:00", "horaFim": "22:00:00" }
  ],
  "versao": 1
}
```

---

### 4.5 Alternar Status da Quadra (Ativar/Inativar)
- **Método:** `PATCH`
- **URL:** `/api/quadras/{id}/status?ativa=false`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN` - dono da quadra)

#### Requisição:
```http
PATCH /api/quadras/11/status?ativa=false HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso (200 OK):
```json
{
  "id_quadra": 11,
  "nome": "Arena Central Premium",
  "tipoEsporte": "TENIS",
  "valorHora": 120.00,
  "ativa": false,
  "cep": "15703-050",
  "logradouro": "Rua Dezoito, 1920",
  "bairro": "Jardim América",
  "cidade": "Jales",
  "estado": "SP",
  "latitude": -20.273,
  "longitude": -50.5398,
  "descricao": "Quadra de saibro premium com amortecimento e iluminação LED profissional.",
  "dataLimiteAgendamento": "2026-12-31",
  "fotos": [],
  "disponibilidades": [],
  "versao": 1
}
```

---

### 4.6 Upload de Fotos da Quadra
Envia arquivos de imagem (JPEG, PNG, WebP) de até 5MB para a galeria da quadra (máximo 5 fotos).

- **Método:** `POST`
- **URL:** `/api/quadras/{id}/fotos`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN` - dono da quadra)
- **Content-Type:** `multipart/form-data`

#### Requisição:
```http
POST /api/quadras/11/fotos HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW

------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="fotos"; filename="quadra1.jpg"
Content-Type: image/jpeg

<binário da foto>
------WebKitFormBoundary7MA4YWxkTrZu0gW--
```

#### Resposta de Sucesso (200 OK):
```json
{
  "id_quadra": 11,
  "nome": "Arena Central Premium",
  "tipoEsporte": "TENIS",
  "valorHora": 120.00,
  "ativa": true,
  "cep": "15703-050",
  "logradouro": "Rua Dezoito, 1920",
  "bairro": "Jardim América",
  "cidade": "Jales",
  "estado": "SP",
  "latitude": -20.273,
  "longitude": -50.5398,
  "descricao": "Quadra de saibro premium.",
  "dataLimiteAgendamento": "2026-12-31",
  "fotos": [
    "/uploads/quadras/3f2a9c1e-7b4d-4e8a-9c21-5d6f7e8a9b0c.jpg"
  ],
  "disponibilidades": [],
  "versao": 1
}
```

---

### 4.7 Remover Foto da Quadra
- **Método:** `DELETE`
- **URL:** `/api/quadras/{id}/fotos?fotoUrl=/uploads/quadras/3f2a9c1e-7b4d-4e8a-9c21-5d6f7e8a9b0c.jpg`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN` - dono da quadra)

#### Requisição:
```http
DELETE /api/quadras/11/fotos?fotoUrl=/uploads/quadras/3f2a9c1e-7b4d-4e8a-9c21-5d6f7e8a9b0c.jpg HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso (200 OK):
```json
{
  "id_quadra": 11,
  "nome": "Arena Central Premium",
  "tipoEsporte": "TENIS",
  "valorHora": 120.00,
  "ativa": true,
  "cep": "15703-050",
  "logradouro": "Rua Dezoito, 1920",
  "bairro": "Jardim América",
  "cidade": "Jales",
  "estado": "SP",
  "latitude": -20.273,
  "longitude": -50.5398,
  "descricao": "Quadra de saibro premium.",
  "dataLimiteAgendamento": "2026-12-31",
  "fotos": [],
  "disponibilidades": [],
  "versao": 1
}
```

---

### 4.8 Excluir Quadra
Exclui a quadra definitivamente, desde que ela não possua histórico de agendamentos no banco.

- **Método:** `DELETE`
- **URL:** `/api/quadras/{id}`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN` - dono da quadra)

#### Requisição:
```http
DELETE /api/quadras/11 HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso:
```http
HTTP/1.1 204 No Content
```

#### Resposta de Erro (400 Bad Request - quadra com histórico de reservas):
```json
{
  "type": "https://api.equadras.com/erros/operacao-invalida",
  "title": "Operação Não Permitida",
  "status": 400,
  "detail": "Esta quadra não pode ser excluída porque possui agendamentos vinculados (histórico de reservas). Recomendamos inativar a quadra.",
  "instance": "/api/quadras/11",
  "code": "OPERACAO_NAO_PERMITIDA",
  "timestamp": "2026-09-29T14:30:00.123456Z"
}
```

---

### 4.9 Consultar Fotos das Quadras
- **Método:** `GET`
- **URLs:** `/api/quadras/{id}/fotos` ou `/api/quadras/fotos?id=11` (também aceita `quadraId`, `nome`, `nomeQuadra`, `tipoEsporte`, `esporte`, `cidade`, `bairro`)
- **Autenticação:** Obrigatória (`ROLE_CLIENT` ou `ROLE_ADMIN`)

O formato da resposta depende dos parâmetros:

| Chamada | Resposta |
|---|---|
| Com ID (path, `id` ou `quadraId`; nessa ordem de precedência) | Um objeto. ID inexistente devolve 400. |
| Sem ID e sem filtros | Lista com as galerias de todas as quadras (até 200) |
| Filtro com exatamente uma quadra encontrada | Um objeto |
| Filtro com várias quadras encontradas | Lista |
| Filtro sem nenhuma quadra encontrada | `{"fotos": []}` |

`nomeQuadra` é alias de `nome`, e `esporte` de `tipoEsporte`; se ambos vierem, vale o principal.

#### Resposta de Sucesso (200 OK - com ID):
```json
{
  "id_quadra": 11,
  "nome": "Arena Central Premium",
  "tipoEsporte": "TENIS",
  "cidade": "Jales",
  "bairro": "Jardim América",
  "fotos": ["/uploads/quadras/3f2a9c1e-7b4d-4e8a-9c21-5d6f7e8a9b0c.jpg"]
}
```

---

## 5. Módulo de Bloqueios e Desbloqueios

Permite criar suspensões pontuais de funcionamento (manutenções, feriados, reformas) sem alterar o cadastro fixo semanal da quadra.

### 5.1 Criar Bloqueio
- **Método:** `POST`
- **URL:** `/api/quadras/{id}/bloqueios`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN` - dono da quadra)

#### Requisição (Cenário 1: Intervalo de Horários Pontual):
```http
POST /api/quadras/11/bloqueios HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "data": "2026-09-10",
  "horaInicio": "14:00:00",
  "horaFim": "17:00:00",
  "motivo": "Reforma do alambrado",
  "substituirDiaInteiro": false
}
```

**Resposta de Sucesso (201 Created):**
```json
{
  "id": 4,
  "quadraId": 11,
  "data": "2026-09-10",
  "horaInicio": "14:00:00",
  "horaFim": "17:00:00",
  "motivo": "Reforma do alambrado",
  "criadoEm": "2026-09-02T16:45:10"
}
```

#### Requisição (Cenário 2: Dia Inteiro):
```http
POST /api/quadras/11/bloqueios HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "data": "2026-12-25",
  "motivo": "Feriado de Natal",
  "substituirDiaInteiro": false
}
```

**Resposta de Sucesso (201 Created):**
```json
{
  "id": 5,
  "quadraId": 11,
  "data": "2026-12-25",
  "horaInicio": null,
  "horaFim": null,
  "motivo": "Feriado de Natal",
  "criadoEm": "2026-09-02T16:46:00"
}
```

---

### 5.2 Listar Todos os Bloqueios das Quadras do Admin (Lote)
Retorna em uma única chamada HTTP todos os bloqueios ativos e futuros de todas as quadras pertencentes ao administrador autenticado.

- **Método:** `GET`
- **URL:** `/api/quadras/bloqueios`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`)

#### Requisição:
```http
GET /api/quadras/bloqueios HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso (200 OK):
```json
[
  {
    "id": 4,
    "quadraId": 11,
    "data": "2026-09-10",
    "horaInicio": "14:00:00",
    "horaFim": "17:00:00",
    "motivo": "Reforma do alambrado",
    "criadoEm": "2026-09-02T16:45:10"
  },
  {
    "id": 5,
    "quadraId": 11,
    "data": "2026-12-25",
    "horaInicio": null,
    "horaFim": null,
    "motivo": "Feriado de Natal",
    "criadoEm": "2026-09-02T16:46:00"
  }
]
```

---

### 5.3 Listar Bloqueios de uma Quadra Específica
- **Método:** `GET`
- **URL:** `/api/quadras/{id}/bloqueios`
- **Autenticação:** Obrigatória (`ROLE_CLIENT` ou `ROLE_ADMIN`)

#### Requisição:
```http
GET /api/quadras/11/bloqueios HTTP/1.1
Host: localhost:8080
X-API-KEY: eq_SUA_CHAVE_AQUI
```

#### Resposta de Sucesso (200 OK):
```json
[
  {
    "id": 4,
    "quadraId": 11,
    "data": "2026-09-10",
    "horaInicio": "14:00:00",
    "horaFim": "17:00:00",
    "motivo": "Reforma do alambrado",
    "criadoEm": "2026-09-02T16:45:10"
  }
]
```

---

### 5.4 Desbloquear Horários

#### Opção A: Remover por ID do Bloqueio
- **Método:** `DELETE`
- **URL:** `/api/quadras/{quadraId}/bloqueios/{bloqueioId}`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN` - dono da quadra)

```http
DELETE /api/quadras/11/bloqueios/4 HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

**Resposta:**
```http
HTTP/1.1 204 No Content
```

#### Opção B: Desbloquear via Requisição com Dados do Horário/Data
- **Método:** `POST`
- **URL:** `/api/quadras/{quadraId}/desbloquear`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN` - dono da quadra)

```http
POST /api/quadras/11/desbloquear HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "data": "2026-09-10",
  "horaInicio": "14:00:00",
  "horaFim": "17:00:00"
}
```

**Resposta de Sucesso (200 OK):**
```json
{
  "mensagem": "Horários desbloqueados com sucesso.",
  "totalRemovidos": 1
}
```

---

## 6. Módulo de Agendamentos e Horários do Dia

### 6.1 Consultar Grade e Status do Dia
Gera a relação completa de horários de 1 em 1 hora para a data indicada, informando o status operacional:
- `DISPONIVEL`: Horário livre para agendamento.
- `AGENDADO`: Horário já reservado por um atleta.
- `BLOQUEADO`: Horário bloqueado pelo administrador ou após a data limite.
- `INDISPONIVEL`: Horário já transcorrido no dia (passado) ou quadra inativa.

- **Método:** `GET`
- **URL:** `/api/agendamentos/quadra/{quadraId}/horarios-disponiveis?data=2026-09-10`
- **Autenticação:** Obrigatória (`ROLE_CLIENT` ou `ROLE_ADMIN`)

#### Requisição:
```http
GET /api/agendamentos/quadra/11/horarios-disponiveis?data=2026-09-10 HTTP/1.1
Host: localhost:8080
X-API-KEY: eq_SUA_CHAVE_AQUI
```

#### Resposta de Sucesso (200 OK):
```json
[
  {
    "inicio": "14:00:00",
    "fim": "15:00:00",
    "disponivel": false,
    "status": "INDISPONIVEL",
    "motivo": "Horário indisponível (passado)"
  },
  {
    "inicio": "15:00:00",
    "fim": "16:00:00",
    "disponivel": false,
    "status": "AGENDADO",
    "motivo": "Horário ocupado / agendado"
  },
  {
    "inicio": "16:00:00",
    "fim": "17:00:00",
    "disponivel": false,
    "status": "AGENDADO",
    "motivo": "Horário ocupado / agendado"
  },
  {
    "inicio": "17:00:00",
    "fim": "18:00:00",
    "disponivel": true,
    "status": "DISPONIVEL",
    "motivo": "Disponível"
  },
  {
    "inicio": "18:00:00",
    "fim": "19:00:00",
    "disponivel": false,
    "status": "BLOQUEADO",
    "motivo": "Bloqueado: Reforma do alambrado"
  }
]
```

---

### 6.2 Consultar Horários Consolidados do Dia para Todas as Quadras do Admin (Lote)
Retorna em uma única requisição a grade completa com o status de cada horário de todas as quadras ativas pertencentes ao administrador autenticado para a data indicada.

- **Método:** `GET`
- **URL:** `/api/agendamentos/dia?data=2026-09-10`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`)

#### Requisição:
```http
GET /api/agendamentos/dia?data=2026-09-10 HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso (200 OK):
```json
{
  "11": [
    {
      "inicio": "17:00:00",
      "fim": "18:00:00",
      "disponivel": true,
      "status": "DISPONIVEL",
      "motivo": "Disponível"
    },
    {
      "inicio": "18:00:00",
      "fim": "19:00:00",
      "disponivel": false,
      "status": "BLOQUEADO",
      "motivo": "Bloqueado: Reforma do alambrado"
    }
  ]
}
```

---

### 6.3 Criar Agendamento (Com Lock Pessimista e Pix)
Executa a validação de concorrência com bloqueio atômico `PESSIMISTIC_WRITE` na quadra, registra o agendamento `PENDENTE` e gera o payload Pix para pagamento.

- **Método:** `POST`
- **URL:** `/api/agendamentos`
- **Autenticação:** `Bearer <TOKEN>`

#### Requisição:
```http
POST /api/agendamentos HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "quadraId": 11,
  "dataHoraInicio": "2026-09-10T17:00:00",
  "dataHoraFim": "2026-09-10T19:00:00"
}
```

#### Resposta de Sucesso (201 Created):
```json
{
  "id_agendamento": 25,
  "usuarioId": 14,
  "nomeUsuario": "Carlos Silva",
  "telefoneUsuario": "(17) 99876-5432",
  "quadraId": 11,
  "nomeQuadra": "Arena Central Premium",
  "dataHoraInicio": "2026-09-10T17:00:00",
  "dataHoraFim": "2026-09-10T19:00:00",
  "valorTotal": 240.00,
  "status": "PENDENTE",
  "transacaoPagamentoId": "mock-pix-1725299000",
  "pixCopiaECola": "00020126580014BR.GOV.BCB.PIX0136123e4567-e89b-12d3-a456-4266141740005204000053039865406240.005802BR5913eQuadras6009SAO PAULO62070503***6304ABCD",
  "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAAAMgAAADIAQAAAACFIImAAAAAkUlEQVR42u3YMRKDMAxFQc9l7H...",
  "criadoEm": "2026-09-02T16:50:00",
  "canceladoEm": null
}
```

#### Resposta de Erro (409 Conflict - Horário Conflitante):
```json
{
  "type": "https://api.equadras.com/erros/horario-indisponivel",
  "title": "Conflito de Horário",
  "status": 409,
  "detail": "O horário selecionado conflita com outro agendamento já existente ou bloqueado para esta quadra.",
  "instance": "/api/agendamentos",
  "code": "HORARIO_INDISPONIVEL",
  "timestamp": "2026-09-29T14:30:00.123456Z"
}
```

---

### 6.4 Listar Agendamentos do Usuário (Paginado por Aba)
Retorna as reservas realizadas pelo atleta autenticado. Para `ROLE_ADMIN`, retorna as reservas das quadras do administrador; o Master Admin vê todas.

- **Método:** `GET`
- **URL:** `/api/agendamentos?page=0&size=10&aba=ATIVOS`
- **Autenticação:** `Bearer <TOKEN>`

**Parâmetros de query:**
- `page` *(define a variante paginada)*: índice da página, começando em 0.
- `size` *(padrão `10`, máximo `50`)*: itens por página.
- `aba`: `ATIVOS`, `REALIZADOS` ou `CANCELADOS`. **Obrigatória**, exceto com `apenasPendentes=true` (sem nenhuma das duas, a API devolve 400).
- `apenasPendentes` *(padrão `false`)*: se `true`, devolve só reservas `PENDENTE` ainda dentro do prazo de pagamento de 15 minutos.
- `sort`: aceita apenas `dataHoraInicio` e `id` (outro campo devolve 400). Padrão: `dataHoraInicio` crescente na aba `ATIVOS` e decrescente nas demais.

**Variante legada (sem `page`):** devolve uma lista simples `array<AgendamentoResponseDTO>`, limitada a 200 itens no SQL e sem aviso ao cliente. Aceita `historico` (padrão `false`, só reservas ativas; `true`, histórico completo, inclusive realizadas e canceladas). Está marcada como *deprecated*; prefira a rota paginada.

#### Requisição:
```http
GET /api/agendamentos?page=0&size=10&aba=ATIVOS HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso (200 OK):
```json
{
  "content": [
    {
      "id_agendamento": 25,
      "usuarioId": 14,
      "nomeUsuario": "Carlos Silva",
      "telefoneUsuario": "(17) 99876-5432",
      "quadraId": 11,
      "nomeQuadra": "Arena Central Premium",
      "dataHoraInicio": "2026-09-10T17:00:00",
      "dataHoraFim": "2026-09-10T19:00:00",
      "valorTotal": 240.00,
      "status": "CONFIRMADO",
      "transacaoPagamentoId": "mock-pix-1725299000",
      "pixCopiaECola": null,
      "qrCodeBase64": null,
      "criadoEm": "2026-09-02T16:50:00",
      "canceladoEm": null
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 6.5 Listar Reservas por Quadra (Apenas Administrador da Quadra)
Retorna as reservas cadastradas para a quadra especificada. Apenas o administrador proprietário da quadra ou o Master Admin possui permissão.

- **Método:** `GET`
- **URL:** `/api/agendamentos/quadra/{quadraId}?page=0&size=10`
- **Autenticação:** Obrigatória (`ROLE_ADMIN` - via `X-API-KEY` ou Sessão)

**Variantes (definidas pela presença do parâmetro `page`):**
- **Com `page`:** `PageResponse<AgendamentoResponseDTO>`. `size` padrão 10, máximo 50; `aba` (`ATIVOS`, `REALIZADOS`, `CANCELADOS`) é opcional; `sort` aceita apenas `dataHoraInicio` e `id`.
- **Sem `page` (variante legada, *deprecated*):** lista simples `array<AgendamentoResponseDTO>`, limitada a 200 itens no SQL e sem aviso ao cliente.

#### Requisição:
```http
GET /api/agendamentos/quadra/11?page=0&size=10 HTTP/1.1
Host: localhost:8080
X-API-KEY: eq_SUA_API_KEY_ADMIN
```

#### Resposta de Sucesso (200 OK):
```json
{
  "content": [
    {
      "id_agendamento": 25,
      "usuarioId": 14,
      "nomeUsuario": "Carlos Silva",
      "telefoneUsuario": "(17) 99876-5432",
      "quadraId": 11,
      "nomeQuadra": "Arena Central Premium",
      "dataHoraInicio": "2026-09-10T17:00:00",
      "dataHoraFim": "2026-09-10T19:00:00",
      "valorTotal": 240.00,
      "status": "CONFIRMADO",
      "transacaoPagamentoId": "mock-pix-1725299000",
      "pixCopiaECola": null,
      "qrCodeBase64": null,
      "criadoEm": "2026-09-02T16:50:00",
      "canceladoEm": null
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 6.6 Cancelar Agendamento
- **Método:** `PATCH`
- **URL:** `/api/agendamentos/{id}/cancelar`
- **Autenticação:** `Bearer <TOKEN>` (Atleta dono da reserva ou Administrador da quadra)

#### Requisição:
```http
PATCH /api/agendamentos/25/cancelar HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso (200 OK):
```json
{
  "id_agendamento": 25,
  "usuarioId": 14,
  "nomeUsuario": "Carlos Silva",
  "telefoneUsuario": "(17) 99876-5432",
  "quadraId": 11,
  "nomeQuadra": "Arena Central Premium",
  "dataHoraInicio": "2026-09-10T17:00:00",
  "dataHoraFim": "2026-09-10T19:00:00",
  "valorTotal": 240.00,
  "status": "CANCELADO",
  "transacaoPagamentoId": "mock-pix-1725299000",
  "pixCopiaECola": null,
  "qrCodeBase64": null,
  "criadoEm": "2026-09-02T16:50:00",
  "canceladoEm": "2026-09-03T08:00:00"
}
```

---

### 6.7 Agendamento Flexível via Bot / WhatsApp
Permite que bots de atendimento inteligente (WhatsApp/Telegram/IA) reservem quadras passando informações em linguagem flexível (datas como `"amanha"`, `"hoje"`, `"sexta"`, `"15/09"` e horários como `"19h"`, `"19:00"`). Se o cliente não existir, ele é auto-cadastrado no sistema a partir do telefone informado.

> **Integração WhatsApp:** Este endpoint é 100% público e isento de tokens ou cabeçalhos de autenticação (`X-Bot-Secret` não é exigido), facilitando integrações diretas com fluxos de WhatsApp (Twilio, Baileys, Evolution API, Typebot, Z-API, webhooks de IA).

- **Método:** `POST`
- **URL:** `/api/agendamentos/bot`
- **Autenticação:** Pública (sem necessidade de token ou secret)

#### Requisição:
```http
POST /api/agendamentos/bot HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "nomeQuadra": "Arena Society",
  "data": "amanha",
  "horaInicio": "19h",
  "horaFim": "20h",
  "nomeCliente": "Arthur Prado",
  "telefoneCliente": "11999998888"
}
```

#### Resposta de Sucesso (201 Created):
```json
{
  "id_agendamento": 42,
  "usuarioId": 18,
  "nomeUsuario": "Arthur Prado",
  "telefoneUsuario": "11999998888",
  "quadraId": 1,
  "nomeQuadra": "Arena Gol Society",
  "dataHoraInicio": "2026-09-05T19:00:00",
  "dataHoraFim": "2026-09-05T20:00:00",
  "valorTotal": 120.00,
  "status": "PENDENTE",
  "transacaoPagamentoId": "mp-pix-987654321",
  "pixCopiaECola": "00020126580014br.gov.bcb.pix...",
  "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAAA...",
  "criadoEm": "2026-09-04T15:45:00",
  "canceladoEm": null
}
```

---

### 6.8 Consultar Grade Consolidada de Horários (Busca Flexível)
Permite buscar a grade de horários de quadras com suporte a linguagem flexível de datas (`"hoje"`, `"amanha"`, datas ISO), com filtros opcionais por esporte, quadraId e opção de trazer somente horários disponíveis (`apenasDisponiveis=true`).

- **Método:** `GET`
- **URL:** `/api/agendamentos/horarios-disponiveis?data=amanha&tipoEsporte=FUTEBOL&apenasDisponiveis=true`
- **Autenticação:** Obrigatória (`ROLE_CLIENT` ou `ROLE_ADMIN`)

#### Resposta de Sucesso (200 OK):
```json
[
  {
    "id_quadra": 1,
    "nome_quadra": "Arena Gol Society",
    "tipoEsporte": "FUTEBOL",
    "valorHora": 120.00,
    "data": "2026-09-05",
    "horarios": [
      {
        "inicio": "18:00:00",
        "fim": "19:00:00",
        "disponivel": true,
        "status": "DISPONIVEL",
        "motivo": "Disponível"
      },
      {
        "inicio": "20:00:00",
        "fim": "21:00:00",
        "disponivel": true,
        "status": "DISPONIVEL",
        "motivo": "Disponível"
      }
    ]
  }
]
```

> Sem `data` (ou com um valor não reconhecido), a resposta cobre os próximos 14 dias a partir de hoje, com um item por quadra e por dia.

---

### 6.9 Buscar Agendamento por ID
- **Método:** `GET`
- **URL:** `/api/agendamentos/{id}`
- **Autenticação:** Obrigatória (dono da reserva, administrador da quadra ou Master Admin)

Devolve o `AgendamentoResponseDTO` (mesmo formato da seção 6.3). `pixCopiaECola` e `qrCodeBase64` só vêm preenchidos para o dono da reserva e enquanto ela está `PENDENTE`. ID inexistente **ou fora do seu escopo** devolve 404 `AGENDAMENTO_NAO_ENCONTRADO` (a API não devolve 403 aqui).

---

### 6.10 Contadores de Agendamentos
| Método | URL | Permissão | Resposta |
|---|---|---|---|
| `GET` | `/api/agendamentos/contadores` | Autenticado | Próprias reservas por aba: `{"ATIVOS": 3, "REALIZADOS": 12, "CANCELADOS": 1}` |
| `GET` | `/api/agendamentos/quadra/{quadraId}/contadores` | `ROLE_ADMIN` dono da quadra ou Master Admin | Reservas da quadra: `{"TODOS": 16, "ATIVOS": 3, "REALIZADOS": 12, "CANCELADOS": 1}` |

---

### 6.11 Agenda do Administrador
- **Autenticação:** Obrigatória (`ROLE_ADMIN`); considera as quadras do administrador autenticado.

| Método | URL | Parâmetros | Resposta |
|---|---|---|---|
| `GET` | `/api/agendamentos/agenda` | `data` **ou** `inicio`+`fim`; opcionais `quadraId`, `aba`, `page`, `size` (padrão 10, máx. 50), `sort` (`dataHoraInicio` ou `id`) | `PageResponse<AgendamentoResponseDTO>` |
| `GET` | `/api/agendamentos/agenda/contadores` | `data` **ou** `inicio`+`fim`; opcional `quadraId` | `{"ATIVOS": 3, "REALIZADOS": 12, "CANCELADOS": 1}` |
| `GET` | `/api/agendamentos/agenda/completa` | `data` **ou** `inicio`+`fim`; opcional `quadraId` | `array<AgendamentoResponseDTO>` com os não cancelados, sem paginação |
| `GET` | `/api/agendamentos/agenda/mensal` | `ano` (2000 a 2100) e `mes` (1 a 12) obrigatórios; opcional `quadraId` | `array<AgendamentoResponseDTO>` com os não cancelados do mês |

Regras:
- `data` usa o formato `yyyy-MM-dd`; `inicio` e `fim` usam `yyyy-MM-ddTHH:mm:ss`, com intervalo `[inicio, fim)` de no máximo 24 horas.
- Sem `data` e sem o par `inicio`+`fim`, a API devolve 400: `Parâmetro 'data' ou intervalo ('inicio' e 'fim') é obrigatório.`

#### Requisição:
```http
GET /api/agendamentos/agenda?data=2026-09-10&page=0&size=10 HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN_ADMIN>
```

---

### 6.12 Métricas do Dashboard
- **Método:** `GET`
- **URL:** `/api/agendamentos/dashboard/metricas`
- **Autenticação:** Obrigatória (`ROLE_ADMIN`)

#### Resposta de Sucesso (200 OK):
```json
{
  "totalQuadras": 4,
  "quadrasAtivas": 3,
  "totalReservas": 128,
  "faturamentoTotal": 15420.00,
  "reservasHoje": 6
}
```

`totalReservas` conta as reservas não canceladas; `faturamentoTotal` soma as confirmadas (pagas).

---

## 7. Módulo de Pagamentos e Webhooks

### 7.1 Simular Aprovação de Pagamento Pix (Dev)
Transita uma reserva pendente para `CONFIRMADO` e notifica o administrador via SSE em tempo real.

- **Método:** `POST`
- **URL:** `/api/pagamentos/{agendamentoId}/simular-aprovacao`
- **Autenticação:** `Bearer <TOKEN>`

#### Requisição:
```http
POST /api/pagamentos/25/simular-aprovacao HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso (200 OK):
```json
{
  "id_agendamento": 25,
  "usuarioId": 14,
  "nomeUsuario": "Carlos Silva",
  "telefoneUsuario": "(17) 99876-5432",
  "quadraId": 11,
  "nomeQuadra": "Arena Central Premium",
  "dataHoraInicio": "2026-09-10T17:00:00",
  "dataHoraFim": "2026-09-10T19:00:00",
  "valorTotal": 240.00,
  "status": "CONFIRMADO",
  "transacaoPagamentoId": "mock-pix-1725299000",
  "pixCopiaECola": null,
  "qrCodeBase64": null,
  "criadoEm": "2026-09-02T16:50:00",
  "canceladoEm": null
}
```

---

### 7.2 Webhook do Gateway de Pagamento
Endpoint para notificações assíncronas do gateway de pagamentos.

- **Método:** `POST`
- **URL:** `/api/pagamentos/webhook?id=12345678&topic=payment`
- **Autenticação:** Pública

#### Requisição:
```http
POST /api/pagamentos/webhook?id=12345678&topic=payment HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "action": "payment.created",
  "api_version": "v1",
  "data": {
    "id": "12345678"
  },
  "date_created": "2026-09-02T16:52:00Z",
  "type": "payment"
}
```

O pagamento é identificado por query (`topic=payment` ou `type=payment`, com `id` ou `data.id`) ou pelo JSON (`type`/`action` contendo `payment` e `data.id`, ou o campo `id`). A API consulta o pagamento no Mercado Pago e, se estiver `approved`, confirma o agendamento indicado em `external_reference`.

#### Respostas possíveis:

**200 OK - notificação sem ID de pagamento relevante (ignorada):**
```json
{
  "status": "ignored"
}
```

**200 OK - pagamento consultado, ainda não aprovado (ou não encontrado no gateway):**
```json
{
  "status": "received"
}
```

**200 OK - pagamento aprovado e agendamento confirmado:**
```json
{
  "status": "processed",
  "payment_status": "approved"
}
```

**500 Internal Server Error - falha ao confirmar ou consultar o pagamento (o gateway deve reenviar a notificação):**
```json
{
  "status": "error",
  "message": "Falha ao processar confirmação de pagamento. Solicitando retentativa."
}
```

---

### 7.3 Consultar Status de Pagamento da Reserva
- **Método:** `GET`
- **URL:** `/api/pagamentos/{agendamentoId}/status`
- **Autenticação:** Obrigatória (`ROLE_CLIENT` ou `ROLE_ADMIN`)

Devolve o `AgendamentoResponseDTO` da reserva (mesmo formato da seção 6.3); o campo `status` indica `PENDENTE`, `CONFIRMADO` ou `CANCELADO`. Se a reserva ainda está `PENDENTE` e tem `transacaoPagamentoId`, a API consulta o Mercado Pago antes de responder e, se o pagamento já estiver aprovado, confirma a reserva na hora. ID inexistente ou fora do seu escopo devolve 404.

---

## 8. Módulo de Notificações em Tempo Real (SSE)

### 8.1 Streaming SSE de Notificações
Estabelece conexão persistente unidirecional para recebimento de alertas de reservas pagas em tempo real.

- **Método:** `GET`
- **URL:** `/api/notificacoes/stream`
- **Headers:** `Accept: text/event-stream`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`)

A conexão dura até 1 hora e cada administrador mantém uma só conexão: abrir uma nova substitui a anterior.

#### Requisição:
```http
GET /api/notificacoes/stream HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
Accept: text/event-stream
```

#### Resposta de Evento em Streaming:
```http
HTTP/1.1 200 OK
Content-Type: text/event-stream;charset=UTF-8
Transfer-Encoding: chunked

event: notificacao
data: {"id":1,"mensagem":"Novo pagamento aprovado para a quadra Arena Central Premium no valor de R$ 240,00 por Carlos Silva.","lida":false,"dataCriacao":"2026-09-02T16:51:30"}
```

---

### 8.2 Listar Notificações do Administrador
- **Método:** `GET`
- **URL:** `/api/notificacoes/admin`
- **Parâmetros de Query:**
  - `page` *(opcional, padrão `0`)*: Índice da página (base 0).
  - `size` *(opcional, padrão `5`)*: Quantidade de notificações por página.
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`)

#### Requisição:
```http
GET /api/notificacoes/admin?page=0&size=5 HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso (200 OK):
```json
{
  "content": [
    {
      "id": 1,
      "mensagem": "Novo pagamento aprovado para a quadra Arena Central Premium no valor de R$ 240,00 por Carlos Silva.",
      "lida": false,
      "excluida": false,
      "dataCriacao": "2026-09-02T16:51:30"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 5,
    "sort": { "empty": true, "sorted": false, "unsorted": true },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "size": 5,
  "number": 0,
  "sort": { "empty": true, "sorted": false, "unsorted": true },
  "numberOfElements": 1,
  "first": true,
  "empty": false
}
```

---

### 8.3 Marcar Notificação como Lida
- **Método:** `PUT`
- **URL:** `/api/notificacoes/{id}/ler`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`)

#### Requisição:
```http
PUT /api/notificacoes/1/ler HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso:
```http
HTTP/1.1 204 No Content
```

---

### 8.4 Marcar Todas as Notificações como Lidas
Marca todas as notificações recebidas pelo administrador autenticado como lidas em uma única operação.

- **Método:** `PUT`
- **URL:** `/api/notificacoes/ler-todas`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`)

#### Requisição:
```http
PUT /api/notificacoes/ler-todas HTTP/1.1
Host: equadras.app
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso:
```http
HTTP/1.1 204 No Content
```

---

### 8.5 Excluir Todas as Notificações (Soft Delete)
Realiza a remoção lógica (*soft delete*) de todo o histórico de notificações do administrador autenticado, ocultando-as de listagens e contadores.

- **Método:** `DELETE`
- **URL:** `/api/notificacoes/todas`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`)

#### Requisição:
```http
DELETE /api/notificacoes/todas HTTP/1.1
Host: equadras.app
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso:
```http
HTTP/1.1 204 No Content
```

---

## 9. Módulo de Auditoria (Apenas Master Admin)

### 9.1 Listar Logs de Auditoria
- **Método:** `GET`
- **URL:** `/api/admin/auditoria?page=0&size=10`
- **Autenticação:** `ROLE_ADMIN`, somente Master Admin (outro administrador recebe 403)

Página de logs ordenada por `criadoEm` decrescente (ordenação fixa). Filtros opcionais e combináveis:
- `page` *(padrão `0`)* e `size` *(padrão `10`, mínimo 1, máximo 100)*;
- `usuarioId`: ID do usuário que executou a ação;
- `categoria`: `AUTENTICACAO`, `AGENDAMENTO`, `QUADRA`, `USUARIO`, `BLOQUEIO` ou `API_KEY`;
- `acao`: código da ação (ex.: `LOGOUT`);
- `dataInicio` e `dataFim`: instantes ISO 8601 com fuso (ex.: `2026-09-29T00:00:00Z`);
- `busca`: texto livre.

#### Resposta de Sucesso (200 OK):
```json
{
  "content": [
    {
      "id": 981,
      "usuarioId": 10,
      "usuarioEmail": "arthur.prado@email.com",
      "usuarioNome": "Arthur Prado",
      "categoria": "AUTENTICACAO",
      "acao": "LOGOUT",
      "entidade": "USUARIO",
      "recursoId": "10",
      "tipoExecutor": "CLIENTE",
      "detalhes": "Logout efetuado com sucesso.",
      "ip": "203.0.113.10",
      "userAgent": "Mozilla/5.0",
      "criadoEm": "2026-09-29T15:40:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": { "empty": false, "sorted": true, "unsorted": false },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "totalPages": 1,
  "totalElements": 1,
  "size": 10,
  "number": 0,
  "sort": { "empty": false, "sorted": true, "unsorted": false },
  "first": true,
  "numberOfElements": 1,
  "empty": false
}
```

### 9.2 Estatísticas de Auditoria de Hoje
- **Método:** `GET`
- **URL:** `/api/admin/auditoria/estatisticas`
- **Autenticação:** `ROLE_ADMIN`, somente Master Admin

#### Resposta de Sucesso (200 OK):
```json
{
  "totalLoginsHoje": 42,
  "totalFalhasLoginHoje": 3,
  "totalAcoesHoje": 180,
  "totalCancelamentosHoje": 2
}
```

---

## 10. Formato Padrão de Erros (RFC 7807 - Problem Details)

A API adota a especificação RFC 7807 (`application/problem+json`) para todos os erros retornados pelo `GlobalExceptionHandler`:

### 10.1 Erro de Validação de Campos (422 Unprocessable Entity)
Ocorre quando algum atributo do DTO viola anotações de validação (`@NotBlank`, `@Email`, `@Size`, etc.). O `detail` resume os campos e `camposIncorretos` os lista, com o nome do campo já traduzido (ex.: `email_usuario` vira `E-mail`):
```json
{
  "type": "https://api.equadras.com/erros/validacao",
  "title": "Dados Inválidos",
  "status": 422,
  "detail": "Dados inválidos: E-mail: Formato de e-mail inválido; Senha: A senha deve conter no mínimo 6 caracteres",
  "instance": "/api/usuarios",
  "code": "ERRO_VALIDACAO",
  "timestamp": "2026-09-29T14:30:00.123456Z",
  "camposIncorretos": [
    {
      "campo": "E-mail",
      "mensagem": "Formato de e-mail inválido"
    },
    {
      "campo": "Senha",
      "mensagem": "A senha deve conter no mínimo 6 caracteres"
    }
  ]
}
```

### 10.2 Erro de Regra de Negócio (400 Bad Request)
Regras de negócio violadas trazem `code` com o motivo (ex.: `CREDENCIAIS_INVALIDAS`, `EMAIL_DUPLICADO`, `HORARIO_PASSADO`, `VERSAO_OBRIGATORIA`):
```json
{
  "type": "https://api.equadras.com/erros/regra-negocio-violada",
  "title": "Regra de Negócio Violada",
  "status": 400,
  "detail": "E-mail ou senha incorretos.",
  "instance": "/api/usuarios/login",
  "code": "CREDENCIAIS_INVALIDAS",
  "timestamp": "2026-09-29T14:30:00.123456Z"
}
```

Validações simples de entrada (ex.: quadra não encontrada, intervalo inválido, parâmetro ausente) também respondem 400, com `type` `.../erros/bad-request`, `.../erros/parametro-ausente` ou `.../erros/parametro-invalido` e o `code` correspondente (`REQUISICAO_INVALIDA`, `PARAMETRO_AUSENTE`, `PARAMETRO_INVALIDO`).

### 10.3 Erro de Conflito (409 Conflict)
Ocorre em conflitos de horário (`HORARIO_INDISPONIVEL`), telefone já em uso (`TELEFONE_EM_USO`), edição concorrente de quadra (`CONFLITO_VERSAO`), confirmação concorrente de reserva (`CONFLITO_STATUS`) e violações de integridade do banco (ex.: `REGISTRO_DUPLICADO`, `REGISTRO_EM_USO`). Exemplo (horário já ocupado):
```json
{
  "type": "https://api.equadras.com/erros/horario-indisponivel",
  "title": "Conflito de Horário",
  "status": 409,
  "detail": "O horário selecionado conflita com outro agendamento já existente ou bloqueado para esta quadra.",
  "instance": "/api/agendamentos",
  "code": "HORARIO_INDISPONIVEL",
  "timestamp": "2026-09-29T14:30:00.123456Z"
}
```

> A exclusão de uma quadra com histórico de reservas **não** devolve 409: responde 400 `OPERACAO_NAO_PERMITIDA` (ver seção 4.8).

---

## 11. Códigos de Status HTTP

| Código | Significado | Descrição |
|---|---|---|
| `200 OK` | Sucesso | Requisição processada com êxito e corpo de dados retornado. |
| `201 Created` | Criado com Sucesso | Recurso criado com êxito (cadastro de usuário, quadra, agendamento, bloqueio). |
| `204 No Content` | Sem Conteúdo | Operação bem-sucedida sem corpo de resposta (ex.: exclusões, `logout`, `minha-senha`, marcar notificações como lidas). |
| `400 Bad Request` | Requisição Inválida | Violação de regra de negócio, dados inválidos ou horários conflitantes. |
| `401 Unauthorized` | Não Autorizado | Token de autenticação ausente, expirado ou inválido. |
| `403 Forbidden` | Proibido | O usuário logado não possui a role necessária para a ação. |
| `404 Not Found` | Não Encontrado | Recurso com o ID fornecido não foi localizado. |
| `409 Conflict` | Conflito | Horário já ocupado, telefone em uso, versão desatualizada da quadra, confirmação concorrente ou violação de integridade do banco (ver seção 10.3). |
| `422 Unprocessable Entity` | Erro de Validação | Falha nas anotações de validação de campo do corpo da requisição. |
| `429 Too Many Requests` | Muitas Requisições | Limite de tentativas excedido: login (5 falhas consecutivas por e-mail, com `Retry-After`) e regeneração de API-Key (5 por minuto, `Retry-After: 60`). |

