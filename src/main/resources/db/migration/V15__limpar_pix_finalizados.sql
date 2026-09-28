-- ==============================================================================
-- Migration V15: limpa dados Pix de agendamentos que já saíram de PENDENTE
-- DESTRUTIVO: o backup das linhas afetadas é feito antes do deploy, em /opt/equadras/backups
-- na VM (onda3_pix.csv). Desde a onda 2 o código já zera esses campos ao confirmar ou cancelar;
-- esta migration limpa o legado.
-- Rollback (dados): carregar o CSV numa tabela temporária e
--   UPDATE public.agendamentos a SET pix_copiaecola = b.pix_copiaecola, qr_code_base64 = b.qr_code_base64
--     FROM bkp_pix b WHERE a.id_agendamento = b.id_agendamento;
-- ==============================================================================

UPDATE public.agendamentos
   SET pix_copiaecola = NULL, qr_code_base64 = NULL
 WHERE status <> 'PENDENTE'
   AND (pix_copiaecola IS NOT NULL OR qr_code_base64 IS NOT NULL);
