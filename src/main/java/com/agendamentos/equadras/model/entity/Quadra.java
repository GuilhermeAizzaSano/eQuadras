package com.agendamentos.equadras.model.entity;

import com.agendamentos.equadras.model.enums.TipoEsporte;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "quadras", indexes = {
    @Index(name = "idx_quadra_admin", columnList = "admin_id"),
    @Index(name = "idx_quadra_ativa", columnList = "ativa")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id_quadra")
public class Quadra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_quadra;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEsporte tipoEsporte;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorHora;

    @Column(nullable = false)
    private boolean ativa;

    @Column(length = 9)
    private String cep;

    @Column(length = 255)
    private String logradouro;

    @Column(length = 100)
    private String bairro;

    @Column(length = 100)
    private String cidade;

    @Column(length = 2)
    private String estado;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quadra_fotos", joinColumns = @JoinColumn(name = "quadra_id"))
    @Column(name = "foto_url")
    @Builder.Default
    private java.util.List<String> fotos = new java.util.ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Usuario admin;

    @PrePersist
    protected void onCreate() {
        this.ativa = true;
    }
}