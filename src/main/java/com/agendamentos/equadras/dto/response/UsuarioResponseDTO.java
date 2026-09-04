package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Perfil cadastral de um usuário do sistema")
public record UsuarioResponseDTO(
        @Schema(description = "ID do usuário", example = "10")
        Long id_usuario,

        @Schema(description = "Nome completo", example = "Arthur Prado")
        String nome_usuario,

        @Schema(description = "Endereço de e-mail", example = "arthur.prado@email.com")
        String email_usuario,

        @Schema(description = "Telefone ou WhatsApp", example = "(11) 99999-8888")
        String phone_usuario,

        @Schema(description = "Papel no sistema (CLIENT ou ADMIN)", example = "CLIENT")
        Role role,

        @Schema(description = "Data e hora de criação da conta", example = "2026-09-04T10:00:00")
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