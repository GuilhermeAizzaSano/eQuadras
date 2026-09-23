# Auditoria de Arquitetura - 02: Dados e Persistência

## 1. Schema, Migrações e Tipos

### 🔵 Achado: Migrações Versionadas (Flyway) e DDL-Auto
**Severidade:** 🔵 Baixo (Positivo)
**Evidência:** 
- `src/main/resources/application.properties:21` (`spring.jpa.hibernate.ddl-auto=validate`)
**Justificativa:** O projeto usa o Flyway adequadamente (migrations de V1 a V7) e a propriedade `ddl-auto=validate` previne alterações acidentais de schema pelo Hibernate em produção.

### 🔵 Achado: Constraints e Defesa no Banco
**Severidade:** 🔵 Baixo (Positivo)
**Evidência:** `src/main/resources/db/migration/V7__defesa_temporal_e_constraints.sql:12-18` (uso de `EXCLUDE USING gist`)
**Justificativa:** O schema espelha as regras de negócio fortemente, usando `btree_gist` para prevenir double booking em nível de banco de dados.

### 🟠 Achado: Uso de TIMESTAMP WITHOUT TIME ZONE
**Severidade:** 🟠 Alto
**Evidência:** 
- `src/main/resources/db/migration/V1__baseline_schema.sql:15` (`criado_em TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL`)
- `src/main/java/com/agendamentos/equadras/model/entity/Agendamento.java:30` (uso de `LocalDateTime` para `dataHoraInicio`)
**Justificativa:** O uso de `TIMESTAMP WITHOUT TIME ZONE` aliado ao `LocalDateTime` no Java faz com que os dados dependam do timezone da JVM. O ideal é armazenar como `timestamptz` e usar `Instant` ou `OffsetDateTime`.

### 🟡 Achado: Ausência de Concorrência Otimista (@Version)
**Severidade:** 🟡 Médio
**Evidência:** Busca por `@Version` em `src/main/java/com/agendamentos/equadras/model/entity` não retornou resultados.
**Justificativa:** Sem `@Version`, o Hibernate não detecta automaticamente atualizações perdidas. O sistema depende de constraints ou locks pessimistas, afetando a escalabilidade.

## 2. JPA / Hibernate

### 🔵 Achado: Mitigação de N+1 (EntityGraphs) e Paginação
**Severidade:** 🔵 Baixo (Positivo)
**Evidência:** 
- `src/main/java/com/agendamentos/equadras/repository/AgendamentoRepository.java:31` (`@EntityGraph`)
- `src/main/java/com/agendamentos/equadras/repository/NotificacaoRepository.java:18` (`Pageable`)
**Justificativa:** A listagem evita o problema N+1 delegando joins para o banco via fetch graphs e usa paginação diretamente no SQL.

### 🟠 Achado: Coleção Mapeada como EAGER
**Severidade:** 🟠 Alto
**Evidência:** `src/main/java/com/agendamentos/equadras/model/entity/Quadra.java:63` (`@ElementCollection(fetch = FetchType.EAGER)`)
**Justificativa:** O uso de `EAGER` na coleção `fotos` (List) pode resultar em N+1 acidental em listagens ou queries que não necessitam desse dado.

### 🔵 Achado: Configurações Seguras e SQL Nativo Parametrizado
**Severidade:** 🔵 Baixo (Positivo)
**Evidência:** 
- `src/main/resources/application.properties:49` (`spring.jpa.open-in-view=false`)
- `src/main/java/com/agendamentos/equadras/repository/QuadraRepository.java:37` (Uso seguro de `@Param`)
**Justificativa:** Parâmetros SQL prevenidos contra injeção de SQL e Open-In-View desativado. `@Transactional` aplicado rigorosamente.

## 3. Conexão com o Supabase

### 🟡 Achado: Ausência de prepareThreshold=0 no Transaction Mode
**Severidade:** 🟡 Médio
**Evidência:** `src/main/resources/application.properties:1` (`spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/equadras_db}`)
**Justificativa:** Em caso de uso do Supabase Pooler em Transaction Mode (típico na porta 6543), prepared statements gerados pelo driver quebram, retornando erros do banco. A ausência do parâmetro explícito `?prepareThreshold=0` na URL JDBC fragiliza o deploy e requer configuração minuciosa na injeção da variável de ambiente para não derrubar consultas transacionais.

### 🟡 Achado: Dimensionamento do Pooler (HikariCP) e Rede (IPv4 vs IPv6)
**Severidade:** 🟡 Médio
**Evidência:** 
- `src/main/resources/application.properties:7` (`spring.datasource.hikari.maximum-pool-size=${HIKARI_MAX_POOL_SIZE:5}`)
**Justificativa:** O tamanho de pool máximo de 5 respeita o limite modesto de conexões do plano Free do Supabase, prevenindo o esgotamento precoce, sendo compatível com o Supavisor. Ademais, como o plano Free do Supabase aboliu IPv4 nativo em conexões diretas (exigindo add-on pago ou IPv6), o uso do Supavisor (pooler) torna-se mandatório para garantir resolução via IPv4 a partir da VM (OCI E2.1.micro Ubuntu) caso ela não tenha conectividade IPv6 nativa configurada.

## 4. Exposição do Supabase (Crítico) e Senhas

### 🔴 Achado: Tabelas de Negócio sem RLS Expostas no PostgREST
**Severidade:** 🔴 Crítico
**Evidência:** 
- `src/main/resources/db/migration/V6__rls_novas_tabelas.sql` (aplica RLS apenas nas tabelas de auditoria).
- `src/main/resources/db/migration/V1__baseline_schema.sql` (sem ENABLE ROW LEVEL SECURITY).
**Justificativa:** O Supabase expõe automaticamente o schema `public` via REST (PostgREST). Sem o RLS ativado, usuários com a anon key podem consultar, extrair ou corromper dados de `usuarios`, `agendamentos` e `quadras`, bypassando o Spring Security.

### 🔴 Achado: Aplicação utilizando Superusuário do Banco
**Severidade:** 🔴 Crítico
**Evidência:** `src/main/resources/application.properties:2` (`spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}`)
**Justificativa:** O backend está usando a role `postgres`, com acesso irrestrito ao banco. É mandatório provisionar um usuário de privilégio mínimo.

### 🔴 Achado: Fallback de Credenciais de Dev e Segredos no properties
**Severidade:** 🔴 Crítico
**Evidência:** 
- `application.properties:3` (`spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:***}`)
- `application.properties:42` (`jwt.secret=${JWT_SECRET:***}`)
- `application.properties:69` (`equadras.bot.api-secret=${BOT_API_SECRET:***}`)
**Justificativa:** Utilizar senhas e chaves fracas em formato de "fallback" hardcoded é um risco massivo. Caso a variável de ambiente não seja injetada por erro operacional (ex: erro no bash/systemd), o sistema irá levantar perigosamente em produção usando `123456` para o banco de dados e assinando tokens JWT com chave comprometida.

### 🔵 Achado: Frontend Não Expõe SDK do Supabase
**Severidade:** 🔵 Baixo (Positivo)
**Evidência:** `frontend/package.json` não contém dependências `@supabase/supabase-js`.
**Justificativa:** A comunicação com dados ocorre integralmente através do gateway do Spring Boot, mitigando a dependência do PostgREST pelo cliente.
