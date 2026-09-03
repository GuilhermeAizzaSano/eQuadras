package com.agendamentos.equadras.service;

import com.agendamentos.equadras.model.entity.Agendamento;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    @Value("${mercadopago.access.token:}")
    private String mercadoPagoAccessToken;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public record PixDados(String transacaoId, String pixCopiaECola, String qrCodeBase64) {}

    public record MercadoPagoStatus(String id, String status, String statusDetail, String externalReference) {}

    /**
     * Gera uma cobranca Pix. Se um access token real for fornecido, chama a API oficial do Mercado Pago.
     * Caso contrario, gera uma cobranca Pix mock formatada com QR Code para testes em desenvolvimento local.
     */
    public PixDados gerarPix(Agendamento agendamento) {
        if (mercadoPagoAccessToken != null && !mercadoPagoAccessToken.isBlank() && !mercadoPagoAccessToken.startsWith("TEST-MOCK")) {
            try {
                return gerarPixMercadoPagoApi(agendamento);
            } catch (Exception e) {
                log.error("Erro ao chamar API do Mercado Pago. Usando gerador de contingencia/mock para Dev.", e);
            }
        }

        return gerarPixMock(agendamento);
    }

    private PixDados gerarPixMercadoPagoApi(Agendamento agendamento) throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();

        // Tratamento seguro de e-mail do comprador para ambiente de teste / sandbox do Mercado Pago
        // O Mercado Pago rejeita pagamentos com o mesmo e-mail do vendedor (Invalid users involved - code 2034)
        // e rejeita domínios @testuser.com não gerados na mesma aplicação (code 4390)
        String email = agendamento.getUsuario().getEmail_usuario();
        if (email == null || !email.contains("@") || email.endsWith("@equadras.com") || email.contains("testuser.com")) {
            email = "cliente_pagador_test@gmail.com";
        }

        String nome = agendamento.getUsuario().getNome_usuario();
        if (nome == null || nome.isBlank()) {
            nome = "Cliente";
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.put("transaction_amount", agendamento.getValorTotal());
        root.put("description", String.format("Reserva %s - #%s",
                agendamento.getQuadra().getNome(),
                agendamento.getId_agendamento() != null ? agendamento.getId_agendamento() : "0"));
        root.put("payment_method_id", "pix");
        if (agendamento.getId_agendamento() != null) {
            root.put("external_reference", String.valueOf(agendamento.getId_agendamento()));
        }

        ObjectNode payer = root.putObject("payer");
        payer.put("email", email);
        payer.put("first_name", nome);

        String jsonPayload = objectMapper.writeValueAsString(root);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mercadopago.com/v1/payments"))
                .header("Authorization", "Bearer " + mercadoPagoAccessToken)
                .header("Content-Type", "application/json")
                .header("X-Idempotency-Key", idempotencyKey)
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode resNode = objectMapper.readTree(response.body());
            log.info("Resposta Mercado Pago Pix com sucesso! Payment ID: {}", resNode.path("id").asText());

            String id = resNode.path("id").asText();
            JsonNode poiNode = resNode.path("point_of_interaction").path("transaction_data");
            String qrCode = poiNode.path("qr_code").asText("");
            String qrCodeBase64 = poiNode.path("qr_code_base64").asText("");

            if (qrCode.isBlank()) {
                qrCode = resNode.path("qr_code").asText("");
            }
            if (qrCodeBase64.isBlank()) {
                qrCodeBase64 = resNode.path("qr_code_base64").asText("");
            }

            return new PixDados(id, qrCode, qrCodeBase64);
        } else {
            log.warn("Mercado Pago retornou status {}: {}. Usando fallback dev para nao interromper a experiencia.", response.statusCode(), response.body());
            return gerarPixMock(agendamento);
        }
    }

    /**
     * Consulta os dados de uma transação diretamente na API do Mercado Pago.
     */
    public Optional<MercadoPagoStatus> consultarPagamentoMercadoPago(String paymentId) {
        if (mercadoPagoAccessToken == null || mercadoPagoAccessToken.isBlank() || paymentId == null || paymentId.isBlank()) {
            return Optional.empty();
        }

        // Se for um ID mock gerado pelo dev, trata localmente
        if (paymentId.startsWith("MP-DEV-")) {
            return Optional.of(new MercadoPagoStatus(paymentId, "approved", "accredited", null));
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mercadopago.com/v1/payments/" + paymentId.trim()))
                    .header("Authorization", "Bearer " + mercadoPagoAccessToken)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                String id = root.path("id").asText();
                String status = root.path("status").asText();
                String statusDetail = root.path("status_detail").asText();
                String externalReference = root.path("external_reference").isMissingNode() || root.path("external_reference").isNull()
                        ? null
                        : root.path("external_reference").asText();

                return Optional.of(new MercadoPagoStatus(id, status, statusDetail, externalReference));
            } else {
                log.warn("Falha ao consultar pagamento {} no MP. Status: {}", paymentId, response.statusCode());
            }
        } catch (Exception e) {
            log.error("Erro ao consultar pagamento no Mercado Pago: {}", paymentId, e);
        }

        return Optional.empty();
    }

    private PixDados gerarPixMock(Agendamento agendamento) {
        String mockId = "MP-DEV-" + UUID.randomUUID().toString().substring(0, 8);
        String mockCopiaECola = String.format("00020126580014br.gov.bcb.pix0136%s520400005303986540%s5802BR5913EQUADRAS APP6008SAO PAULO62070503***6304DEV",
                UUID.randomUUID(), agendamento.getValorTotal() != null ? agendamento.getValorTotal().toString() : "0.00");

        String svgQr = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='100' height='100' fill='white'/><rect x='10' y='10' width='30' height='30' fill='black'/><rect x='15' y='15' width='20' height='20' fill='white'/><rect x='20' y='20' width='10' height='10' fill='black'/><rect x='60' y='10' width='30' height='30' fill='black'/><rect x='65' y='15' width='20' height='20' fill='white'/><rect x='70' y='20' width='10' height='10' fill='black'/><rect x='10' y='60' width='30' height='30' fill='black'/><rect x='15' y='65' width='20' height='20' fill='white'/><rect x='20' y='70' width='10' height='10' fill='black'/><rect x='50' y='50' width='15' height='15' fill='black'/><rect x='70' y='70' width='15' height='15' fill='black'/><rect x='50' y='75' width='10' height='15' fill='black'/><rect x='75' y='50' width='15' height='10' fill='black'/></svg>";

        return new PixDados(mockId, mockCopiaECola, svgQr);
    }
}
