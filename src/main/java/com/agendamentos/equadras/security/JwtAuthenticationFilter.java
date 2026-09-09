package com.agendamentos.equadras.security;

import com.agendamentos.equadras.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 1. Extrai token do cookie de sessão HttpOnly
        String cookieToken = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("equadras_session".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    cookieToken = c.getValue();
                    break;
                }
            }
        }

        // 2. Extrai token do cabeçalho Authorization
        String headerToken = null;
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            headerToken = header.substring(7);
        }

        // 3. Suporte a SSE (/notificacoes/stream) via query parameter
        String sseToken = null;
        if (uri != null && uri.contains("/notificacoes/stream") && request.getParameter("token") != null) {
            sseToken = request.getParameter("token");
        }

        // Determina o token a ser validado
        String token = (headerToken != null) ? headerToken : ((cookieToken != null) ? cookieToken : sseToken);

        if (token != null) {
            try {
                Claims claims = jwtService.validarEExtrairClaims(token);
                Long usuarioId = Long.valueOf(claims.getSubject());
                Role role = Role.valueOf(claims.get("role", String.class));
                UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado(usuarioId, role);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        usuarioAutenticado,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }

        // 4. Verificação de segurança para rotas internas consultadas pelo Frontend (rotas não /api)
        // Permite requisições que possuam sessão válida (cookie ou token Bearer)
        boolean isApiRoute = uri != null && (uri.startsWith("/api/") || uri.equals("/api"));
        if (!isApiRoute && !isPublicFrontendRoute(uri, method)) {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/problem+json;charset=UTF-8");
                response.getWriter().write("""
                    {
                        "status": 401,
                        "title": "Acesso não autorizado",
                        "detail": "Acesso restrito: para consultar recursos do sistema é obrigatório possuir uma autenticação válida (sessão ativa com cookie de login ou token de autorização). Requisições não autenticadas foram recusadas."
                    }
                    """);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicFrontendRoute(String uri, String method) {
        if (uri == null) return true;
        if ("OPTIONS".equalsIgnoreCase(method)) return true;

        // Login, auto-cadastro e logout
        if ("POST".equalsIgnoreCase(method) && (uri.equals("/usuarios") || uri.equals("/usuarios/"))) return true;
        if ("POST".equalsIgnoreCase(method) && (uri.equals("/usuarios/login") || uri.equals("/usuarios/login/"))) return true;
        if ("POST".equalsIgnoreCase(method) && (uri.equals("/usuarios/logout") || uri.equals("/usuarios/logout/"))) return true;

        // Webhook Mercado Pago e Bot
        if ("POST".equalsIgnoreCase(method) && uri.contains("/pagamentos/webhook")) return true;
        if ("POST".equalsIgnoreCase(method) && uri.contains("/agendamentos/bot")) return true;

        // Uploads de arquivos e fotos
        if (uri.startsWith("/uploads/")) return true;

        // Documentação OpenAPI e Swagger UI
        if (uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui") || uri.contains("swagger")) return true;
        if (uri.startsWith("/h2-console")) return true;
        if (uri.startsWith("/error")) return true;

        // Recursos estáticos
        if (uri.startsWith("/assets/") || uri.endsWith(".ico") || uri.endsWith(".png") || uri.endsWith(".jpg")
                || uri.endsWith(".svg") || uri.endsWith(".js") || uri.endsWith(".css") || uri.endsWith(".html")) {
            return true;
        }

        return false;
    }
}
