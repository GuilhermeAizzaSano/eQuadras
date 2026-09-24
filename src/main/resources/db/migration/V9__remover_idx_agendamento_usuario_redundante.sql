-- ==============================================================================
-- Migration V9: Remoção de índice redundante
-- idx_agendamento_usuario (usuario_id) é coberto pelo prefixo de
-- idx_agendamento_usuario_data (usuario_id, data_hora_inicio DESC, id_agendamento DESC) criado na V8.
-- Não remove dados. Reversão:
--   CREATE INDEX IF NOT EXISTS idx_agendamento_usuario ON public.agendamentos (usuario_id);
-- ==============================================================================

DROP INDEX IF EXISTS public.idx_agendamento_usuario;
