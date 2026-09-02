package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;

import java.time.LocalDateTime;

public record UsuarioResponseDTO(
        Long id_usuario,
        String nome_usuario,
        String email_usuario,
        String phone_usuario,
        Role role,
        LocalDateTime criadoEm
) {
    public static UsuarioResponseDTO fromEntity(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId_usuario(),
                usuario.getNome_usuario(),
                usuario.getEmail_usuario(),
                usuario.getPhone_usuario(),
                usuario.getRole(),
                usuario.getCriadoEm()
        );
    }
}