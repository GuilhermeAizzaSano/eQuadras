-- ==============================================================================
-- Migration V13: regras de domínio de quadra no banco
-- Espelha as validações do QuadraCriacaoDTO (UF, coordenadas, horas) e impede dia repetido
-- na mesma quadra. NOT VALID + VALIDATE separa a criação da checagem das linhas existentes.
-- O índice único é criado sem CONCURRENTLY: a tabela é pequena e a migration fica transacional.
-- Rollback: DROP CONSTRAINT IF EXISTS das três constraints e DROP INDEX IF EXISTS uk_disp_quadra_dia.
-- ==============================================================================

ALTER TABLE public.quadras DROP CONSTRAINT IF EXISTS chk_quadra_estado_uf;
ALTER TABLE public.quadras ADD CONSTRAINT chk_quadra_estado_uf
    CHECK (estado IS NULL OR estado ~ '^[A-Z]{2}$') NOT VALID;

ALTER TABLE public.quadras DROP CONSTRAINT IF EXISTS chk_quadra_coordenadas;
ALTER TABLE public.quadras ADD CONSTRAINT chk_quadra_coordenadas CHECK (
    (latitude IS NULL OR latitude BETWEEN -90 AND 90) AND (longitude IS NULL OR longitude BETWEEN -180 AND 180)) NOT VALID;

ALTER TABLE public.quadra_disponibilidades DROP CONSTRAINT IF EXISTS chk_disp_horas;
ALTER TABLE public.quadra_disponibilidades ADD CONSTRAINT chk_disp_horas CHECK (hora_fim > hora_inicio) NOT VALID;

ALTER TABLE public.quadras VALIDATE CONSTRAINT chk_quadra_estado_uf;
ALTER TABLE public.quadras VALIDATE CONSTRAINT chk_quadra_coordenadas;
ALTER TABLE public.quadra_disponibilidades VALIDATE CONSTRAINT chk_disp_horas;

CREATE UNIQUE INDEX IF NOT EXISTS uk_disp_quadra_dia ON public.quadra_disponibilidades (quadra_id, dia_semana);
