# ADR 003: Uso de PostgreSQL Gerenciado no Supabase (AWS sa-east-1)

- **Status:** Proposto — retroativo
- **Data:** 2026-09-23
- **Decisores:** Time de Desenvolvimento eQuadras
- **Domínios Afetados:** A2 (Data & Persistence), A4 (Security), A5 (Infra & Operations)

---

## 1. Contexto

A plataforma necessitava de um banco de dados relacional robusto (PostgreSQL 17) sem o custo e a complexidade operacional de manter uma instância de banco autogerenciada dentro da VM OCI de 1 GB de RAM.

## 2. Decisão

Adotou-se o **Supabase Database (PostgreSQL 17 no Free Tier)** hospedado na região AWS `sa-east-1` (São Paulo), conectado a partir da VM OCI (também em São Paulo) via JDBC pooler HikariCP.

## 3. Consequências e Riscos Identificados

### Positivas
- **Desoneração da VM:** Economiza os escassos recursos de memória (1 GB RAM) e disco da VM Always Free da Oracle Cloud.
- **Recursos Nativos Avançados:** Suporte a extensões modernas como `btree_gist` para exclusão temporal de agendamentos.
- **Baixa Latência Intra-Região:** Comunicação entre OCI SP e AWS SP com latências de rede aceitáveis (< 15-20 ms).

### Negativas / Riscos Revelados na Auditoria
- **Exposição do Schema Public via PostgREST (A2/A4):** O Supabase expõe por padrão uma API REST pública sobre o schema `public`. As tabelas de negócio do eQuadras (geradas no Flyway V1) foram criadas sem RLS (Row Level Security) e com conexão pela role superuser `postgres`. Qualquer usuário de posse da anon key pública do projeto Supabase pode contornar o Spring Security e consultar ou mutar dados diretamente.
- **Compatibilidade com Supavisor / Transaction Mode (A2):** O pooler do Supabase em modo transação (porta 6543) não suporta prepared statements do driver JDBC sem o parâmetro `prepareThreshold=0`, podendo causar falhas aleatórias de execução de query.
- **Tamanho Reduzido do Pool HikariCP (A2/A3):** Configurado com `maximum-pool-size=5` para não esgotar as conexões do Supabase Free. Sob concorrência de requisições, esse pool vira o gargalo primário do sistema.
- **Risco de Pausamento por Inatividade no Free Tier (A5):** Instâncias no plano gratuito do Supabase são suspensas após 7 dias sem requisições ativas na API do Supabase.
- **Backups e RPO (A5):** O plano gratuito oferece apenas retenção básica de backup, sem rotina automatizada de `pg_dump` externo para atender com segurança ao RPO de 24 horas.
