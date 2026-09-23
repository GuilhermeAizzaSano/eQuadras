-- ==============================================================================
-- Migration V7: Defesa Temporal (btree_gist), Unicidade e Constraints Defensivas
-- ==============================================================================

-- 1. Habilita extensão para índices de exclusão GiST com tipos escalares
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- 2. Constraint de exclusão temporal física para agendamentos ativos/pendentes
ALTER TABLE public.agendamentos
    DROP CONSTRAINT IF EXISTS agendamentos_sem_sobreposicao;

ALTER TABLE public.agendamentos
    ADD CONSTRAINT agendamentos_sem_sobreposicao
    EXCLUDE USING gist (
        quadra_id WITH =,
        tsrange(data_hora_inicio, data_hora_fim, '[)') WITH &&
    )
    WHERE (status IN ('PENDENTE', 'CONFIRMADO'));

-- 3. Unicidade de e-mail case-insensitive
CREATE UNIQUE INDEX IF NOT EXISTS uk_usuarios_email_lower
    ON public.usuarios (lower(email_usuario));

-- 4. Constraints defensivas em agendamentos
ALTER TABLE public.agendamentos
    DROP CONSTRAINT IF EXISTS chk_agendamento_datas,
    DROP CONSTRAINT IF EXISTS chk_agendamento_valor;

ALTER TABLE public.agendamentos
    ADD CONSTRAINT chk_agendamento_datas CHECK (data_hora_fim > data_hora_inicio),
    ADD CONSTRAINT chk_agendamento_valor CHECK (valor_total >= 0);

-- 5. Constraints defensivas em quadras
ALTER TABLE public.quadras
    DROP CONSTRAINT IF EXISTS chk_quadra_valor_hora;

ALTER TABLE public.quadras
    ADD CONSTRAINT chk_quadra_valor_hora CHECK (valor_hora > 0);

-- 6. Constraints defensivas em bloqueios
ALTER TABLE public.quadra_bloqueios
    DROP CONSTRAINT IF EXISTS chk_bloqueio_horas;

ALTER TABLE public.quadra_bloqueios
    ADD CONSTRAINT chk_bloqueio_horas CHECK (hora_fim IS NULL OR hora_inicio IS NULL OR hora_fim > hora_inicio);
