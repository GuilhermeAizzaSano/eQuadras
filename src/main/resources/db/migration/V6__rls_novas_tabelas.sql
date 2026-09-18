-- ==============================================================================
-- Migration V6: Configuração de Row Level Security (RLS) nas novas tabelas
-- Habilita RLS e cria políticas idênticas às tabelas existentes (acesso irrestrito
-- para roles de infraestrutura e backend: postgres e service_role).
-- ==============================================================================

-- 1. Tabela: auditoria_api_key
ALTER TABLE public.auditoria_api_key ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS backend_full_access_auditoria_api_key ON public.auditoria_api_key;
CREATE POLICY backend_full_access_auditoria_api_key
    ON public.auditoria_api_key
    AS PERMISSIVE
    FOR ALL
    TO postgres, service_role
    USING (true)
    WITH CHECK (true);

-- 2. Tabela: logs_auditoria
ALTER TABLE public.logs_auditoria ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS backend_full_access_logs_auditoria ON public.logs_auditoria;
CREATE POLICY backend_full_access_logs_auditoria
    ON public.logs_auditoria
    AS PERMISSIVE
    FOR ALL
    TO postgres, service_role
    USING (true)
    WITH CHECK (true);
