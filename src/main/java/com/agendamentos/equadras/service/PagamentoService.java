package com.agendamentos.equadras.service;

import com.agendamentos.equadras.model.entity.Agendamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    @Value("${mercadopago.access.token:}")
    private String mercadoPagoAccessToken;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public record PixDados(String transacaoId, String pixCopiaECola, String qrCodeBase64) {}

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
        
        // Formatar valor com ponto decimal independente do locale do sistema
        String amountStr = String.format(Locale.US, "%.2f", agendamento.getValorTotal());
        String email = (agendamento.getUsuario().getEmail_usuario() != null && agendamento.getUsuario().getEmail_usuario().contains("@"))
                ? agendamento.getUsuario().getEmail_usuario()
                : "comprador_test@equadras.com";

        String jsonPayload = String.format(Locale.US, """
            {
                "transaction_amount": %s,
                "description": "Reserva %s - #%d",
                "payment_method_id": "pix",
                "payer": {
                    "email": "%s",
                    "first_name": "%s"
                }
            }
            """,
            amountStr,
            agendamento.getQuadra().getNome().replace("\"", ""),
            agendamento.getId_agendamento() != null ? agendamento.getId_agendamento() : 0,
            email,
            agendamento.getUsuario().getNome_usuario().replace("\"", "")
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mercadopago.com/v1/payments"))
                .header("Authorization", "Bearer " + mercadoPagoAccessToken)
                .header("Content-Type", "application/json")
                .header("X-Idempotency-Key", idempotencyKey)
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            String body = response.body();
            log.info("Resposta Mercado Pago Pix com sucesso!");
            String id = extractJsonField(body, "id");
            String qrCode = extractNestedJsonField(body, "qr_code");
            String qrCodeBase64 = extractNestedJsonField(body, "qr_code_base64");

            if (qrCode == null || qrCode.isBlank()) {
                qrCode = extractJsonField(body, "qr_code");
            }
            if (qrCodeBase64 == null || qrCodeBase64.isBlank()) {
                qrCodeBase64 = extractJsonField(body, "qr_code_base64");
            }

            return new PixDados(id, qrCode, qrCodeBase64);
        } else {
            log.warn("Mercado Pago retornou status {}: {}. Usando fallback dev para nao interromper a experiencia.", response.statusCode(), response.body());
            return gerarPixMock(agendamento);
        }
    }

    private PixDados gerarPixMock(Agendamento agendamento) {
        String mockId = "MP-DEV-" + UUID.randomUUID().toString().substring(0, 8);
        String mockCopiaECola = String.format("00020126580014br.gov.bcb.pix0136%s520400005303986540%s5802BR5913EQUADRAS APP6008SAO PAULO62070503***6304DEV",
                UUID.randomUUID(), agendamento.getValorTotal().toString());
        
        String svgQr = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='100' height='100' fill='white'/><rect x='10' y='10' width='30' height='30' fill='black'/><rect x='15' y='15' width='20' height='20' fill='white'/><rect x='20' y='20' width='10' height='10' fill='black'/><rect x='60' y='10' width='30' height='30' fill='black'/><rect x='65' y='15' width='20' height='20' fill='white'/><rect x='70' y='20' width='10' height='10' fill='black'/><rect x='10' y='60' width='30' height='30' fill='black'/><rect x='15' y='65' width='20' height='20' fill='white'/><rect x='20' y='70' width='10' height='10' fill='black'/><rect x='50' y='50' width='15' height='15' fill='black'/><rect x='70' y='70' width='15' height='15' fill='black'/><rect x='50' y='75' width='10' height='15' fill='black'/><rect x='75' y='50' width='15' height='10' fill='black'/></svg>";

        return new PixDados(mockId, mockCopiaECola, svgQr);
    }

    private String extractJsonField(String json, String field) {
        Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*\"?([^,\"}]+)\"?");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String extractNestedJsonField(String json, String field) {
        Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}
