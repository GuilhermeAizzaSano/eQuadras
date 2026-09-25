package com.agendamentos.equadras.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest
class PagamentoServiceContextTest {

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    @DisplayName("Deve usar o JsonMapper gerenciado pelo Spring")
    void deveUsarJsonMapperDoSpring() {
        assertSame(jsonMapper, ReflectionTestUtils.getField(pagamentoService, "objectMapper"));
    }
}
