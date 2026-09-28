-- ==============================================================================
-- Migration V18: confere que os índices criados na onda 3 existem e estão válidos
-- CREATE INDEX CONCURRENTLY que falha deixa o índice INVALID, e numa nova tentativa o
-- IF NOT EXISTS da V11/V16 pula a criação. Esta verificação impede o deploy de subir
-- com um índice que não garante nada. Só lê o catálogo.
-- Runbook se falhar (sempre com autorização):
--   1. DROP INDEX CONCURRENTLY IF EXISTS public.<índice>;
--   2. corrigir a causa (ex.: telefones duplicados para uk_usuarios_phone);
--   3. recriar o índice com a definição da migration original (V11, V13 ou V16);
--   4. flyway repair, se a V18 ficou registrada como falha, e novo deploy.
-- ==============================================================================

DO $$
DECLARE
    nome text;
BEGIN
    FOREACH nome IN ARRAY ARRAY['idx_quadra_fotos_quadra_id', 'idx_quadra_disp_quadra_id', 'uk_disp_quadra_dia', 'uk_usuarios_phone'] LOOP
        IF NOT EXISTS (SELECT 1
                         FROM pg_index i
                         JOIN pg_class c ON c.oid = i.indexrelid
                         JOIN pg_namespace n ON n.oid = c.relnamespace
                        WHERE n.nspname = 'public' AND c.relname = nome AND i.indisvalid) THEN
            RAISE EXCEPTION 'Índice % ausente ou inválido. Ver runbook no cabeçalho da V18.', nome;
        END IF;
    END LOOP;
END $$;
