-- ==============================================================================
-- Migration V8: Índice para Paginação e Filtros de Agendamentos por Usuário
-- Otimiza a ordenação e paginação por data_hora_inicio e desempate por id_agendamento
-- ==============================================================================

CREATE INDEX IF NOT EXISTS idx_agendamento_usuario_data
    ON public.agendamentos (usuario_id, data_hora_inicio DESC, id_agendamento DESC);
