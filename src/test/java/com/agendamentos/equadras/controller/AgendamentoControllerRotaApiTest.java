package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.request.AgendamentoBotRequestDTO;
import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.dto.response.ReservaConfirmadaResponseDTO;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.service.AgendaConsultaService;
import com.agendamentos.equadras.service.AgendamentoBotService;
import com.agendamentos.equadras.service.AgendamentoService;
import com.agendamentos.equadras.service.DashboardService;
import com.agendamentos.equadras.service.GradeHorariosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendamentoControllerRotaApiTest {

    @Mock private AgendamentoService agendamentoService;
    @Mock private DashboardService dashboardService;
    @Mock private AgendamentoBotService agendamentoBotService;
    @Mock private GradeHorariosService gradeHorariosService;
    @Mock private AgendaConsultaService agendaConsultaService;

    private AgendamentoController controller;

    private final UsuarioAutenticado cliente = new UsuarioAutenticado(1L, Role.CLIENT);
    private final AgendamentoCriacaoDTO dto = new AgendamentoCriacaoDTO(null, 1L,
            LocalDateTime.of(2026, 10, 12, 19, 0), LocalDateTime.of(2026, 10, 12, 20, 0));
    private final AgendamentoBotRequestDTO botDto = new AgendamentoBotRequestDTO(
            1L, null, null, "amanha", "19h", null, "Robson", "11999998888");

    @BeforeEach
    void setUp() {
        controller = new AgendamentoController(agendamentoService, dashboardService, agendamentoBotService,
                gradeHorariosService, agendaConsultaService);
    }

    private final AgendamentoResponseDTO confirmada = new AgendamentoResponseDTO(42L, 1L, "Robson", "11999998888",
            1L, "Arena", LocalDateTime.of(2026, 10, 12, 19, 0), LocalDateTime.of(2026, 10, 12, 20, 0),
            new BigDecimal("120.00"), StatusAgendamento.CONFIRMADO, null, null, null,
            LocalDateTime.of(2026, 10, 5, 10, 0), null);

    @Test
    @DisplayName("POST /api/agendamentos cria a reserva confirmada e devolve o DTO sem campos de pagamento")
    void rotaApiConfirmaDireto() {
        when(agendamentoService.agendarConfirmado(dto, 1L)).thenReturn(confirmada);

        ResponseEntity<?> resposta = controller.agendar(dto, cliente, new MockHttpServletRequest("POST", "/api/agendamentos"));

        verify(agendamentoService, never()).agendar(any(), any());
        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertEquals(ReservaConfirmadaResponseDTO.de(confirmada), resposta.getBody());
    }

    @Test
    @DisplayName("POST /agendamentos (frontend) mantém o fluxo com Pix")
    void rotaFrontendMantemPix() {
        when(agendamentoService.agendar(dto, 1L)).thenReturn(confirmada);

        ResponseEntity<?> resposta = controller.agendar(dto, cliente, new MockHttpServletRequest("POST", "/agendamentos"));

        verify(agendamentoService, never()).agendarConfirmado(any(), any());
        assertInstanceOf(AgendamentoResponseDTO.class, resposta.getBody());
    }

    @Test
    @DisplayName("POST /api/agendamentos/bot confirma direto sem campos de pagamento; /agendamentos/bot mantém o Pix")
    void botSegueOPrefixo() {
        when(agendamentoBotService.agendarViaBot(botDto, true)).thenReturn(confirmada);
        when(agendamentoBotService.agendarViaBot(botDto, false)).thenReturn(confirmada);

        ResponseEntity<?> api = controller.agendarViaBot(botDto, new MockHttpServletRequest("POST", "/api/agendamentos/bot"));
        assertEquals(ReservaConfirmadaResponseDTO.de(confirmada), api.getBody());

        ResponseEntity<?> frontend = controller.agendarViaBot(botDto, new MockHttpServletRequest("POST", "/agendamentos/bot"));
        assertInstanceOf(AgendamentoResponseDTO.class, frontend.getBody());
    }
}
