package com.agendamentos.equadras.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AlterarSenhaDTO(
        @NotBlank(message = "A senha atual é obrigatória")
        @Schema(description = "Senha atual da conta", example = "SenhaVelha@123")
        String senhaAtual,

        @NotBlank(message = "A nova senha é obrigatória")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{6,}$",
                message = "A nova senha deve ter no mínimo 6 caracteres, incluindo 1 letra maiúscula, 1 minúscula, 1 número e 1 caractere especial/símbolo."
        )
        @Schema(description = "Nova senha: mínimo de 6 caracteres, com 1 maiúscula, 1 minúscula, 1 número e 1 símbolo. Deve ser diferente da atual", example = "SenhaSuperSegura@456")
        String novaSenha
) {}
