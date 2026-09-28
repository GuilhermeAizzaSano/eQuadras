package com.agendamentos.equadras.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private static final int LIMITE_LOGIN_POR_MINUTO = 10;

    private final RateLimitFilter filtro = new RateLimitFilter();

    private MockHttpServletResponse enviarLogin(String remoteAddr, String xForwardedFor) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/usuarios/login");
        request.setRemoteAddr(remoteAddr);
        if (xForwardedFor != null) {
            request.addHeader("X-Forwarded-For", xForwardedFor);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void deveIgnorarXForwardedForForjadoPeloCliente() throws Exception {
        for (int i = 0; i < LIMITE_LOGIN_POR_MINUTO; i++) {
            assertThat(enviarLogin("203.0.113.10", "10.0.0." + i).getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse bloqueada = enviarLogin("203.0.113.10", "10.0.0.99");

        assertThat(bloqueada.getStatus()).isEqualTo(429);
        assertThat(bloqueada.getHeader("Retry-After")).isNotNull();
    }

    @Test
    void deveManterBaldesSeparadosParaIpsReaisDiferentes() throws Exception {
        for (int i = 0; i < LIMITE_LOGIN_POR_MINUTO; i++) {
            enviarLogin("203.0.113.10", null);
        }

        assertThat(enviarLogin("203.0.113.10", null).getStatus()).isEqualTo(429);
        assertThat(enviarLogin("198.51.100.20", null).getStatus()).isEqualTo(200);
    }
}
