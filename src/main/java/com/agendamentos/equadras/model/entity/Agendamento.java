package com.agendamentos.equadras.model.entity;

import com.agendamentos.equadras.model.enums.StatusAgendamento;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "agendamentos", indexes = {
    @Index(name = "idx_agendamento_quadra_data", columnList = "quadra_id, dataHoraInicio, dataHoraFim"),
    @Index(name = "idx_agendamento_usuario", columnList = "usuario_id"),
    @Index(name = "idx_agendamento_status", columnList = "status")
})
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_agendamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quadra_id", nullable = false)
    private Quadra quadra;

    @Column(nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(nullable = false)
    private LocalDateTime dataHoraFim;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAgendamento status;

    @Column(length = 100)
    private String transacaoPagamentoId;

    @Column(columnDefinition = "TEXT")
    private String pixCopiaECola;

    @Column(columnDefinition = "TEXT")
    private String qrCodeBase64;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public Agendamento() {}

    public Agendamento(Long id_agendamento, Usuario usuario, Quadra quadra, LocalDateTime dataHoraInicio, LocalDateTime dataHoraFim, BigDecimal valorTotal, StatusAgendamento status, String transacaoPagamentoId, String pixCopiaECola, String qrCodeBase64, LocalDateTime criadoEm) {
        this.id_agendamento = id_agendamento;
        this.usuario = usuario;
        this.quadra = quadra;
        this.dataHoraInicio = dataHoraInicio;
        this.dataHoraFim = dataHoraFim;
        this.valorTotal = valorTotal;
        this.status = status;
        this.transacaoPagamentoId = transacaoPagamentoId;
        this.pixCopiaECola = pixCopiaECola;
        this.qrCodeBase64 = qrCodeBase64;
        this.criadoEm = criadoEm;
    }

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        if (this.status == null) {
            this.status = StatusAgendamento.PENDENTE;
        }
    }

    public Long getId_agendamento() { return id_agendamento; }
    public void setId_agendamento(Long id_agendamento) { this.id_agendamento = id_agendamento; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Quadra getQuadra() { return quadra; }
    public void setQuadra(Quadra quadra) { this.quadra = quadra; }

    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public void setDataHoraInicio(LocalDateTime dataHoraInicio) { this.dataHoraInicio = dataHoraInicio; }

    public LocalDateTime getDataHoraFim() { return dataHoraFim; }
    public void setDataHoraFim(LocalDateTime dataHoraFim) { this.dataHoraFim = dataHoraFim; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public StatusAgendamento getStatus() { return status; }
    public void setStatus(StatusAgendamento status) { this.status = status; }

    public String getTransacaoPagamentoId() { return transacaoPagamentoId; }
    public void setTransacaoPagamentoId(String transacaoPagamentoId) { this.transacaoPagamentoId = transacaoPagamentoId; }

    public String getPixCopiaECola() { return pixCopiaECola; }
    public void setPixCopiaECola(String pixCopiaECola) { this.pixCopiaECola = pixCopiaECola; }

    public String getQrCodeBase64() { return qrCodeBase64; }
    public void setQrCodeBase64(String qrCodeBase64) { this.qrCodeBase64 = qrCodeBase64; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Agendamento that = (Agendamento) o;
        return Objects.equals(id_agendamento, that.id_agendamento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id_agendamento);
    }

    public static AgendamentoBuilder builder() {
        return new AgendamentoBuilder();
    }

    public static class AgendamentoBuilder {
        private Long id_agendamento;
        private Usuario usuario;
        private Quadra quadra;
        private LocalDateTime dataHoraInicio;
        private LocalDateTime dataHoraFim;
        private BigDecimal valorTotal;
        private StatusAgendamento status;
        private String transacaoPagamentoId;
        private String pixCopiaECola;
        private String qrCodeBase64;
        private LocalDateTime criadoEm;

        public AgendamentoBuilder id_agendamento(Long id_agendamento) { this.id_agendamento = id_agendamento; return this; }
        public AgendamentoBuilder usuario(Usuario usuario) { this.usuario = usuario; return this; }
        public AgendamentoBuilder quadra(Quadra quadra) { this.quadra = quadra; return this; }
        public AgendamentoBuilder dataHoraInicio(LocalDateTime dataHoraInicio) { this.dataHoraInicio = dataHoraInicio; return this; }
        public AgendamentoBuilder dataHoraFim(LocalDateTime dataHoraFim) { this.dataHoraFim = dataHoraFim; return this; }
        public AgendamentoBuilder valorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; return this; }
        public AgendamentoBuilder status(StatusAgendamento status) { this.status = status; return this; }
        public AgendamentoBuilder transacaoPagamentoId(String transacaoPagamentoId) { this.transacaoPagamentoId = transacaoPagamentoId; return this; }
        public AgendamentoBuilder pixCopiaECola(String pixCopiaECola) { this.pixCopiaECola = pixCopiaECola; return this; }
        public AgendamentoBuilder qrCodeBase64(String qrCodeBase64) { this.qrCodeBase64 = qrCodeBase64; return this; }
        public AgendamentoBuilder criadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; return this; }

        public Agendamento build() {
            return new Agendamento(id_agendamento, usuario, quadra, dataHoraInicio, dataHoraFim, valorTotal, status, transacaoPagamentoId, pixCopiaECola, qrCodeBase64, criadoEm);
        }
    }
}