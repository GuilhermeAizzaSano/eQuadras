package com.agendamentos.equadras.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.data.autoconfigure.web.DataWebProperties;

@SpringBootTest
class PageablePropertiesIntegrationTest {

    @Autowired
    private DataWebProperties properties;

    @Test
    void deveLimitarTamanhoMaximoEConfigurarPadroesDePaginacao() {
        assertThat(properties.getPageable().getDefaultPageSize()).isEqualTo(10);
        assertThat(properties.getPageable().getMaxPageSize()).isEqualTo(50);
        assertThat(properties.getPageable().isOneIndexedParameters()).isFalse();
    }
}
