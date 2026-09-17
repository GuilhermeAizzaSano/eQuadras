package com.agendamentos.equadras.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auditoria_api_key")
public class AuditoriaApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = true)
    private Long usuarioId;

    @Column(nullable = false, length = 20)
    private String evento; // 'GERADA', 'REGENERADA', 'REVOGADA'

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public AuditoriaApiKey() {}

    public AuditoriaApiKey(Long usuarioId, String evento, String ip, String userAgent, Instant criadoEm) {
        this.usuarioId = usuarioId;
        this.evento = evento;
        this.ip = ip;
        this.userAgent = userAgent;
        this.criadoEm = criadoEm != null ? criadoEm : Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.criadoEm == null) {
            this.criadoEm = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getEvento() { return evento; }
    public void setEvento(String evento) { this.evento = evento; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
