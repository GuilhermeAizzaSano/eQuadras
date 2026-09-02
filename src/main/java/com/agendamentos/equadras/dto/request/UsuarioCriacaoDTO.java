package com.agendamentos.equadras.dto.request;

import com.agendamentos.equadras.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioCriacaoDTO(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 80, message = "O nome deve ter entre 3 e 80 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "Caracteres HTML não são permitidos")
        String nome_usuario,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        @Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres")
        String email_usuario,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve conter no mínimo 6 caracteres")
        String senha_usuario,

        @NotBlank(message = "O telefone é obrigatório")
        String phone_usuario
) {}