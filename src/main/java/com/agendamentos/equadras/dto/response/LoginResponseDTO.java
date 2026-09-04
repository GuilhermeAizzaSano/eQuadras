package com.agendamentos.equadras.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de autenticação bem-sucedida e token JWT")
public record LoginResponseDTO(
        @Schema(description = "Token JWT assinado para autenticação Bearer", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "Perfil cadastral do usuário autenticado")
        UsuarioResponseDTO usuario
) {}