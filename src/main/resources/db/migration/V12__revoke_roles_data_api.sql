-- ==============================================================================
-- Migration V12: remove privilégios dos roles da Data API do Supabase (anon, authenticated)
-- O backend conecta como postgres e o frontend não usa a Data API. TRUNCATE ignora RLS,
-- então os grants eram a única barreira. A checagem de role mantém a migration válida
-- em bancos sem Supabase.
-- Rollback: GRANT ALL ON ALL TABLES/SEQUENCES IN SCHEMA public TO anon, authenticated
--           e os ALTER DEFAULT PRIVILEGES ... GRANT correspondentes.
-- ==============================================================================

DO $$
DECLARE
    papel text;
BEGIN
    FOREACH papel IN ARRAY ARRAY['anon', 'authenticated'] LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = papel) THEN
            EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA public FROM %I', papel);
            EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM %I', papel);
            EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM %I', papel);
            EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM %I', papel);
        END IF;
    END LOOP;
END $$;
