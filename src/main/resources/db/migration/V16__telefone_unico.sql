-- ==============================================================================
-- Migration V16: um telefone por usuário (o bot identifica o cliente pelo telefone)
-- Pré-requisito: telefones normalizados (só dígitos) e sem duplicados, feito antes do deploy
-- por script manual. Se ainda houver duplicado, o índice falha e o deploy é revertido.
-- Sem transação por causa do CONCURRENTLY (ver .conf).
-- Rollback: DROP INDEX CONCURRENTLY IF EXISTS public.uk_usuarios_phone;
-- ==============================================================================

CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS uk_usuarios_phone
    ON public.usuarios (phone_usuario) WHERE phone_usuario IS NOT NULL;
