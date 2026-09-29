package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.security.UsuarioLogado;
import com.agendamentos.equadras.service.AgendamentoService;
import com.agendamentos.equadras.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

    @Operation(summary = "Simular aprovação de pagamento Pix",
            description = "Papéis: CLIENT ou ADMIN. Move uma reserva PENDENTE para CONFIRMADO sem passar pelo gateway e notifica o administrador da quadra por SSE. Reserva cancelada devolve 400 `STATUS_INVALIDO`; confirmação concorrente, 409 `CONFLITO_STATUS`. Devolve o `AgendamentoResponseDTO` atualizado.")
    @PostMapping("/{agendamentoId}/simular-aprovacao")
    public ResponseEntity<AgendamentoResponseDTO> simularAprovacao(@Parameter(description = "ID do agendamento (`id_agendamento`)", example = "42") @PathVariable Long agendamentoId,
                                                                   @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        AgendamentoResponseDTO response = agendamentoService.confirmarPagamento(agendamentoId, usuarioLogado.id());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar status de pagamento da reserva",
            description = "Papéis: CLIENT ou ADMIN. Devolve o `AgendamentoResponseDTO` (o campo `status` é PENDENTE, CONFIRMADO ou CANCELADO). Efeito colateral: se a reserva está PENDENTE e tem `transacaoPagamentoId`, consulta o Mercado Pago e, se o pagamento estiver `approved`, confirma a reserva antes de responder. ID inexistente ou fora do seu escopo devolve 404.")
    @GetMapping("/{agendamentoId}/status")
    public ResponseEntity<AgendamentoResponseDTO> consultarStatus(@Parameter(description = "ID do agendamento (`id_agendamento`)", example = "42") @PathVariable Long agendamentoId,
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

    @Operation(summary = "Webhook do Mercado Pago",
            description = "Rota PÚBLICA, chamada pelo gateway. Identifica o pagamento por query (`topic=payment` ou `type=payment` com `id` ou `data.id`) ou pelo JSON (`type`/`action` contendo `payment` e `data.id`, ou `id`). Consulta o pagamento no Mercado Pago e, se `approved`, confirma o agendamento (`external_reference`). Respostas: `{status: ignored}` sem ID relevante; `{status: received}` se não aprovado; `{status: processed, payment_status: approved}` se confirmou; 500 `{status: error, message}` para o gateway reenviar.")
    @SecurityRequirements
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> webhook(@RequestBody(required = false) Map<String, Object> payload,
                                                        @Parameter(description = "ID do pagamento no formato IPN (usado com `topic=payment` ou `type=payment`).", example = "987654321")
                                                        @RequestParam(value = "id", required = false) String paramId,
                                                        @Parameter(description = "Tópico IPN. Só `payment` é processado.", example = "payment")
                                                        @RequestParam(value = "topic", required = false) String topic,
                                                        @Parameter(description = "Tipo do evento. Só valores contendo `payment` são processados.", example = "payment")
                                                        @RequestParam(value = "type", required = false) String type,
                                                        @Parameter(description = "ID do pagamento no formato Webhooks V2 (`data.id`), usado se `id` não vier.", example = "987654321")
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
                        } catch (NumberFormatException e) {
                            log.warn("Formato numérico inválido de externalReference [{}] no pagamento {}", mpStatus.externalReference(), paymentId);
                        }
                    }

                    try {
                        agendamentoService.confirmarPagamentoPorWebhook(agendamentoId, paymentId, mpStatus.transactionAmount());
                        log.info("Agendamento associado ao pagamento {} confirmado com sucesso via Webhook!", paymentId);
                        return ResponseEntity.ok(Map.of("status", "processed", "payment_status", "approved"));
                    } catch (Exception e) {
                        log.error("Falha ao confirmar pagamento via webhook para agendamento {} (paymentId {}): {}", agendamentoId, paymentId, e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("status", "error", "message", "Falha ao processar confirmação de pagamento. Solicitando retentativa."));
                    }
                }
            } else {
                log.warn("Não foi possível consultar os detalhes do pagamento {} junto ao Mercado Pago.", paymentId);
            }
        } catch (Exception e) {
            log.error("Erro inesperado ao processar webhook do Mercado Pago para paymentId {}", paymentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Erro interno ao processar webhook"));
        }

        return ResponseEntity.ok(Map.of("status", "received"));
    }
}
