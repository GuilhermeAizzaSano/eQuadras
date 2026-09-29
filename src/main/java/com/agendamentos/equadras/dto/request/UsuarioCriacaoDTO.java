package com.agendamentos.equadras.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import com.agendamentos.equadras.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioCriacaoDTO(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 80, message = "O nome deve ter entre 3 e 80 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "Caracteres HTML não são permitidos")
        @Schema(description = "Nome completo, de 3 a 80 caracteres, sem os símbolos < e >", example = "Carlos Eduardo")
        String nome_usuario,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        @Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres")
        @Schema(description = "E-mail da conta, até 100 caracteres. Não pode repetir outro cadastro (400 EMAIL_DUPLICADO)", example = "carlos.eduardo@email.com")
        String email_usuario,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve conter no mínimo 6 caracteres")
        @Schema(description = "Senha de acesso, mínimo de 6 caracteres", example = "SenhaForte@123")
        String senha_usuario,

        @NotBlank(message = "O telefone é obrigatório")
        @Size(min = 8, max = 20, message = "O telefone deve ter entre 8 e 20 caracteres")
        @Schema(description = "Telefone com DDD, de 8 a 20 caracteres. Não pode repetir outro cadastro (409 TELEFONE_EM_USO)", example = "(11) 98888-7777")
        String phone_usuario,

        @Schema(description = "Papel da conta: CLIENT ou ADMIN. Opcional", example = "CLIENT")
        Role role
) {
    public UsuarioCriacaoDTO(String nome_usuario, String email_usuario, String senha_usuario, String phone_usuario) {
        this(nome_usuario, email_usuario, senha_usuario, phone_usuario, Role.CLIENT);
    }
}