package com.agendamentos.equadras.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import com.agendamentos.equadras.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioEdicaoDTO(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 80, message = "O nome deve ter entre 3 e 80 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "Caracteres HTML não são permitidos")
        @Schema(description = "Nome completo, de 3 a 80 caracteres, sem os símbolos < e >", example = "Arthur Prado Silva")
        String nome_usuario,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        @Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres")
        @Schema(description = "E-mail da conta, até 100 caracteres", example = "arthur.silva@email.com")
        String email_usuario,

        @NotBlank(message = "O telefone é obrigatório")
        @Schema(description = "Telefone com DDD", example = "(11) 99999-9999")
        String phone_usuario,

        @Schema(description = "Papel da conta: CLIENT ou ADMIN", example = "CLIENT")
        Role role,

        @Size(min = 6, message = "A nova senha deve conter no mínimo 6 caracteres")
        @Schema(description = "Opcional. Nova senha, mínimo de 6 caracteres. Omitida, a senha atual é mantida", example = "NovaSenha@456")
        String nova_senha
) {}
