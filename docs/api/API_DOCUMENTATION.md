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
Para clientes e administradores em produção:
- **Endpoint:** `POST /usuarios/login`
- **Retorno:** Token JWT com expiração configurada (default 8 horas).

---

## 2. Sumário dos Endpoints

| Módulo | Método | Endpoint | Permissão | Descrição |
|---|---|---|---|---|
| **Usuários** | `POST` | `/usuarios` | Público | Cadastro de novo usuário (`CLIENT` ou `ADMIN`) |
| **Usuários** | `POST` | `/usuarios/login` | Público | Autenticação e obtenção de token JWT |
| **Usuários** | `GET` | `/usuarios/me` | Autenticado | Dados cadastrais do usuário autenticado |
| **Quadras** | `POST` | `/quadras` | `ROLE_ADMIN` | Cadastrar nova quadra com horários e data limite |
| **Quadras** | `GET` | `/quadras` | Público | Listar todas as quadras ativas ou filtrar por raio KM |
| **Quadras** | `GET` | `/quadras/{id}` | Público | Buscar detalhes completos e horários da quadra |
| **Quadras** | `PUT` | `/quadras/{id}` | `ROLE_ADMIN` | Atualizar dados cadastrais, horários e data limite |
| **Quadras** | `DELETE` | `/quadras/{id}` | `ROLE_ADMIN` | Excluir quadra sem histórico de reservas |
| **Quadras** | `POST` | `/quadras/{id}/fotos` | `ROLE_ADMIN` | Upload de fotos (multipart/form-data) |
| **Quadras** | `PATCH` | `/quadras/{id}/status` | `ROLE_ADMIN` | Alternar status ativo/inativo |
| **Bloqueios** | `POST` | `/quadras/{id}/bloqueios` | `ROLE_ADMIN` | Criar bloqueio de dia inteiro ou horário pontual |
| **Bloqueios** | `GET` | `/quadras/{id}/bloqueios` | Público | Listar bloqueios ativos da quadra |
| **Bloqueios** | `DELETE`| `/quadras/{quadraId}/bloqueios/{bloqueioId}` | `ROLE_ADMIN` | Remover bloqueio por ID |
| **Bloqueios** | `POST` | `/quadras/{quadraId}/desbloquear` | `ROLE_ADMIN` | Desbloquear horários/dias via corpo da requisição |
| **Agendamentos** | `GET` | `/agendamentos/quadra/{quadraId}/horarios-disponiveis` | Público | Listar grade com status detalhado dos slots |
| **Agendamentos** | `POST` | `/agendamentos` | Autenticado | Criar agendamento sob Lock Pessimista e gerar Pix |
| **Agendamentos** | `GET` | `/agendamentos` | Autenticado | Listar histórico de reservas do atleta ou admin |
| **Agendamentos** | `GET` | `/agendamentos/quadra/{quadraId}/data` | Público | Listar reservas do dia para uma quadra |
| **Agendamentos** | `PATCH`| `/agendamentos/{id}/cancelar` | Autenticado | Cancelar agendamento ativo |
| **Notificações** | `GET` | `/notificacoes/stream` | `ROLE_ADMIN` | Iniciar stream SSE em tempo real de novos pagamentos |

---

## 3. Módulo de Quadras

### 3.1 Cadastrar Nova Quadra
Cria uma nova quadra esportiva definindo nome, modalidade, valor/hora, endereço completo com coordenadas geográficas, **data limite de agendamento** (opcional), até 5 fotos e **grade de funcionamento semanal personalizada** (`disponibilidades`).

- **Método:** `POST`
- **URL:** `/quadras`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`)
- **Corpo da Requisição (JSON):**
```json
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

- **Exemplo de Resposta (201 Created):**
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

### 3.2 Listar Quadras Ativas
- **Método:** `GET`
- **URL:** `/quadras`
- **Query Params Opcionais:**
  - `latitude`: Coordenada latitude do usuário (ex: `-20.2730`).
  - `longitude`: Coordenada longitude do usuário (ex: `-50.5398`).
  - `raioKm`: Raio de busca em KM (default `2.0`).

---

### 3.3 Buscar Detalhes da Quadra por ID
- **Método:** `GET`
- **URL:** `/quadras/{id}`

---

### 3.4 Atualizar Quadra
Permite atualizar todas as informações cadastrais, horários semanais e a data limite de agendamentos.
- **Método:** `PUT`
- **URL:** `/quadras/{id}`
- **Autenticação:** `ROLE_ADMIN` (dono da quadra)
- **Corpo:** Mesma estrutura do `POST /quadras`.

---

### 3.5 Alternar Status da Quadra (Ativar/Inativar)
- **Método:** `PATCH`
- **URL:** `/quadras/{id}/status`
- **Autenticação:** `ROLE_ADMIN` (dono da quadra)

---

## 4. Módulo de Bloqueios e Desbloqueios

Permite criar suspensões pontuais de funcionamento (manutenções, feriados, reformas) sem alterar o cadastro fixo semanal da quadra.

### 4.1 Criar Bloqueio
- **Método:** `POST`
- **URL:** `/quadras/{id}/bloqueios`
- **Autenticação:** `ROLE_ADMIN` (dono da quadra)

#### Exemplo 1: Bloqueio de Horário Pontual
```json
{
  "data": "2026-09-10",
  "horaInicio": "14:00:00",
  "horaFim": "17:00:00",
  "motivo": "Reforma do alambrado"
}
```

#### Exemplo 2: Bloqueio do Dia Todo
```json
{
  "data": "2026-12-25",
  "motivo": "Feriado de Natal"
}
```

---

### 4.2 Listar Bloqueios da Quadra
- **Método:** `GET`
- **URL:** `/quadras/{id}/bloqueios`

---

### 4.3 Desbloquear Horários

#### Opção A: Remover por ID do Bloqueio
- **Método:** `DELETE`
- **URL:** `/quadras/{quadraId}/bloqueios/{bloqueioId}`
- **Resposta:** `204 No Content`

#### Opção B: Desbloquear via Requisição com Dados do Horário/Data
- **Método:** `POST`
- **URL:** `/quadras/{quadraId}/desbloquear`
- **Autenticação:** `ROLE_ADMIN` (dono da quadra)
- **Corpo (JSON):**
```json
{
  "data": "2026-09-10",
  "horaInicio": "14:00:00",
  "horaFim": "17:00:00"
}
```
- **Resposta (200 OK):**
```json
{
  "mensagem": "Horários desbloqueados com sucesso.",
  "totalRemovidos": 1
}
```

---

## 5. Módulo de Agendamentos e Horários do Dia

### 5.1 Consultar Grade e Status do Dia
Gera a relação completa de horários de 1 em 1 hora para a data indicada, informando o status operacional:
- `DISPONIVEL`: Horário livre para agendamento.
- `AGENDADO`: Horário já reservado por um atleta.
- `BLOQUEADO`: Horário bloqueado pelo administrador ou após a data limite.
- `INDISPONIVEL`: Horário já transcorrido no dia (passado) ou quadra inativa.

- **Método:** `GET`
- **URL:** `/agendamentos/quadra/{quadraId}/horarios-disponiveis`
- **Query Params:**
  - `data` (obrigatório, `YYYY-MM-DD`): Ex: `2026-09-02`.

#### Exemplo de Resposta (200 OK):
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

### 5.2 Criar Agendamento (Com Lock Pessimista e Pix)
Executa a validação de concorrência com bloqueio atômico `PESSIMISTIC_WRITE` na quadra, registra o agendamento `PENDENTE` e gera o payload Pix para pagamento.

- **Método:** `POST`
- **URL:** `/agendamentos`
- **Autenticação:** `Bearer <TOKEN>`
- **Corpo (JSON):**
```json
{
  "quadraId": 10,
  "dataHoraInicio": "2026-09-05T17:00:00",
  "dataHoraFim": "2026-09-05T19:00:00"
}
```

- **Resposta (201 Created):**
```json
{
  "id_agendamento": 15,
  "nomeQuadra": "Arena Central Premium",
  "dataHoraInicio": "2026-09-05T17:00:00",
  "dataHoraFim": "2026-09-05T19:00:00",
  "valorTotal": 240.00,
  "status": "PENDENTE",
  "pixCopiaECola": "00020126580014BR.GOV.BCB.PIX...",
  "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAA...",
  "usuarioId": 52
}
```

---

### 5.3 Cancelar Agendamento
- **Método:** `PATCH`
- **URL:** `/agendamentos/{id}/cancelar`
- **Autenticação:** Atleta dono da reserva ou Administrador da quadra.

---

## 6. Notificações em Tempo Real (SSE)

Permite ao painel administrativo escutar eventos instantâneos quando um Pix for compensado.

- **Método:** `GET`
- **URL:** `/notificacoes/stream`
- **Headers:** `Accept: text/event-stream`
- **Autenticação:** `Bearer <TOKEN>` (`ROLE_ADMIN`) ou `?token=<TOKEN>`

---

## 7. Códigos de Status HTTP

| Código | Significado |
|---|---|
| `200 OK` | Requisição processada com sucesso. |
| `201 Created` | Recurso criado com sucesso. |
| `204 No Content` | Exclusão realizada com sucesso. |
| `400 Bad Request` | Dados inválidos, violação de regras de negócio ou conflito de horário. |
| `401 Unauthorized` | Token ausente, inválido ou expirado. |
| `403 Forbidden` | Usuário não tem permissão para o recurso solicitado. |
| `404 Not Found` | Recurso não encontrado. |
