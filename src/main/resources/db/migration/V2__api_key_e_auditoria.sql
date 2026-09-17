ALTER TABLE usuarios
  ADD COLUMN IF NOT EXISTS api_key_hash          VARCHAR(64),
  ADD COLUMN IF NOT EXISTS api_key_last4         VARCHAR(4),
  ADD COLUMN IF NOT EXISTS api_key_criada_em     TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS api_key_ultimo_uso_em TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS idx_usuarios_api_key_hash
  ON usuarios (api_key_hash) WHERE api_key_hash IS NOT NULL;

CREATE TABLE IF NOT EXISTS auditoria_api_key (
  id          BIGSERIAL PRIMARY KEY,
  usuario_id  BIGINT REFERENCES usuarios (id_usuario) ON DELETE SET NULL,
  evento      VARCHAR(20) NOT NULL CHECK (evento IN ('GERADA','REGENERADA','REVOGADA')),
  ip          VARCHAR(45),
  user_agent  VARCHAR(255),
  criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_auditoria_api_key_usuario ON auditoria_api_key (usuario_id, criado_em DESC);
