-- ==============================================================================
-- Migration V11: índices de quadra_id nas coleções de quadra
-- Em produção a V1 nunca rodou (baseline 3), então quadra_fotos e quadra_disponibilidades
-- ficaram sem índice. CONCURRENTLY evita bloquear escrita; por isso o .conf desliga a transação.
-- Rollback: DROP INDEX CONCURRENTLY IF EXISTS dos dois índices.
-- ==============================================================================

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_quadra_fotos_quadra_id ON public.quadra_fotos (quadra_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_quadra_disp_quadra_id ON public.quadra_disponibilidades (quadra_id);
