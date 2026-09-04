package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.security.UsuarioLogado;
import com.agendamentos.equadras.service.AgendamentoService;
import com.agendamentos.equadras.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Tag(name = "Pagamentos (Integração e Webhook)", description = "Endpoints para lidar com simulações, webhook oficial e consultas de status via Mercado Pago.")
@RestController
@RequestMapping({"/pagamentos", "/api/pagamentos"})
public class PagamentoController {

    private static final Logger log = LoggerFactory.getLogger(PagamentoController.class);

    private final AgendamentoService agendamentoService;
    private final PagamentoService pagamentoService;

    public PagamentoController(AgendamentoService agendamentoService, PagamentoService pagamentoService) {
        this.agendamentoService = agendamentoService;
        this.pagamentoService = pagamentoService;
    }

    @Operation(summary = "Simular aprovação de pagamento Pix (Dev)", description = "Transita uma reserva pendente para CONFIRMADO e notifica o administrador via SSE.")
    @com.agendamentos.equadras.config.DevOnly
    @PostMapping("/{agendamentoId}/simular-aprovacao")
    public ResponseEntity<AgendamentoResponseDTO> simularAprovacao(@PathVariable Long agendamentoId,
                                                                   @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        AgendamentoResponseDTO response = agendamentoService.confirmarPagamento(agendamentoId, usuarioLogado.id());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar status de pagamento da reserva", description = "Verifica se o agendamento já foi confirmado ou se o pagamento foi aprovado no gateway.")
    @GetMapping("/{agendamentoId}/status")
    public ResponseEntity<AgendamentoResponseDTO> consultarStatus(@PathVariable Long agendamentoId,
                                                                  @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        AgendamentoResponseDTO agendamento = agendamentoService.buscarPorId(agendamentoId, usuarioLogado.id());

        // Se ainda estiver pendente e possuir ID de transação, faz double-check na API do Mercado Pago
        if (agendamento.status() == com.agendamentos.equadras.model.enums.StatusAgendamento.PENDENTE
                && agendamento.transacaoPagamentoId() != null
                && !agendamento.transacaoPagamentoId().isBlank()) {
            Optional<PagamentoService.MercadoPagoStatus> mpStatusOpt =
                    pagamentoService.consultarPagamentoMercadoPago(agendamento.transacaoPagamentoId());

            if (mpStatusOpt.isPresent() && "approved".equalsIgnoreCase(mpStatusOpt.get().status())) {
                log.info("Double-check confirmou pagamento aprovado para agendamento {}", agendamentoId);
                AgendamentoResponseDTO confirmado = agendamentoService.confirmarPagamentoPorWebhook(
                        agendamentoId, agendamento.transacaoPagamentoId()
                );
                return ResponseEntity.ok(confirmado);
            }
        }

        return ResponseEntity.ok(agendamento);
    }

    @Operation(summary = "Webhook do Mercado Pago", description = "Recepção de notificações assíncronas de pagamento instantâneo do gateway.")
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> webhook(@RequestBody(required = false) Map<String, Object> payload,
                                                        @RequestParam(value = "id", required = false) String paramId,
                                                        @RequestParam(value = "topic", required = false) String topic,
                                                        @RequestParam(value = "type", required = false) String type,
                                                        @RequestParam(value = "data.id", required = false) String dataIdParam) {
        String paymentId = null;

        // 1. Identificação via query params (IPN tradicional Mercado Pago)
        if ("payment".equalsIgnoreCase(topic) || "payment".equalsIgnoreCase(type)) {
            paymentId = paramId != null ? paramId : dataIdParam;
        }

        // 2. Identificação via JSON payload (Webhooks V2 do Mercado Pago: action = payment.created / payment.updated)
        if (paymentId == null && payload != null) {
            String typeFromPayload = String.valueOf(payload.getOrDefault("type", payload.getOrDefault("action", "")));
            if (typeFromPayload.contains("payment")) {
                Object dataObj = payload.get("data");
                if (dataObj instanceof Map<?, ?> dataMap) {
                    Object idVal = dataMap.get("id");
                    if (idVal != null) {
                        paymentId = String.valueOf(idVal);
                    }
                }
            }
            if (paymentId == null && payload.containsKey("id")) {
                paymentId = String.valueOf(payload.get("id"));
            }
        }

        if (paymentId == null || paymentId.isBlank() || "null".equalsIgnoreCase(paymentId)) {
            log.info("Webhook Mercado Pago recebido sem ID de pagamento relevante (Topic: {}, Type: {})", topic, type);
            return ResponseEntity.ok(Map.of("status", "ignored"));
        }

        log.info("Webhook Mercado Pago processando pagamento ID: {}", paymentId);

        try {
            Optional<PagamentoService.MercadoPagoStatus> statusOpt = pagamentoService.consultarPagamentoMercadoPago(paymentId);
            if (statusOpt.isPresent()) {
                PagamentoService.MercadoPagoStatus mpStatus = statusOpt.get();
                log.info("Status do pagamento {} no Mercado Pago: {} ({})", paymentId, mpStatus.status(), mpStatus.statusDetail());

                if ("approved".equalsIgnoreCase(mpStatus.status())) {
                    Long agendamentoId = null;
                    if (mpStatus.externalReference() != null && !mpStatus.externalReference().isBlank()) {
                        try {
                            agendamentoId = Long.parseLong(mpStatus.externalReference().trim());
                        } catch (NumberFormatException ignored) {}
                    }

                    agendamentoService.confirmarPagamentoPorWebhook(agendamentoId, paymentId);
                    log.info("Agendamento associado ao pagamento {} confirmado com sucesso via Webhook!", paymentId);
                    return ResponseEntity.ok(Map.of("status", "processed", "payment_status", "approved"));
                }
            } else {
                log.warn("Não foi possível consultar os detalhes do pagamento {} junto ao Mercado Pago.", paymentId);
            }
        } catch (Exception e) {
            log.error("Erro ao processar webhook do Mercado Pago para paymentId {}", paymentId, e);
        }

        return ResponseEntity.ok(Map.of("status", "received"));
    }
}
