-- ==============================================================================
-- Migration V14: remove objetos redundantes (achado B5 da auditoria)
-- ukbdp41y8e8un0nsxowhgsl783s: UNIQUE (email_usuario) é coberta por uk_usuarios_email_lower
--   (lower(email_usuario)), que é mais restrita. 0 scans em produção.
-- idx_quadra_ativa: booleano de baixa seletividade, 0 scans em produção.
-- Sem transação por causa do CONCURRENTLY (ver .conf).
-- Rollback: ALTER TABLE public.usuarios ADD CONSTRAINT ukbdp41y8e8un0nsxowhgsl783s UNIQUE (email_usuario);
--           CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_quadra_ativa ON public.quadras (ativa);
-- ==============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'uk_usuarios_email_lower') THEN
        RAISE EXCEPTION 'uk_usuarios_email_lower ausente: a unique de e-mail não pode ser removida';
    END IF;
END $$;

ALTER TABLE public.usuarios DROP CONSTRAINT IF EXISTS ukbdp41y8e8un0nsxowhgsl783s;
DROP INDEX CONCURRENTLY IF EXISTS public.idx_quadra_ativa;
