package com.agendamentos.equadras.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class ClientHeaderFilter extends OncePerRequestFilter {

    private static final Set<String> METODOS_MUTANTES = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String PREFIXO_BEARER_API_KEY = "Bearer eq_";

    private final AccessDeniedHandler accessDeniedHandler;

    public ClientHeaderFilter(AccessDeniedHandler accessDeniedHandler) {
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();

        if (METODOS_MUTANTES.contains(method)) {
            // Isenção exata para Webhook Mercado Pago e Bot Agendamentos
            boolean isWebhookOuBot = SecurityRoutes.isRotaIsenta(uri);

            // Isenção para requisições com credencial de integração válida:
            // X-API-KEY preenchido OU Authorization: Bearer eq_... (nunca Authorization genérico)
            String apiKeyHeader = request.getHeader("X-API-KEY");
            String authHeader = request.getHeader("Authorization");
            boolean possuiApiKey = apiKeyHeader != null && !apiKeyHeader.isBlank();
            boolean possuiBearerApiKey = authHeader != null
                    && authHeader.regionMatches(true, 0, PREFIXO_BEARER_API_KEY, 0, PREFIXO_BEARER_API_KEY.length());
            boolean possuiCredencialIntegracao = possuiApiKey || possuiBearerApiKey;

            if (!isWebhookOuBot && !possuiCredencialIntegracao) {
                String clientHeader = request.getHeader("X-Client");
                if (clientHeader == null || !clientHeader.trim().equalsIgnoreCase("frontend")) {
                    accessDeniedHandler.handle(request, response,
                            new AccessDeniedException("Cabeçalho X-Client obrigatório ausente ou inválido para requisições com mutação de estado."));
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
