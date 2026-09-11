package com.agendamentos.equadras.dto.request;

import com.agendamentos.equadras.model.enums.TipoEsporte;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record QuadraCriacaoDTO(
        @NotBlank(message = "O nome da quadra é obrigatório")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "Caracteres HTML não são permitidos")
        String nome,

        @NotNull(message = "O tipo de esporte é obrigatório")
        TipoEsporte tipoEsporte,

        @NotNull(message = "O valor por hora é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor por hora deve ser maior que zero")
        BigDecimal valorHora,

        @Pattern(regexp = "^\\d{5}-\\d{3}$", message = "O CEP deve estar no formato XXXXX-XXX")
        String cep,
        
        @Size(max = 255, message = "O logradouro deve ter no máximo 255 caracteres")
        String logradouro,
        
        @Size(max = 100, message = "O bairro deve ter no máximo 100 caracteres")
        String bairro,
        
        @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres")
        String cidade,
        
        @Size(max = 2, message = "O estado deve ter no máximo 2 caracteres")
        String estado,

        Double latitude,
        Double longitude,
        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "Caracteres HTML não são permitidos na descrição")
        String descricao,
        java.time.LocalDate dataLimiteAgendamento,
        @Size(max = 5, message = "Uma quadra pode ter no máximo 5 fotos")
        java.util.List<@Size(max = 500, message = "URL da foto inválida") String> fotos,
        @Size(max = 7, message = "Uma quadra pode ter no máximo 7 regras de disponibilidade semanal")
        java.util.List<@jakarta.validation.Valid DisponibilidadeDiaDTO> disponibilidades
) {}