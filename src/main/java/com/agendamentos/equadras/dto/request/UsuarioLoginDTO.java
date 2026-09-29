package com.agendamentos.equadras.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioLoginDTO(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        @Schema(description = "E-mail de acesso da conta", example = "arthur.prado@email.com")
        String email_usuario,

        @NotBlank(message = "A senha é obrigatória")
        @Schema(description = "Senha da conta", example = "SenhaSegura@123")
        String senha_usuario
) {}
