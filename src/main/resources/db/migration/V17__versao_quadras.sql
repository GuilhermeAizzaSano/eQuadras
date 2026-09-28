-- ==============================================================================
-- Migration V17: coluna de versão para lock otimista em quadras (achado B2)
-- Com DEFAULT constante o PostgreSQL 11+ só altera o catálogo, sem reescrever a tabela.
-- Deve subir junto com o @Version da entidade Quadra (ddl-auto=validate).
-- Rollback: ALTER TABLE public.quadras DROP COLUMN IF EXISTS versao; + revert do código.
-- ==============================================================================

ALTER TABLE public.quadras ADD COLUMN IF NOT EXISTS versao bigint NOT NULL DEFAULT 0;
