-- ==============================================================================
-- Migration V5: Índices Estratégicos para Otimização de Consultas (Supabase)
-- ==============================================================================

-- 1. Índice parcial para checagem de conflitos de horários em agendamentos ativos
-- Ignora registros com status 'CANCELADO', otimizando a checagem de disponibilidade
CREATE INDEX IF NOT EXISTS idx_agendamentos_ativos_horario
    ON public.agendamentos (quadra_id, data_hora_inicio, data_hora_fim)
    WHERE status != 'CANCELADO';

-- 2. Índice parcial para busca rápida de notificações não lidas e não excluídas
-- Otimiza o contador e a listagem de alertas do admin sem escanear histórico antigo
CREATE INDEX IF NOT EXISTS idx_notificacoes_admin_nao_lidas
    ON public.notificacoes (admin_id, data_criacao DESC)
    WHERE lida = false AND excluida = false;

-- 3. Índice em quadra_bloqueios por data para agilizar filtros de bloqueios diários
CREATE INDEX IF NOT EXISTS idx_quadra_bloqueios_data
    ON public.quadra_bloqueios (data);
