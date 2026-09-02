package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.service.AgendamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/pagamentos")
@CrossOrigin(origins = "*")
public class PagamentoController {

    private final AgendamentoService agendamentoService;

    public PagamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    /**
     * Endpoint para simulação de pagamento aprovado em ambiente Dev/Sandbox.
     */
    @PostMapping("/{agendamentoId}/simular-aprovacao")
    public ResponseEntity<AgendamentoResponseDTO> simularAprovacao(@PathVariable Long agendamentoId) {
        AgendamentoResponseDTO response = agendamentoService.confirmarPagamento(agendamentoId);
        return ResponseEntity.ok(response);
    }

    /**
     * Webhook padrão para notificações do gateway Mercado Pago.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> webhook(@RequestBody(required = false) Map<String, Object> payload,
                                                        @RequestParam(value = "id", required = false) String id,
                                                        @RequestParam(value = "topic", required = false) String topic) {
        // Log para auditoria do webhook
        System.out.println("Webhook Mercado Pago recebido. Topic: " + topic + " ID: " + id);

        return ResponseEntity.ok(Map.of("status", "received"));
    }
}
