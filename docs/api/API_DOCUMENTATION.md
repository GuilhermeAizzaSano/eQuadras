# Documentação Completa da API REST - eQuadras

Bem-vindo à documentação técnica oficial da API REST do **eQuadras**. Este documento descreve detalhadamente todos os endpoints disponíveis, fluxos de autenticação, estruturas de requisição/resposta, exemplos práticos e regras de negócio.

A documentação interativa com Swagger UI / OpenAPI 3 também está disponível em execução local em:
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON Spec:** `http://localhost:8080/v3/api-docs`

---

## 1. Autenticação & Segurança

A API adota o modelo **Stateless** baseado em tokens no cabeçalho HTTP:
```http
Authorization: Bearer <TOKEN>
```

### 1.1 Token Fixo de Administração (Master Admin Token)
Para facilitar testes manuais, integrações com Postman/Insomnia, testes automatizados e chamadas diretas via `curl`, a API disponibiliza um token de autenticação estático com privilégios de `ROLE_ADMIN` vinculado ao administrador padrão do sistema:

```text
equadras_master_admin_token_2026_secret_key_fixed
```

**Exemplo de uso via cURL:**
```bash
curl -X GET "http://localhost:8080/quadras" \
  -H "Authorization: Bearer equadras_master_admin_token_2026_secret_key_fixed"
```

### 1.2 Autenticação Dinâmica via JWT
Para atletas e administradores:
- **Endpoint:** `POST /usuarios/login`
- **Retorno:** Token JWT com expiração configurada (default 8 horas) e dados do perfil.

---

## 2. Sumário dos Endpoints

| Módulo | Método | Endpoint | Permissão | Descrição |
|---|---|---|---|---|
| **Usuários** | `POST` | `/usuarios` | Público | Cadastro de novo usuário (`CLIENT`) |
| **Usuários** | `POST` | `/usuarios/login` | Público | Autenticação e emissão de token JWT |
| **Usuários** | `GET` | `/usuarios` | `ROLE_ADMIN` | Listar todos os usuários do sistema |
| **Usuários** | `GET` | `/usuarios/{id}` | Autenticado | Buscar dados de usuário por ID |
| **Quadras** | `POST` | `/quadras` | `ROLE_ADMIN` | Cadastrar nova quadra com horários e data limite |
| **Quadras** | `GET` | `/quadras` | Público | Listar todas as quadras ativas ou filtrar por raio KM |
| **Quadras** | `GET` | `/quadras/{id}` | Público | Buscar detalhes completos e horários da quadra |
| **Quadras** | `PUT` | `/quadras/{id}` | `ROLE_ADMIN` | Atualizar dados cadastrais, horários e data limite |
| **Quadras** | `DELETE` | `/quadras/{id}` | `ROLE_ADMIN` | Excluir quadra sem histórico de reservas |
| **Quadras** | `POST` | `/quadras/{id}/fotos` | `ROLE_ADMIN` | Upload de fotos (multipart/form-data) |
| **Quadras** | `DELETE` | `/quadras/{id}/fotos` | `ROLE_ADMIN` | Remover foto da galeria por URL |
| **Quadras** | `PATCH` | `/quadras/{id}/status` | `ROLE_ADMIN` | Alternar status ativo/inativo |
| **Bloqueios** | `POST` | `/quadras/{id}/bloqueios` | `ROLE_ADMIN` | Criar bloqueio de dia inteiro ou horário pontual |
| **Bloqueios** | `GET` | `/quadras/bloqueios` | `ROLE_ADMIN` | Listar todos os bloqueios de todas as quadras do admin em lote |
| **Bloqueios** | `GET` | `/quadras/{id}/bloqueios` | Público | Listar bloqueios ativos da quadra |
| **Bloqueios** | `DELETE`| `/quadras/{quadraId}/bloqueios/{bloqueioId}` | `ROLE_ADMIN` | Remover bloqueio por ID |
| **Bloqueios** | `POST` | `/quadras/{quadraId}/desbloquear` | `ROLE_ADMIN` | Desbloquear horários/dias via corpo da requisição |
| **Agendamentos** | `GET` | `/agendamentos/quadra/{quadraId}/horarios-disponiveis` | Público | Listar grade com status detalhado dos slots da quadra |
| **Agendamentos** | `GET` | `/agendamentos/dia` | `ROLE_ADMIN` | Listar horários consolidados de todas as quadras do admin para a data em lote |
| **Agendamentos** | `POST` | `/agendamentos` | Autenticado | Criar agendamento sob Lock Pessimista e gerar Pix |
| **Agendamentos** | `GET` | `/agendamentos` | Autenticado | Listar histórico de reservas do atleta ou admin |
| **Agendamentos** | `GET` | `/agendamentos/quadra/{quadraId}/data` | Público | Listar reservas do dia para uma quadra |
| **Agendamentos** | `PATCH`| `/agendamentos/{id}/cancelar` | Autenticado | Cancelar agendamento ativo |
| **Pagamentos** | `POST` | `/pagamentos/{id}/simular-aprovacao` | Autenticado | Simular aprovação Pix (Ambiente Dev) |
| **Pagamentos** | `POST` | `/pagamentos/webhook` | Público | Webhook de notificações de pagamento |
| **Notificações** | `GET` | `/notificacoes/stream` | `ROLE_ADMIN` | Iniciar stream SSE em tempo real de novos pagamentos |
| **Notificações** | `GET` | `/notificacoes/admin` | `ROLE_ADMIN` | Histórico de notificações do administrador |
| **Notificações** | `PUT` | `/notificacoes/{id}/ler` | `ROLE_ADMIN` | Marcar notificação como lida |

---

## 3. Módulo de Usuários e Autenticação

### 3.1 Cadastrar Novo Usuário (Atleta)
Cria uma nova conta de atleta no sistema e já retorna o perfil criado acompanhado do token JWT pronto para uso.

- **Método:** `POST`
- **URL:** `/usuarios`
- **Autenticação:** Pública

#### Requisição:
```http
POST /usuarios HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "nome_usuario": "Carlos Silva",
  "email_usuario": "carlos.silva@email.com",
  "senha_usuario": "senha123",
  "phone_usuario": "(17) 99876-5432"
}
```

#### Resposta de Sucesso (201 Created):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjYXJsb3Muc2lsdmFAZW1haWwuY29tIiwicm9sZSI6IkNMSUVOVCIsImlkIjoxNCwiaWF0IjoxNzg4MzgwNDAwLCJleHAiOjE3ODg0MDkyMDB9.signature...",
  "usuario": {
    "id_usuario": 14,
    "nome_usuario": "Carlos Silva",
    "email_usuario": "carlos.silva@email.com",
    "phone_usuario": "(17) 99876-5432",
    "role": "CLIENT",
    "criadoEm": "2026-09-02T16:30:00"
  }
}
```

#### Resposta de Erro (400 Bad Request - E-mail Já Cadastrado):
```json
{
  "type": "https://api.equadras.com/erros/bad-request",
  "title": "Requisição Inválida",
  "status": 400,
  "detail": "E-mail já cadastrado no sistema."
}
```

---

### 3.2 Realizar Login
Autentica o usuário por e-mail e senha, retornando o token JWT e as informações do usuário logado.

- **Método:** `POST`
- **URL:** `/usuarios/login`
- **Autenticação:** Pública

#### Requisição:
```http
POST /usuarios/login HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "email_usuario": "carlos.silva@email.com",
  "senha_usuario": "senha123"
}
```

#### Resposta de Sucesso (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjYXJsb3Muc2lsdmFAZW1haWwuY29tIiwicm9sZSI6IkNMSUVOVCIsImlkIjoxNCwiaWF0IjoxNzg4MzgwNDAwLCJleHAiOjE3ODg0MDkyMDB9.signature...",
  "usuario": {
    "id_usuario": 14,
    "nome_usuario": "Carlos Silva",
    "email_usuario": "carlos.silva@email.com",
    "phone_usuario": "(17) 99876-5432",
    "role": "CLIENT",
    "criadoEm": "2026-09-02T16:30:00"
  }
}
```

#### Resposta de Erro (400 Bad Request - Credenciais Inválidas):
```json
{
  "type": "https://api.equadras.com/erros/bad-request",
  "title": "Requisição Inválida",
  "status": 400,
  "detail": "E-mail ou senha incorretos."
}
```

---

### 3.3 Listar Todos os Usuários
Retorna a listagem de todos os usuários registrados no sistema.

- **Método:** `GET`
- **URL:** `/usuarios`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`)

#### Requisição:
```http
GET /usuarios HTTP/1.1
Host: localhost:8080
Authorization: Bearer equadras_master_admin_token_2026_secret_key_fixed
```

#### Resposta de Sucesso (200 OK):
```json
[
  {
    "id_usuario": 1,
    "nome_usuario": "Administrador Geral",
    "email_usuario": "admin@equadras.com",
    "phone_usuario": "(17) 99999-0000",
    "role": "ADMIN",
    "criadoEm": "2026-01-01T10:00:00"
  },
  {
    "id_usuario": 14,
    "nome_usuario": "Carlos Silva",
    "email_usuario": "carlos.silva@email.com",
    "phone_usuario": "(17) 99876-5432",
    "role": "CLIENT",
    "criadoEm": "2026-09-02T16:30:00"
  }
]
```

---

### 3.4 Buscar Usuário por ID
- **Método:** `GET`
- **URL:** `/usuarios/{id}`
- **Autenticação:** `Bearer <TOKEN>`

#### Requisição:
```http
GET /usuarios/14 HTTP/1.1
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
  "criadoEm": "2026-09-02T16:30:00"
}
```

---

## 4. Módulo de Quadras

### 4.1 Cadastrar Nova Quadra
Cria uma nova quadra esportiva definindo nome, modalidade, valor/hora, endereço completo com coordenadas geográficas, **data limite de agendamento** (opcional), até 5 fotos e **grade de funcionamento semanal personalizada** (`disponibilidades`).

- **Método:** `POST`
- **URL:** `/quadras`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`)

#### Requisição:
```http
POST /quadras HTTP/1.1
Host: localhost:8080
Authorization: Bearer equadras_master_admin_token_2026_secret_key_fixed
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
> **Nota sobre `disponibilidades`:** Se omitida ou vazia, o sistema aplica o padrão comercial: Segunda a Domingo, das 06:00:00 às 23:00:00. Dias não incluídos na lista são considerados como **FECHADOS**.

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
  ]
}
```

---

### 4.2 Listar Quadras Ativas / Busca por Proximidade
Retorna a relação de todas as quadras ativas. Permite cálculo e ordenação por proximidade ao enviar parâmetros de geolocalização.

- **Método:** `GET`
- **URL:** `/quadras` ou `/quadras?latitude=-20.2730&longitude=-50.5398&raioKm=5.0`
- **Autenticação:** Pública

#### Requisição:
```http
GET /quadras?latitude=-20.2730&longitude=-50.5398&raioKm=5.0 HTTP/1.1
Host: localhost:8080
```

#### Resposta de Sucesso (200 OK):
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
    "fotos": [
      "/uploads/quadra-11-foto1.jpg"
    ],
    "disponibilidades": [
      { "diaSemana": "MONDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" }
    ]
  }
]
```

---

### 4.3 Buscar Detalhes da Quadra por ID
- **Método:** `GET`
- **URL:** `/quadras/{id}`
- **Autenticação:** Pública

#### Requisição:
```http
GET /quadras/11 HTTP/1.1
Host: localhost:8080
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
    "/uploads/quadra-11-foto1.jpg"
  ],
  "disponibilidades": [
    { "diaSemana": "MONDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" },
    { "diaSemana": "TUESDAY", "horaInicio": "06:00:00", "horaFim": "23:00:00" }
  ]
}
```

---

### 4.4 Atualizar Quadra
Permite atualizar todas as informações cadastrais, horários semanais e a data limite de agendamentos.

- **Método:** `PUT`
- **URL:** `/quadras/{id}`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN` - dono da quadra)

#### Requisição:
```http
PUT /quadras/11 HTTP/1.1
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
  ]
}
```

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
  ]
}
```

---

### 4.5 Alternar Status da Quadra (Ativar/Inativar)
- **Método:** `PATCH`
- **URL:** `/quadras/{id}/status?ativa=false`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN` - dono da quadra)

#### Requisição:
```http
PATCH /quadras/11/status?ativa=false HTTP/1.1
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
  "disponibilidades": []
}
```

---

### 4.6 Upload de Fotos da Quadra
Envia arquivos de imagem (JPEG, PNG, WebP) de até 5MB para a galeria da quadra (máximo 5 fotos).

- **Método:** `POST`
- **URL:** `/quadras/{id}/fotos`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN` - dono da quadra)
- **Content-Type:** `multipart/form-data`

#### Requisição:
```http
POST /quadras/11/fotos HTTP/1.1
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
    "/uploads/quadra_11_foto1_1725298800000.jpg"
  ],
  "disponibilidades": []
}
```

---

### 4.7 Remover Foto da Quadra
- **Método:** `DELETE`
- **URL:** `/quadras/{id}/fotos?fotoUrl=/uploads/quadra_11_foto1_1725298800000.jpg`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN` - dono da quadra)

#### Requisição:
```http
DELETE /quadras/11/fotos?fotoUrl=/uploads/quadra_11_foto1_1725298800000.jpg HTTP/1.1
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
  "fotos": []
}
```

---

### 4.8 Excluir Quadra
Exclui a quadra definitivamente, desde que ela não possua histórico de agendamentos no banco.

- **Método:** `DELETE`
- **URL:** `/quadras/{id}`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN` - dono da quadra)

#### Requisição:
```http
DELETE /quadras/11 HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso:
```http
HTTP/1.1 204 No Content
```

#### Resposta de Conflito (409 Conflict):
```json
{
  "type": "https://api.equadras.com/erros/conflito-integridade",
  "title": "Conflito de Integridade de Dados",
  "status": 409,
  "detail": "Não é possível excluir este registro pois ele possui agendamentos ou vínculos no banco de dados. Utilize a opção de inativar a quadra."
}
```

---

## 5. Módulo de Bloqueios e Desbloqueios

Permite criar suspensões pontuais de funcionamento (manutenções, feriados, reformas) sem alterar o cadastro fixo semanal da quadra.

### 5.1 Criar Bloqueio
- **Método:** `POST`
- **URL:** `/quadras/{id}/bloqueios`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN` - dono da quadra)

#### Requisição (Cenário 1: Intervalo de Horários Pontual):
```http
POST /quadras/11/bloqueios HTTP/1.1
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
POST /quadras/11/bloqueios HTTP/1.1
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
- **URL:** `/quadras/bloqueios`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`)

#### Requisição:
```http
GET /quadras/bloqueios HTTP/1.1
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
- **URL:** `/quadras/{id}/bloqueios`
- **Autenticação:** Pública

#### Requisição:
```http
GET /quadras/11/bloqueios HTTP/1.1
Host: localhost:8080
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
- **URL:** `/quadras/{quadraId}/bloqueios/{bloqueioId}`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN` - dono da quadra)

```http
DELETE /quadras/11/bloqueios/4 HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

**Resposta:**
```http
HTTP/1.1 204 No Content
```

#### Opção B: Desbloquear via Requisição com Dados do Horário/Data
- **Método:** `POST`
- **URL:** `/quadras/{quadraId}/desbloquear`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN` - dono da quadra)

```http
POST /quadras/11/desbloquear HTTP/1.1
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
- **URL:** `/agendamentos/quadra/{quadraId}/horarios-disponiveis?data=2026-09-10`
- **Autenticação:** Pública

#### Requisição:
```http
GET /agendamentos/quadra/11/horarios-disponiveis?data=2026-09-10 HTTP/1.1
Host: localhost:8080
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
- **URL:** `/agendamentos/dia?data=2026-09-10`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`)

#### Requisição:
```http
GET /agendamentos/dia?data=2026-09-10 HTTP/1.1
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
- **URL:** `/agendamentos`
- **Autenticação:** `Bearer <TOKEN>`

#### Requisição:
```http
POST /agendamentos HTTP/1.1
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
  "criadoEm": "2026-09-02T16:50:00"
}
```

#### Resposta de Erro (400 Bad Request - Horário Conflitante):
```json
{
  "type": "https://api.equadras.com/erros/bad-request",
  "title": "Requisição Inválida",
  "status": 400,
  "detail": "Horário indisponível: já existe um agendamento confirmado ou pendente para este intervalo."
}
```

---

### 6.4 Listar Histórico de Agendamentos do Usuário
Retorna o histórico de todas as reservas realizadas pelo atleta autenticado ou pelas quadras do admin logado.

- **Método:** `GET`
- **URL:** `/agendamentos`
- **Autenticação:** `Bearer <TOKEN>`

#### Requisição:
```http
GET /agendamentos HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso (200 OK):
```json
[
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
    "pixCopiaECola": "00020126580014BR.GOV.BCB.PIX...",
    "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAA...",
    "criadoEm": "2026-09-02T16:50:00"
  }
]
```

---

### 6.5 Listar Reservas por Quadra e Data
- **Método:** `GET`
- **URL:** `/agendamentos/quadra/{quadraId}/data?data=2026-09-10`
- **Autenticação:** Pública

#### Requisição:
```http
GET /agendamentos/quadra/11/data?data=2026-09-10 HTTP/1.1
Host: localhost:8080
```

#### Resposta de Sucesso (200 OK):
```json
[
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
    "criadoEm": "2026-09-02T16:50:00"
  }
]
```

---

### 6.6 Cancelar Agendamento
- **Método:** `PATCH`
- **URL:** `/agendamentos/{id}/cancelar`
- **Autenticação:** `Bearer <TOKEN>` (Atleta dono da reserva ou Administrador da quadra)

#### Requisição:
```http
PATCH /agendamentos/25/cancelar HTTP/1.1
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
  "pixCopiaECola": "00020126580014BR.GOV.BCB.PIX...",
  "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAA...",
  "criadoEm": "2026-09-02T16:50:00"
}
```

---

## 7. Módulo de Pagamentos e Webhooks

### 7.1 Simular Aprovação de Pagamento Pix (Dev)
Transita uma reserva pendente para `CONFIRMADO` e notifica o administrador via SSE em tempo real.

- **Método:** `POST`
- **URL:** `/pagamentos/{agendamentoId}/simular-aprovacao`
- **Autenticação:** `Bearer <TOKEN>`

#### Requisição:
```http
POST /pagamentos/25/simular-aprovacao HTTP/1.1
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
  "pixCopiaECola": "00020126580014BR.GOV.BCB.PIX...",
  "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAA...",
  "criadoEm": "2026-09-02T16:50:00"
}
```

---

### 7.2 Webhook do Gateway de Pagamento
Endpoint para notificações assíncronas do gateway de pagamentos.

- **Método:** `POST`
- **URL:** `/pagamentos/webhook?id=12345678&topic=payment`
- **Autenticação:** Pública

#### Requisição:
```http
POST /pagamentos/webhook?id=12345678&topic=payment HTTP/1.1
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

#### Resposta de Sucesso (200 OK):
```json
{
  "status": "received"
}
```

---

## 8. Módulo de Notificações em Tempo Real (SSE)

### 8.1 Streaming SSE de Notificações
Estabelece conexão persistente unidirecional para recebimento de alertas de reservas pagas em tempo real.

- **Método:** `GET`
- **URL:** `/notificacoes/stream`
- **Headers:** `Accept: text/event-stream`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`) ou `?token=<TOKEN>`

#### Requisição:
```http
GET /notificacoes/stream HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
Accept: text/event-stream
```

#### Resposta de Evento em Streaming:
```http
HTTP/1.1 200 OK
Content-Type: text/event-stream;charset=UTF-8
Transfer-Encoding: chunked

data: {"id":1,"mensagem":"Novo pagamento aprovado para a quadra Arena Central Premium no valor de R$ 240,00 por Carlos Silva.","lida":false,"dataCriacao":"2026-09-02T16:51:30"}
```

---

### 8.2 Listar Notificações do Administrador
- **Método:** `GET`
- **URL:** `/notificacoes/admin`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`)

#### Requisição:
```http
GET /notificacoes/admin HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso (200 OK):
```json
[
  {
    "id": 1,
    "mensagem": "Novo pagamento aprovado para a quadra Arena Central Premium no valor de R$ 240,00 por Carlos Silva.",
    "lida": false,
    "dataCriacao":"2026-09-02T16:51:30"
  }
]
```

---

### 8.3 Marcar Notificação como Lida
- **Método:** `PUT`
- **URL:** `/notificacoes/{id}/ler`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`)

#### Requisição:
```http
PUT /notificacoes/1/ler HTTP/1.1
Host: localhost:8080
Authorization: Bearer <TOKEN>
```

#### Resposta de Sucesso:
```http
HTTP/1.1 204 No Content
```

---

## 9. Formato Padrão de Erros (RFC 7807 - Problem Details)

A API adota a especificação RFC 7807 (`application/problem+json`) para todos os erros retornados pelo `GlobalExceptionHandler`:

### 9.1 Erro de Validação de Campos (422 Unprocessable Entity)
Ocorre quando algum atributo do DTO viola anotações de validação (`@NotBlank`, `@Email`, `@Size`, etc.):
```json
{
  "type": "https://api.equadras.com/erros/validacao",
  "title": "Erro de Validação",
  "status": 422,
  "detail": "Erro de validação nos campos informados.",
  "camposIncorretos": [
    {
      "campo": "email_usuario",
      "mensagem": "Formato de e-mail inválido"
    },
    {
      "campo": "senha_usuario",
      "mensagem": "A senha deve conter no mínimo 6 caracteres"
    }
  ]
}
```

### 9.2 Erro de Regra de Negócio (400 Bad Request)
```json
{
  "type": "https://api.equadras.com/erros/bad-request",
  "title": "Requisição Inválida",
  "status": 400,
  "detail": "E-mail ou senha incorretos."
}
```

### 9.3 Erro de Conflito de Integridade de Dados (409 Conflict)
```json
{
  "type": "https://api.equadras.com/erros/conflito-integridade",
  "title": "Conflito de Integridade de Dados",
  "status": 409,
  "detail": "Não é possível excluir este registro pois ele possui agendamentos ou vínculos no banco de dados. Utilize a opção de inativar a quadra."
}
```

---

## 10. Códigos de Status HTTP

| Código | Significado | Descrição |
|---|---|---|
| `200 OK` | Sucesso | Requisição processada com êxito e corpo de dados retornado. |
| `201 Created` | Criado com Sucesso | Recurso criado com êxito (cadastro de usuário, quadra, agendamento, bloqueio). |
| `204 No Content` | Sem Conteúdo | Operação bem-sucedida que não requer corpo de resposta (ex: exclusões ou leituras). |
| `400 Bad Request` | Requisição Inválida | Violação de regra de negócio, dados inválidos ou horários conflitantes. |
| `401 Unauthorized` | Não Autorizado | Token de autenticação ausente, expirado ou inválido. |
| `403 Forbidden` | Proibido | O usuário logado não possui a role necessária para a ação. |
| `404 Not Found` | Não Encontrado | Recurso com o ID fornecido não foi localizado. |
| `409 Conflict` | Conflito de Integridade | O registro possui vínculos associados que impedem a exclusão física. |
| `422 Unprocessable Entity` | Erro de Validação | Falha nas anotações de validação de campo do corpo da requisição. |

