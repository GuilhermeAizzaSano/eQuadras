package com.agendamentos.equadras.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditoriaListenerFaseTest {

    static Stream<Arguments> handlers() {
        return Stream.of(QuadraAuditoriaListener.class, BloqueioHorarioAuditoriaListener.class,
                        AgendamentoAuditoriaListener.class, AgendamentoNotificacaoListener.class)
                .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                .filter(m -> m.isAnnotationPresent(TransactionalEventListener.class))
                .map(m -> Arguments.of(m.getDeclaringClass().getSimpleName() + "#" + m.getName(), m));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("handlers")
    @DisplayName("Listeners que gravam no banco rodam antes do commit, na mesma transação")
    void listenersRodamAntesDoCommit(String nome, Method metodo) {
        assertEquals(TransactionPhase.BEFORE_COMMIT,
                metodo.getAnnotation(TransactionalEventListener.class).phase(), nome);
    }
}
