package com.agendamentos.equadras.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import com.agendamentos.equadras.model.enums.TipoEsporte;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@DisponibilidadesSemDiaRepetido
public record QuadraCriacaoDTO(
        @NotBlank(message = "O nome da quadra é obrigatório")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "Caracteres HTML não são permitidos")
        @Schema(description = "Nome da quadra, de 3 a 100 caracteres, sem os símbolos < e >", example = "Arena Gol Society")
        String nome,

        @NotNull(message = "O tipo de esporte é obrigatório")
        @Schema(description = "Modalidade: FUTEBOL, FUTSAL, VOLEI, BEACH_TENNIS, BASQUETE ou TENIS", example = "FUTEBOL")
        TipoEsporte tipoEsporte,

        @NotNull(message = "O valor por hora é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor por hora deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "O valor por hora deve ter no máximo 8 dígitos inteiros e 2 casas decimais")
        @Schema(description = "Valor cobrado por hora, maior que zero, até 8 dígitos inteiros e 2 decimais", example = "120.00")
        BigDecimal valorHora,

        @Pattern(regexp = "^\\d{5}-\\d{3}$", message = "O CEP deve estar no formato XXXXX-XXX")
        @Schema(description = "CEP no formato XXXXX-XXX", example = "15000-000")
        String cep,
        
        @Size(max = 255, message = "O logradouro deve ter no máximo 255 caracteres")
        @Schema(description = "Logradouro, até 255 caracteres", example = "Av. Brasil, 1500")
        String logradouro,
        
        @Size(max = 100, message = "O bairro deve ter no máximo 100 caracteres")
        @Schema(description = "Bairro, até 100 caracteres", example = "Jardim das Flores")
        String bairro,
        
        @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres")
        @Schema(description = "Cidade, até 100 caracteres", example = "São José do Rio Preto")
        String cidade,
        
        @Size(max = 2, message = "O estado deve ter no máximo 2 caracteres")
        @Pattern(regexp = "^[A-Z]{2}$", message = "O estado deve ser uma UF com 2 letras maiúsculas")
        @Schema(description = "UF com 2 letras maiúsculas", example = "SP")
        String estado,

        @DecimalMin(value = "-90.0", message = "Latitude deve estar entre -90 e 90")
        @DecimalMax(value = "90.0", message = "Latitude deve estar entre -90 e 90")
        @Schema(description = "Latitude em graus, de -90 a 90", example = "-20.8113")
        Double latitude,
        @DecimalMin(value = "-180.0", message = "Longitude deve estar entre -180 e 180")
        @DecimalMax(value = "180.0", message = "Longitude deve estar entre -180 e 180")
        @Schema(description = "Longitude em graus, de -180 a 180", example = "-49.3758")
        Double longitude,
        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "Caracteres HTML não são permitidos na descrição")
        @Schema(description = "Descrição da infraestrutura, até 2000 caracteres, sem os símbolos < e >", example = "Grama sintética padrão FIFA com iluminação em LED e vestiários.")
        String descricao,
        @Schema(description = "Data limite para agendamentos futuros (yyyy-MM-dd). Não pode estar no passado", example = "2026-12-31")
        java.time.LocalDate dataLimiteAgendamento,
        @Size(max = 5, message = "Uma quadra pode ter no máximo 5 fotos")
        @Schema(description = "URLs das fotos da quadra, no máximo 5, cada uma com até 255 caracteres")
        java.util.List<@Size(max = 255, message = "A URL da foto deve ter no máximo 255 caracteres") String> fotos,
        @Size(max = 7, message = "Uma quadra pode ter no máximo 7 regras de disponibilidade semanal")
        @Schema(description = "Horários de funcionamento semanais, no máximo 7 regras, um dia da semana por regra")
        java.util.List<@jakarta.validation.Valid DisponibilidadeDiaDTO> disponibilidades,
        @io.swagger.v3.oas.annotations.media.Schema(description = "Versão lida antes da edição; obrigatória no PUT (ausente retorna 400, diferente da atual retorna 409). Ignorada no POST", example = "3")
        Long versao
) {
    public QuadraCriacaoDTO(String nome, TipoEsporte tipoEsporte, BigDecimal valorHora, String cep, String logradouro,
                            String bairro, String cidade, String estado, Double latitude, Double longitude, String descricao,
                            java.time.LocalDate dataLimiteAgendamento, List<String> fotos, List<DisponibilidadeDiaDTO> disponibilidades) {
        this(nome, tipoEsporte, valorHora, cep, logradouro, bairro, cidade, estado, latitude, longitude, descricao,
                dataLimiteAgendamento, fotos, disponibilidades, null);
    }
}
