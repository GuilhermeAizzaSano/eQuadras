package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.service.NotificacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotificacaoControllerTest {

    @Test
    @DisplayName("B6: size acima de 50 é limitado a 50")
    void sizeLimitadoA50() {
        NotificacaoService service = mock(NotificacaoService.class);
        when(service.listarPorAdmin(eq(1L), any(Pageable.class))).thenReturn(Page.empty());
        NotificacaoController controller = new NotificacaoController(service);

        controller.listarPorAdmin(new UsuarioAutenticado(1L, Role.ADMIN), 0, 100000);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).listarPorAdmin(eq(1L), captor.capture());
        assertEquals(50, captor.getValue().getPageSize());
    }
}
