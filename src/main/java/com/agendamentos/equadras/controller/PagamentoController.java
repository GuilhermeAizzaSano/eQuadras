package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.security.UsuarioLogado;
import com.agendamentos.equadras.service.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Pagamentos e Webhooks", description = "Endpoints de integração de pagamentos Pix Mercado Pago e simulação de confirmação em ambiente de desenvolvimento.")
@RestController
@RequestMapping("/pagamentos")
public class PagamentoController {

    private final AgendamentoService agendamentoService;

    public PagamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @Operation(summary = "Simular aprovação de pagamento Pix (Dev)", description = "Transita uma reserva pendente para CONFIRMADO e notifica o administrador via SSE.")
    @com.agendamentos.equadras.config.DevOnly
    @PostMapping("/{agendamentoId}/simular-aprovacao")
    public ResponseEntity<AgendamentoResponseDTO> simularAprovacao(@PathVariable Long agendamentoId,
                                                                   @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        AgendamentoResponseDTO response = agendamentoService.confirmarPagamento(agendamentoId, usuarioLogado.id());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Webhook do Mercado Pago", description = "Recepção de notificações assíncronas de pagamento instantâneo do gateway.")
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> webhook(@RequestBody(required = false) Map<String, Object> payload,
                                                        @RequestParam(value = "id", required = false) String id,
                                                        @RequestParam(value = "topic", required = false) String topic) {
        System.out.println("Webhook Mercado Pago recebido. Topic: " + topic + " ID: " + id);
        return ResponseEntity.ok(Map.of("status", "received"));
    }
}
