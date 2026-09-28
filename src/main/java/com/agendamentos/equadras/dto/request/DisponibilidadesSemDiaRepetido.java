package com.agendamentos.equadras.dto.request;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Objects;

/** Validação de classe: cada dia da semana aparece uma vez, com o erro apontado para o campo disponibilidades. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DisponibilidadesSemDiaRepetido.Validador.class)
public @interface DisponibilidadesSemDiaRepetido {

    String message() default "Não é permitido repetir o dia da semana nas disponibilidades";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<DisponibilidadesSemDiaRepetido, QuadraCriacaoDTO> {
        @Override
        public boolean isValid(QuadraCriacaoDTO dto, ConstraintValidatorContext context) {
            if (dto == null || dto.disponibilidades() == null) return true;
            List<DayOfWeek> dias = dto.disponibilidades().stream()
                    .filter(Objects::nonNull)
                    .map(DisponibilidadeDiaDTO::diaSemana)
                    .filter(Objects::nonNull)
                    .toList();
            if (dias.stream().distinct().count() == dias.size()) return true;
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("disponibilidades")
                    .addConstraintViolation();
            return false;
        }
    }
}
