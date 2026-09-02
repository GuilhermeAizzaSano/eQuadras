package com.agendamentos.equadras.model.entity;

import com.agendamentos.equadras.model.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id_usuario")
@Data
@Entity
@Table(name = "usuarios")
@Valid
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

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        if (this.role == null){
            this.role = Role.CLIENT;
        }
    }
}
