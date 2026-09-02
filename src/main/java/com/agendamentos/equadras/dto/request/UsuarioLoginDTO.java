package com.agendamentos.equadras.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioLoginDTO(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email_usuario,

        @NotBlank(message = "A senha é obrigatória")
        String senha_usuario
) {}
