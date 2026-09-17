-- ATENCAO: script MANUAL e DESTRUTIVO, fora do fluxo Flyway (Flyway Community nao suporta undo/U-scripts).
-- Reverte as alteracoes da migration V2 (api_key_e_auditoria).
-- Executar apenas manualmente, sob supervisao, e apos backup do banco.
-- Efeito: apaga a tabela auditoria_api_key (todo o historico de auditoria de API-KEY) e as colunas
-- de api key em usuarios (api_key_hash, api_key_last4, api_key_criada_em, api_key_ultimo_uso_em),
-- revogando de fato todas as API-KEYs emitidas.

DROP TABLE IF EXISTS auditoria_api_key;

ALTER TABLE usuarios
  DROP COLUMN IF EXISTS api_key_hash,
  DROP COLUMN IF EXISTS api_key_last4,
  DROP COLUMN IF EXISTS api_key_criada_em,
  DROP COLUMN IF EXISTS api_key_ultimo_uso_em;

DROP INDEX IF EXISTS idx_usuarios_api_key_hash;
