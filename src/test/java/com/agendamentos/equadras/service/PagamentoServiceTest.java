package com.agendamentos.equadras.service;

import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.config.HttpClientConfig;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private JsonMapper objectMapper;
    private Agendamento agendamento;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().build();

        Usuario usuario = Usuario.builder()
                .id_usuario(1L)
                .nome_usuario("Carlos Teste")
                .email_usuario("carlos@teste.com")
                .role(Role.CLIENT)
                .build();

        Quadra quadra = Quadra.builder()
                .id_quadra(10L)
                .nome("Quadra Central")
                .valorHora(BigDecimal.valueOf(120.00))
                .build();

        agendamento = Agendamento.builder()
                .id_agendamento(100L)
                .usuario(usuario)
                .quadra(quadra)
                .dataHoraInicio(LocalDateTime.now().plusDays(1))
                .dataHoraFim(LocalDateTime.now().plusDays(1).plusHours(1))
                .valorTotal(BigDecimal.valueOf(120.00))
                .status(StatusAgendamento.PENDENTE)
                .build();
    }

    @Test
    @DisplayName("Deve gerar Pix mock quando access token for vazio")
    void deveGerarPixMockSemToken() {
        PagamentoService service = new PagamentoService("", httpClient, objectMapper);

        PagamentoService.PixDados pix = service.gerarPix(agendamento);

        assertNotNull(pix);
        assertTrue(pix.transacaoId().startsWith("MP-DEV-"));
        assertTrue(pix.pixCopiaECola().contains("br.gov.bcb.pix"));
        assertTrue(pix.qrCodeBase64().startsWith("data:image/svg+xml;utf8,<svg"));
        verifyNoInteractions(httpClient);
    }

    @Test
    @DisplayName("Deve gerar Pix via API oficial do Mercado Pago quando access token estiver configurado")
    void deveGerarPixMercadoPagoApi() throws Exception {
        String tokenReal = "APP_USR-123456789";
        PagamentoService service = new PagamentoService(tokenReal, httpClient, objectMapper);

        String jsonResponse = """
                {
                    "id": "1234567890",
                    "point_of_interaction": {
                        "transaction_data": {
                            "qr_code": "copia-e-cola-mp",
                            "qr_code_base64": "base64-mp"
                        }
                    }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        PagamentoService.PixDados pix = service.gerarPix(agendamento);

        assertNotNull(pix);
        assertEquals("1234567890", pix.transacaoId());
        assertEquals("copia-e-cola-mp", pix.pixCopiaECola());
        assertEquals("base64-mp", pix.qrCodeBase64());
        verify(httpClient, times(1)).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("Deve acionar fallback resiliente para Pix mock em caso de timeout em sandbox TEST-")
    void deveAcionarFallbackPixMockEmTimeoutSandbox() throws Exception {
        String tokenSandbox = "TEST-123456789";
        PagamentoService service = new PagamentoService(tokenSandbox, httpClient, objectMapper);

        doThrow(new HttpTimeoutException("Timeout")).when(httpClient).send(any(HttpRequest.class), any());

        PagamentoService.PixDados pix = service.gerarPix(agendamento);

        assertNotNull(pix);
        assertTrue(pix.transacaoId().startsWith("MP-DEV-"));
        assertTrue(pix.pixCopiaECola().contains("br.gov.bcb.pix"));
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException em caso de falha de gateway em produção")
    void deveLancarExcecaoEmFalhaProducao() throws Exception {
        String tokenProd = "APP_USR-999999999";
        PagamentoService service = new PagamentoService(tokenProd, httpClient, objectMapper);

        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("{\"error\": \"internal\"}");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        assertThrows(IllegalStateException.class, () -> service.gerarPix(agendamento));
    }

    @Test
    @DisplayName("Deve consultar pagamento Mercado Pago com sucesso quando aprovado")
    void deveConsultarPagamentoMercadoPagoAprovado() throws Exception {
        String tokenReal = "APP_USR-123456789";
        PagamentoService service = new PagamentoService(tokenReal, httpClient, objectMapper);

        String jsonResponse = """
                {
                    "id": "1234567890",
                    "status": "approved",
                    "status_detail": "accredited",
                    "external_reference": "100",
                    "transaction_amount": 120.00
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        Optional<PagamentoService.MercadoPagoStatus> statusOpt = service.consultarPagamentoMercadoPago("1234567890");

        assertTrue(statusOpt.isPresent());
        PagamentoService.MercadoPagoStatus status = statusOpt.get();
        assertEquals("1234567890", status.id());
        assertEquals("approved", status.status());
        assertEquals("accredited", status.statusDetail());
        assertEquals("100", status.externalReference());
        assertEquals(0, new BigDecimal("120.00").compareTo(status.transactionAmount()));
    }

    @Test
    @DisplayName("Deve tratar localmente pagamentos com prefixo MP-DEV- sem chamar HTTP")
    void deveTratarPagamentoDevLocalmente() {
        PagamentoService service = new PagamentoService("token", httpClient, objectMapper);

        Optional<PagamentoService.MercadoPagoStatus> statusOpt = service.consultarPagamentoMercadoPago("MP-DEV-abc12345");

        assertTrue(statusOpt.isPresent());
        PagamentoService.MercadoPagoStatus status = statusOpt.get();
        assertEquals("MP-DEV-abc12345", status.id());
        assertEquals("approved", status.status());
        assertEquals("accredited", status.statusDetail());
        verifyNoInteractions(httpClient);
    }

    @Test
    @DisplayName("Deve gerar Pix mock sem chamar HTTP quando token começar com TEST-MOCK")
    void deveGerarPixMockComTokenTestMock() {
        PagamentoService service = new PagamentoService("TEST-MOCK-123", httpClient, objectMapper);

        PagamentoService.PixDados pix = service.gerarPix(agendamento);

        assertTrue(pix.transacaoId().startsWith("MP-DEV-"));
        verifyNoInteractions(httpClient);
    }

    @Test
    @DisplayName("Deve enviar POST ao Mercado Pago com URI, headers e payload corretos")
    void deveEnviarRequisicaoPixCorreta() throws Exception {
        PagamentoService service = new PagamentoService("APP_USR-123", httpClient, objectMapper);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"id\":\"999\",\"point_of_interaction\":{\"transaction_data\":{\"qr_code\":\"qr\",\"qr_code_base64\":\"b64\"}}}");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        service.gerarPix(agendamento);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        HttpRequest req = captor.getValue();
        assertEquals("POST", req.method());
        assertEquals("https://api.mercadopago.com/v1/payments", req.uri().toString());
        assertEquals("Bearer APP_USR-123", req.headers().firstValue("Authorization").orElseThrow());
        assertEquals("eq-agendamento-100", req.headers().firstValue("X-Idempotency-Key").orElseThrow());
        assertEquals(HttpClientConfig.TIMEOUT, req.timeout().orElseThrow());

        JsonNode json = objectMapper.readTree(lerCorpo(req));
        assertEquals("pix", json.path("payment_method_id").asText());
        assertEquals(0, new BigDecimal("120.0").compareTo(json.path("transaction_amount").decimalValue()));
        assertEquals("100", json.path("external_reference").asText());
    }

    @Test
    @DisplayName("Deve acionar fallback mock quando sandbox retornar HTTP 500")
    void deveAcionarFallbackMockEmErroHttpSandbox() throws Exception {
        PagamentoService service = new PagamentoService("TEST-abc", httpClient, objectMapper);
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("erro");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        assertTrue(service.gerarPix(agendamento).transacaoId().startsWith("MP-DEV-"));
    }

    @Test
    @DisplayName("Deve acionar fallback mock quando sandbox lançar exceção genérica")
    void deveAcionarFallbackMockEmExcecaoGenericaSandbox() throws Exception {
        PagamentoService service = new PagamentoService("TEST-abc", httpClient, objectMapper);
        doThrow(new IOException("conexão recusada")).when(httpClient).send(any(HttpRequest.class), any());

        assertTrue(service.gerarPix(agendamento).transacaoId().startsWith("MP-DEV-"));
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException quando produção lançar exceção genérica")
    void deveLancarExcecaoEmExcecaoGenericaProducao() throws Exception {
        PagamentoService service = new PagamentoService("APP_USR-123", httpClient, objectMapper);
        doThrow(new IOException("conexão recusada")).when(httpClient).send(any(HttpRequest.class), any());

        assertThrows(IllegalStateException.class, () -> service.gerarPix(agendamento));
    }

    @Test
    @DisplayName("Deve retornar vazio na consulta quando token estiver vazio")
    void deveRetornarVazioSemToken() {
        PagamentoService service = new PagamentoService("", httpClient, objectMapper);

        assertTrue(service.consultarPagamentoMercadoPago("123").isEmpty());
        verifyNoInteractions(httpClient);
    }

    @Test
    @DisplayName("Deve retornar vazio na consulta quando o gateway responder 404")
    void deveRetornarVazioEmStatusNao2xx() throws Exception {
        PagamentoService service = new PagamentoService("APP_USR-123", httpClient, objectMapper);
        when(httpResponse.statusCode()).thenReturn(404);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        assertTrue(service.consultarPagamentoMercadoPago("123").isEmpty());
    }

    @Test
    @DisplayName("Deve retornar vazio na consulta quando o JSON for inválido")
    void deveRetornarVazioEmJsonInvalido() throws Exception {
        PagamentoService service = new PagamentoService("APP_USR-123", httpClient, objectMapper);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{invalido");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        assertTrue(service.consultarPagamentoMercadoPago("123").isEmpty());
    }

    @Test
    @DisplayName("Deve retornar amount e external_reference nulos quando ausentes")
    void deveRetornarAmountNuloQuandoAusente() throws Exception {
        PagamentoService service = new PagamentoService("APP_USR-123", httpClient, objectMapper);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"id\":\"123\",\"status\":\"pending\",\"status_detail\":\"waiting\"}");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        PagamentoService.MercadoPagoStatus status = service.consultarPagamentoMercadoPago("123").orElseThrow();

        assertNull(status.transactionAmount());
        assertNull(status.externalReference());
    }

    private static String lerCorpo(HttpRequest req) throws Exception {
        CompletableFuture<String> corpo = new CompletableFuture<>();
        req.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            private final StringBuilder sb = new StringBuilder();

            @Override
            public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }

            @Override
            public void onNext(ByteBuffer b) { sb.append(StandardCharsets.UTF_8.decode(b)); }

            @Override
            public void onError(Throwable t) { corpo.completeExceptionally(t); }

            @Override
            public void onComplete() { corpo.complete(sb.toString()); }
        });
        return corpo.get();
    }
}
