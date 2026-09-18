-- ==============================================================================
-- Migration V4: Token Version e Idempotência de Pagamentos
-- Adiciona suporte a revogação real de tokens JWT e índice único em transações
-- ==============================================================================

-- 1. Controle de versão de token para permitir invalidação imediata em logout e troca de senha
ALTER TABLE public.usuarios
  ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 1;

-- 2. Índice único e idempotência para transações de pagamento (evita duplicidade em webhooks)
CREATE UNIQUE INDEX IF NOT EXISTS uk_agendamentos_transacao_pagamento
  ON public.agendamentos (transacao_pagamento_id)
  WHERE transacao_pagamento_id IS NOT NULL;
