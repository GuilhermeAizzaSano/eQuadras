package com.agendamentos.equadras.model.entity;

import com.agendamentos.equadras.model.enums.Role;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id_usuario;

    @Column(nullable = false, length = 80)
    private String nome_usuario;

    @Column(nullable = false, unique = true, length = 100)
    private String email_usuario;

    @Column(nullable = false)
    private String senha_usuario;

    @Column(nullable = false)
    private String phone_usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "api_key_hash", length = 64)
    private String apiKeyHash;

    @Column(name = "api_key_last4", length = 4)
    private String apiKeyLast4;

    @Column(name = "api_key_criada_em")
    private Instant apiKeyCriadaEm;

    @Column(name = "api_key_ultimo_uso_em")
    private Instant apiKeyUltimoUsoEm;

    @Column(nullable = false)
    private boolean ativo = true;

    public Usuario() {}

    public Usuario(Long id_usuario, String nome_usuario, String email_usuario, String senha_usuario, String phone_usuario, Role role, LocalDateTime criadoEm) {
        this.id_usuario = id_usuario;
        this.nome_usuario = nome_usuario;
        this.email_usuario = email_usuario;
        this.senha_usuario = senha_usuario;
        this.phone_usuario = phone_usuario;
        this.role = role;
        this.criadoEm = criadoEm;
        this.ativo = true;
    }

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        if (this.role == null){
            this.role = Role.CLIENT;
        }
    }

    public Long getId_usuario() { return id_usuario; }
    public void setId_usuario(Long id_usuario) { this.id_usuario = id_usuario; }

    public String getNome_usuario() { return nome_usuario; }
    public void setNome_usuario(String nome_usuario) { this.nome_usuario = nome_usuario; }

    public String getEmail_usuario() { return email_usuario; }
    public void setEmail_usuario(String email_usuario) { this.email_usuario = email_usuario; }

    public String getSenha_usuario() { return senha_usuario; }
    public void setSenha_usuario(String senha_usuario) { this.senha_usuario = senha_usuario; }

    public String getPhone_usuario() { return phone_usuario; }
    public void setPhone_usuario(String phone_usuario) { this.phone_usuario = phone_usuario; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public String getApiKeyHash() { return apiKeyHash; }
    public void setApiKeyHash(String apiKeyHash) { this.apiKeyHash = apiKeyHash; }

    public String getApiKeyLast4() { return apiKeyLast4; }
    public void setApiKeyLast4(String apiKeyLast4) { this.apiKeyLast4 = apiKeyLast4; }

    public Instant getApiKeyCriadaEm() { return apiKeyCriadaEm; }
    public void setApiKeyCriadaEm(Instant apiKeyCriadaEm) { this.apiKeyCriadaEm = apiKeyCriadaEm; }

    public Instant getApiKeyUltimoUsoEm() { return apiKeyUltimoUsoEm; }
    public void setApiKeyUltimoUsoEm(Instant apiKeyUltimoUsoEm) { this.apiKeyUltimoUsoEm = apiKeyUltimoUsoEm; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public boolean isMasterAdmin() {
        return this.role == Role.ADMIN && "gui@gmail.com".equalsIgnoreCase(this.email_usuario);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return Objects.equals(id_usuario, usuario.id_usuario);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id_usuario);
    }

    public static UsuarioBuilder builder() {
        return new UsuarioBuilder();
    }

    public static class UsuarioBuilder {
        private Long id_usuario;
        private String nome_usuario;
        private String email_usuario;
        private String senha_usuario;
        private String phone_usuario;
        private Role role;
        private LocalDateTime criadoEm;
        private String apiKeyHash;
        private String apiKeyLast4;
        private Instant apiKeyCriadaEm;
        private Instant apiKeyUltimoUsoEm;
        private boolean ativo = true;

        public UsuarioBuilder id_usuario(Long id_usuario) { this.id_usuario = id_usuario; return this; }
        public UsuarioBuilder nome_usuario(String nome_usuario) { this.nome_usuario = nome_usuario; return this; }
        public UsuarioBuilder email_usuario(String email_usuario) { this.email_usuario = email_usuario; return this; }
        public UsuarioBuilder senha_usuario(String senha_usuario) { this.senha_usuario = senha_usuario; return this; }
        public UsuarioBuilder phone_usuario(String phone_usuario) { this.phone_usuario = phone_usuario; return this; }
        public UsuarioBuilder role(Role role) { this.role = role; return this; }
        public UsuarioBuilder criadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; return this; }
        public UsuarioBuilder apiKeyHash(String apiKeyHash) { this.apiKeyHash = apiKeyHash; return this; }
        public UsuarioBuilder apiKeyLast4(String apiKeyLast4) { this.apiKeyLast4 = apiKeyLast4; return this; }
        public UsuarioBuilder apiKeyCriadaEm(Instant apiKeyCriadaEm) { this.apiKeyCriadaEm = apiKeyCriadaEm; return this; }
        public UsuarioBuilder apiKeyUltimoUsoEm(Instant apiKeyUltimoUsoEm) { this.apiKeyUltimoUsoEm = apiKeyUltimoUsoEm; return this; }
        public UsuarioBuilder ativo(boolean ativo) { this.ativo = ativo; return this; }

        public Usuario build() {
            Usuario u = new Usuario(id_usuario, nome_usuario, email_usuario, senha_usuario, phone_usuario, role, criadoEm);
            u.setApiKeyHash(this.apiKeyHash);
            u.setApiKeyLast4(this.apiKeyLast4);
            u.setApiKeyCriadaEm(this.apiKeyCriadaEm);
            u.setApiKeyUltimoUsoEm(this.apiKeyUltimoUsoEm);
            u.setAtivo(this.ativo);
            return u;
        }
    }
}
