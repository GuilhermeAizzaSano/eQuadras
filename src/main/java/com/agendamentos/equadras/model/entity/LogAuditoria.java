package com.agendamentos.equadras.model.entity;

import com.agendamentos.equadras.model.enums.CategoriaAuditoria;
import com.agendamentos.equadras.model.enums.TipoExecutor;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "logs_auditoria")
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_email")
    private String usuarioEmail;

    @Column(name = "usuario_nome")
    private String usuarioNome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaAuditoria categoria;

    @Column(nullable = false, length = 50)
    private String acao;

    @Column(length = 50)
    private String entidade;

    @Column(name = "recurso_id", length = 100)
    private String recursoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_executor", nullable = false, length = 30)
    private TipoExecutor tipoExecutor;

    @Column(columnDefinition = "TEXT")
    private String detalhes;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    public LogAuditoria() {
    }

    public LogAuditoria(Long usuarioId, String usuarioEmail, String usuarioNome,
                        CategoriaAuditoria categoria, String acao, String entidade,
                        String recursoId, TipoExecutor tipoExecutor, String detalhes,
                        String ip, String userAgent, Instant criadoEm) {
        this.usuarioId = usuarioId;
        this.usuarioEmail = usuarioEmail;
        this.usuarioNome = usuarioNome;
        this.categoria = categoria;
        this.acao = acao;
        this.entidade = entidade;
        this.recursoId = recursoId;
        this.tipoExecutor = tipoExecutor;
        this.detalhes = detalhes;
        this.ip = ip;
        this.userAgent = userAgent;
        this.criadoEm = (criadoEm != null) ? criadoEm : Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.criadoEm == null) {
            this.criadoEm = Instant.now();
        }
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioEmail() { return usuarioEmail; }
    public void setUsuarioEmail(String usuarioEmail) { this.usuarioEmail = usuarioEmail; }

    public String getUsuarioNome() { return usuarioNome; }
    public void setUsuarioNome(String usuarioNome) { this.usuarioNome = usuarioNome; }

    public CategoriaAuditoria getCategoria() { return categoria; }
    public void setCategoria(CategoriaAuditoria categoria) { this.categoria = categoria; }

    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }

    public String getEntidade() { return entidade; }
    public void setEntidade(String entidade) { this.entidade = entidade; }

    public String getRecursoId() { return recursoId; }
    public void setRecursoId(String recursoId) { this.recursoId = recursoId; }

    public TipoExecutor getTipoExecutor() { return tipoExecutor; }
    public void setTipoExecutor(TipoExecutor tipoExecutor) { this.tipoExecutor = tipoExecutor; }

    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }

    // Builder
    public static LogAuditoriaBuilder builder() {
        return new LogAuditoriaBuilder();
    }

    public static class LogAuditoriaBuilder {
        private Long usuarioId;
        private String usuarioEmail;
        private String usuarioNome;
        private CategoriaAuditoria categoria;
        private String acao;
        private String entidade;
        private String recursoId;
        private TipoExecutor tipoExecutor;
        private String detalhes;
        private String ip;
        private String userAgent;
        private Instant criadoEm;

        public LogAuditoriaBuilder usuarioId(Long usuarioId) { this.usuarioId = usuarioId; return this; }
        public LogAuditoriaBuilder usuarioEmail(String usuarioEmail) { this.usuarioEmail = usuarioEmail; return this; }
        public LogAuditoriaBuilder usuarioNome(String usuarioNome) { this.usuarioNome = usuarioNome; return this; }
        public LogAuditoriaBuilder categoria(CategoriaAuditoria categoria) { this.categoria = categoria; return this; }
        public LogAuditoriaBuilder acao(String acao) { this.acao = acao; return this; }
        public LogAuditoriaBuilder entidade(String entidade) { this.entidade = entidade; return this; }
        public LogAuditoriaBuilder recursoId(String recursoId) { this.recursoId = recursoId; return this; }
        public LogAuditoriaBuilder tipoExecutor(TipoExecutor tipoExecutor) { this.tipoExecutor = tipoExecutor; return this; }
        public LogAuditoriaBuilder detalhes(String detalhes) { this.detalhes = detalhes; return this; }
        public LogAuditoriaBuilder ip(String ip) { this.ip = ip; return this; }
        public LogAuditoriaBuilder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public LogAuditoriaBuilder criadoEm(Instant criadoEm) { this.criadoEm = criadoEm; return this; }

        public LogAuditoria build() {
            return new LogAuditoria(usuarioId, usuarioEmail, usuarioNome, categoria, acao, entidade,
                    recursoId, tipoExecutor, detalhes, ip, userAgent, criadoEm);
        }
    }
}
