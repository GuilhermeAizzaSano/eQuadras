package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.request.AgendamentoBotRequestDTO;
import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
import com.agendamentos.equadras.model.enums.Role;
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
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @Test
    @DisplayName("POST /api/agendamentos cria a reserva confirmada, sem Pix")
    void rotaApiConfirmaDireto() {
        controller.agendar(dto, cliente, new MockHttpServletRequest("POST", "/api/agendamentos"));

        verify(agendamentoService).agendarConfirmado(dto, 1L);
        verify(agendamentoService, never()).agendar(any(), any());
    }

    @Test
    @DisplayName("POST /agendamentos (frontend) mantém o fluxo com Pix")
    void rotaFrontendMantemPix() {
        controller.agendar(dto, cliente, new MockHttpServletRequest("POST", "/agendamentos"));

        verify(agendamentoService).agendar(dto, 1L);
        verify(agendamentoService, never()).agendarConfirmado(any(), any());
    }

    @Test
    @DisplayName("POST /api/agendamentos/bot confirma direto; /agendamentos/bot mantém o Pix")
    void botSegueOPrefixo() {
        controller.agendarViaBot(botDto, new MockHttpServletRequest("POST", "/api/agendamentos/bot"));
        verify(agendamentoBotService).agendarViaBot(botDto, true);

        controller.agendarViaBot(botDto, new MockHttpServletRequest("POST", "/agendamentos/bot"));
        verify(agendamentoBotService).agendarViaBot(botDto, false);
    }
}
