package com.agendamentos.equadras.dto.request;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Validação de classe: horaFim posterior a horaInicio, com o erro apontado para o campo horaFim. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IntervaloHorarioValido.Validador.class)
public @interface IntervaloHorarioValido {

    String message() default "O horário de fim deve ser posterior ao de início";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<IntervaloHorarioValido, DisponibilidadeDiaDTO> {
        @Override
        public boolean isValid(DisponibilidadeDiaDTO dto, ConstraintValidatorContext context) {
            if (dto == null || dto.horaInicio() == null || dto.horaFim() == null || dto.horaFim().isAfter(dto.horaInicio())) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("horaFim")
                    .addConstraintViolation();
            return false;
        }
    }
}
