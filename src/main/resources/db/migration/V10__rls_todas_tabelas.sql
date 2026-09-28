-- ==============================================================================
-- Migration V10: Row Level Security (RLS) explícito em todas as tabelas do schema public
-- As tabelas criadas no baseline (V1) tiveram RLS configurado manualmente no Supabase,
-- sem registro versionado. Esta migration torna a configuração explícita e reprodutível,
-- usando o mesmo padrão da V6: RLS habilitado e acesso irrestrito apenas para as roles
-- de infraestrutura/backend (postgres e service_role). As roles expostas pela API do
-- Supabase (anon, authenticated) ficam sem política e, portanto, sem acesso.
-- Idempotente: pode ser reaplicada sem efeito colateral.
-- flyway_schema_history não é alterada aqui: o Flyway mantém lock nela em outra conexão
-- durante a migração, e o ALTER TABLE ficaria bloqueado até o statement timeout. Seu RLS
-- já está habilitado no Supabase e é garantido pela verificação ao final deste script.
-- ==============================================================================

DO $$
DECLARE
    tabela TEXT;
    tabelas TEXT[] := ARRAY[
        'usuarios',
        'quadras',
        'quadra_fotos',
        'quadra_disponibilidades',
        'quadra_bloqueios',
        'notificacoes',
        'agendamentos',
        'auditoria_api_key',
        'logs_auditoria'
    ];
BEGIN
    FOREACH tabela IN ARRAY tabelas LOOP
        IF to_regclass('public.' || tabela) IS NOT NULL THEN
            EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', tabela);
            EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', 'backend_full_access_' || tabela, tabela);
            EXECUTE format(
                'CREATE POLICY %I ON public.%I AS PERMISSIVE FOR ALL TO postgres, service_role USING (true) WITH CHECK (true)',
                'backend_full_access_' || tabela, tabela);
        END IF;
    END LOOP;
END $$;

-- Verificação: falha a migration se restar qualquer tabela do schema public sem RLS
DO $$
DECLARE
    sem_rls TEXT;
BEGIN
    SELECT string_agg(c.relname, ', ')
      INTO sem_rls
      FROM pg_class c
      JOIN pg_namespace n ON n.oid = c.relnamespace
     WHERE n.nspname = 'public'
       AND c.relkind IN ('r', 'p')
       AND NOT c.relrowsecurity;

    IF sem_rls IS NOT NULL THEN
        RAISE EXCEPTION 'Tabelas do schema public sem RLS habilitado: %', sem_rls;
    END IF;
END $$;
