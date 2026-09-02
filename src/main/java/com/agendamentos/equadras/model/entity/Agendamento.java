package com.agendamentos.equadras.model.entity;

import com.agendamentos.equadras.model.enums.StatusAgendamento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendamentos", indexes = {
    @Index(name = "idx_agendamento_quadra_data", columnList = "quadra_id, dataHoraInicio, dataHoraFim"),
    @Index(name = "idx_agendamento_usuario", columnList = "usuario_id"),
    @Index(name = "idx_agendamento_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id_agendamento")
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

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        if (this.status == null) {
            this.status = StatusAgendamento.PENDENTE;
        }
    }
}