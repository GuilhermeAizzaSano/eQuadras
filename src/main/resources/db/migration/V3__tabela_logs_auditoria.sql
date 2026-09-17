CREATE TABLE IF NOT EXISTS logs_auditoria (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT REFERENCES usuarios (id_usuario) ON DELETE SET NULL,
    usuario_email   VARCHAR(255),
    usuario_nome    VARCHAR(255),
    categoria       VARCHAR(30) NOT NULL,
    acao            VARCHAR(50) NOT NULL,
    entidade        VARCHAR(50),
    recurso_id      VARCHAR(100),
    tipo_executor   VARCHAR(30) NOT NULL,
    detalhes        TEXT,
    ip              VARCHAR(45),
    user_agent      VARCHAR(255),
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Índices otimizados para consultas e filtros do painel
CREATE INDEX IF NOT EXISTS idx_logs_auditoria_criado_em ON logs_auditoria (criado_em DESC);
CREATE INDEX IF NOT EXISTS idx_logs_auditoria_usuario_data ON logs_auditoria (usuario_id, criado_em DESC);
CREATE INDEX IF NOT EXISTS idx_logs_auditoria_cat_data ON logs_auditoria (categoria, criado_em DESC);
CREATE INDEX IF NOT EXISTS idx_logs_auditoria_acao_data ON logs_auditoria (acao, criado_em DESC);
CREATE INDEX IF NOT EXISTS idx_logs_auditoria_email ON logs_auditoria (usuario_email);
CREATE INDEX IF NOT EXISTS idx_logs_auditoria_recurso ON logs_auditoria (entidade, recurso_id);
