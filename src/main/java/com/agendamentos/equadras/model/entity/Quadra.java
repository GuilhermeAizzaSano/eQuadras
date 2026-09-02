package com.agendamentos.equadras.model.entity;

import com.agendamentos.equadras.model.enums.TipoEsporte;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "quadras", indexes = {
    @Index(name = "idx_quadra_admin", columnList = "admin_id"),
    @Index(name = "idx_quadra_ativa", columnList = "ativa"),
    @Index(name = "idx_quadras_lat_lng", columnList = "latitude, longitude")
})
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
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<String> fotos = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "quadra_disponibilidades", joinColumns = @JoinColumn(name = "quadra_id"))
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<DisponibilidadeDia> disponibilidades = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Usuario admin;

    public Quadra() {}

    public Quadra(Long id_quadra, String nome, TipoEsporte tipoEsporte, BigDecimal valorHora, boolean ativa, String cep, String logradouro, String bairro, String cidade, String estado, Double latitude, Double longitude, String descricao, List<String> fotos, List<DisponibilidadeDia> disponibilidades, Usuario admin) {
        this.id_quadra = id_quadra;
        this.nome = nome;
        this.tipoEsporte = tipoEsporte;
        this.valorHora = valorHora;
        this.ativa = ativa;
        this.cep = cep;
        this.logradouro = logradouro;
        this.bairro = bairro;
        this.cidade = cidade;
        this.estado = estado;
        this.latitude = latitude;
        this.longitude = longitude;
        this.descricao = descricao;
        this.fotos = fotos != null ? fotos : new ArrayList<>();
        this.disponibilidades = disponibilidades != null ? disponibilidades : new ArrayList<>();
        this.admin = admin;
    }

    public Quadra(Long id_quadra, String nome, TipoEsporte tipoEsporte, BigDecimal valorHora, boolean ativa, String cep, String logradouro, String bairro, String cidade, String estado, Double latitude, Double longitude, String descricao, List<String> fotos, Usuario admin) {
        this(id_quadra, nome, tipoEsporte, valorHora, ativa, cep, logradouro, bairro, cidade, estado, latitude, longitude, descricao, fotos, new ArrayList<>(), admin);
    }

    @PrePersist
    protected void onCreate() {
        this.ativa = true;
    }

    public Long getId_quadra() { return id_quadra; }
    public void setId_quadra(Long id_quadra) { this.id_quadra = id_quadra; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public TipoEsporte getTipoEsporte() { return tipoEsporte; }
    public void setTipoEsporte(TipoEsporte tipoEsporte) { this.tipoEsporte = tipoEsporte; }

    public BigDecimal getValorHora() { return valorHora; }
    public void setValorHora(BigDecimal valorHora) { this.valorHora = valorHora; }

    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public List<String> getFotos() { return fotos != null ? fotos : (fotos = new ArrayList<>()); }
    public void setFotos(List<String> fotos) { this.fotos = fotos; }

    public List<DisponibilidadeDia> getDisponibilidades() { return disponibilidades != null ? disponibilidades : (disponibilidades = new ArrayList<>()); }
    public void setDisponibilidades(List<DisponibilidadeDia> disponibilidades) { this.disponibilidades = disponibilidades; }

    public Usuario getAdmin() { return admin; }
    public void setAdmin(Usuario admin) { this.admin = admin; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quadra quadra = (Quadra) o;
        return Objects.equals(id_quadra, quadra.id_quadra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id_quadra);
    }

    public static QuadraBuilder builder() {
        return new QuadraBuilder();
    }

    public static class QuadraBuilder {
        private Long id_quadra;
        private String nome;
        private TipoEsporte tipoEsporte;
        private BigDecimal valorHora;
        private boolean ativa = true;
        private String cep;
        private String logradouro;
        private String bairro;
        private String cidade;
        private String estado;
        private Double latitude;
        private Double longitude;
        private String descricao;
        private List<String> fotos = new ArrayList<>();
        private List<DisponibilidadeDia> disponibilidades = new ArrayList<>();
        private Usuario admin;

        public QuadraBuilder id_quadra(Long id_quadra) { this.id_quadra = id_quadra; return this; }
        public QuadraBuilder nome(String nome) { this.nome = nome; return this; }
        public QuadraBuilder tipoEsporte(TipoEsporte tipoEsporte) { this.tipoEsporte = tipoEsporte; return this; }
        public QuadraBuilder valorHora(BigDecimal valorHora) { this.valorHora = valorHora; return this; }
        public QuadraBuilder ativa(boolean ativa) { this.ativa = ativa; return this; }
        public QuadraBuilder cep(String cep) { this.cep = cep; return this; }
        public QuadraBuilder logradouro(String logradouro) { this.logradouro = logradouro; return this; }
        public QuadraBuilder bairro(String bairro) { this.bairro = bairro; return this; }
        public QuadraBuilder cidade(String cidade) { this.cidade = cidade; return this; }
        public QuadraBuilder estado(String estado) { this.estado = estado; return this; }
        public QuadraBuilder latitude(Double latitude) { this.latitude = latitude; return this; }
        public QuadraBuilder longitude(Double longitude) { this.longitude = longitude; return this; }
        public QuadraBuilder descricao(String descricao) { this.descricao = descricao; return this; }
        public QuadraBuilder fotos(List<String> fotos) { this.fotos = fotos; return this; }
        public QuadraBuilder disponibilidades(List<DisponibilidadeDia> disponibilidades) { this.disponibilidades = disponibilidades; return this; }
        public QuadraBuilder admin(Usuario admin) { this.admin = admin; return this; }

        public Quadra build() {
            return new Quadra(id_quadra, nome, tipoEsporte, valorHora, ativa, cep, logradouro, bairro, cidade, estado, latitude, longitude, descricao, fotos, disponibilidades, admin);
        }
    }
}