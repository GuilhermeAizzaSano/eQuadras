package com.agendamentos.equadras.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de proteção contra ataques de força bruta (brute-force), scraping excessivo e DoS.
 * Implementa o algoritmo Token Bucket de forma concorrente e thread-safe.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private static final int MAX_TRACKED_IPS = 5000;

    public static class TokenBucket {
        private final double capacity;
        private final double refillTokensPerSecond;
        private double tokens;
        private long lastRefillTimestampNanos;

        public TokenBucket(double capacity, double refillTokensPerSecond) {
            this.capacity = capacity;
            this.refillTokensPerSecond = refillTokensPerSecond;
            this.tokens = capacity;
            this.lastRefillTimestampNanos = System.nanoTime();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillTimestampNanos) / 1_000_000_000.0;
            if (elapsedSeconds > 0) {
                tokens = Math.min(capacity, tokens + elapsedSeconds * refillTokensPerSecond);
                lastRefillTimestampNanos = now;
            }
        }

        public synchronized long secondsUntilAvailable() {
            if (tokens >= 1.0) return 0;
            double needed = 1.0 - tokens;
            return Math.max(1, (long) Math.ceil(needed / refillTokensPerSecond));
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Requisições OPTIONS (CORS preflight) não consomem quota de rate limit
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        if (uri == null) {
            uri = "";
        }

        // Isenção para arquivos estáticos e documentação Swagger
        if (uri.startsWith("/uploads/") || uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs") || uri.startsWith("/h2-console")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extrairClientIp(request);
        String categoria;
        double capacidade;
        double recargaPorSegundo;

        if (uri.endsWith("/usuarios/login") || uri.endsWith("/usuarios/login/")) {
            // Limite rigoroso para login: 10 requisições por minuto
            categoria = "LOGIN";
            capacidade = 10.0;
            recargaPorSegundo = 10.0 / 60.0;
        } else if (uri.endsWith("/agendamentos/bot") || uri.endsWith("/agendamentos/bot/")) {
            // Limite para agendamento via Bot: 20 requisições por minuto
            categoria = "BOT";
            capacidade = 20.0;
            recargaPorSegundo = 20.0 / 60.0;
        } else {
            // Limite para requisições gerais da API: 120 requisições por minuto por IP
            categoria = "GERAL";
            capacidade = 120.0;
            recargaPorSegundo = 120.0 / 60.0;
        }

        // Limpeza de segurança se houver muitos IPs rastreados
        if (buckets.size() > MAX_TRACKED_IPS) {
            buckets.clear();
        }

        String cacheKey = categoria + ":" + clientIp;
        TokenBucket bucket = buckets.computeIfAbsent(cacheKey, k -> new TokenBucket(capacidade, recargaPorSegundo));

        if (!bucket.tryConsume()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(bucket.secondsUntilAvailable()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            String jsonError = """
                {
                  "type": "https://api.equadras.com/erros/too-many-requests",
                  "title": "Limite de Requisições Excedido",
                  "status": 429,
                  "detail": "Você enviou muitas requisições em um curto período de tempo. Por favor, aguarde alguns segundos antes de tentar novamente."
                }
                """;
            response.getWriter().write(jsonError);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extrairClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "desconhecido";
    }
}
