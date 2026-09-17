package com.agendamentos.equadras.security;

import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final ApiKeyService apiKeyService;
    private final ApiKeyRateLimiter rateLimiter;
    private final UsuarioRepository usuarioRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            ApiKeyService apiKeyService,
            ApiKeyRateLimiter rateLimiter,
            UsuarioRepository usuarioRepository,
            AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.apiKeyService = apiKeyService;
        this.rateLimiter = rateLimiter;
        this.usuarioRepository = usuarioRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String apiKeyHeader = request.getHeader("X-API-KEY");
        String authHeader = request.getHeader("Authorization");

        // 1. Processamento prioritário de API-KEY
        boolean hasApiKeyHeader = apiKeyHeader != null && !apiKeyHeader.isBlank();
        boolean hasAuthHeader = authHeader != null && !authHeader.isBlank();

        boolean rotaIsenta = SecurityRoutes.isRotaIsenta(request.getRequestURI());

        if (hasApiKeyHeader || hasAuthHeader) {
            // Se houver Authorization, deve ser Bearer eq_... (exceto em rotas isentas, ex: bot/webhook)
            if (hasAuthHeader && !authHeader.startsWith("Bearer eq_") && !rotaIsenta) {
                authenticationEntryPoint.commence(request, response,
                        new InsufficientAuthenticationException("Formato de token inválido ou legado. Para integrações, utilize o cabeçalho X-API-KEY com chave válida (eq_...)."));
                return;
            }

            String valorAuth = (hasAuthHeader && authHeader.startsWith("Bearer eq_")) ? authHeader.substring(7).trim() : null;
            String valorApiKey = hasApiKeyHeader ? apiKeyHeader.trim() : null;
            String ip = request.getRemoteAddr();

            // Se ambos os cabeçalhos estiverem presentes, seus valores devem ser idênticos
            if (valorAuth != null && valorApiKey != null && !valorAuth.equals(valorApiKey)) {
                rateLimiter.registrarFalha(ip);
                authenticationEntryPoint.commence(request, response,
                        new BadCredentialsException("Conflito de credenciais: cabeçalhos X-API-KEY e Authorization divergentes."));
                return;
            }

            String rawKey = (valorApiKey != null) ? valorApiKey : valorAuth;

            if (rawKey == null) {
                // Authorization presente porém sem prefixo válido, em rota isenta: segue para tentativa de cookie
                filterChain.doFilter(request, response);
                return;
            }

            // Verificação de Rate Limit por IP para falhas de API-KEY
            if (rateLimiter.isIpBloqueado(ip)) {
                long retryAfter = rateLimiter.getRetryAfterSegundos(ip);
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(retryAfter));
                response.setContentType("application/problem+json;charset=UTF-8");
                response.getWriter().write(String.format("""
                    {
                        "status": 429,
                        "title": "Too Many Requests",
                        "detail": "Limite de tentativas incorretas de API-KEY excedido para este IP. Tente novamente em %d segundos."
                    }
                    """, retryAfter));
                return;
            }

            Optional<Usuario> usuarioOpt = apiKeyService.autenticar(rawKey);
            if (usuarioOpt.isEmpty()) {
                rateLimiter.registrarFalha(ip);
                // Não tenta o cookie como fallback
                authenticationEntryPoint.commence(request, response,
                        new BadCredentialsException("Chave de API inválida, revogada ou usuário inativo."));
                return;
            }

            Usuario usuario = usuarioOpt.get();
            UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado(
                    usuario.getId_usuario(),
                    usuario.getRole(),
                    TipoAutenticacao.API_KEY
            );

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    usuarioAutenticado,
                    null,
                    List.of(
                            new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()),
                            new SimpleGrantedAuthority("SCOPE_API_KEY")
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Registro de último uso com throttling: falha aqui NÃO pode derrubar a requisição principal
            try {
                apiKeyService.registrarUso(usuario.getId_usuario());
            } catch (RuntimeException e) {
                log.warn("Falha não-bloqueante ao registrar último uso de API-KEY para usuarioId={}: {}",
                        usuario.getId_usuario(), e.getMessage());
            }

            filterChain.doFilter(request, response);
            return;
        }

        // 2. Processamento de Sessão Web via Cookie HttpOnly
        String cookieToken = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("equadras_session".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    cookieToken = c.getValue();
                    break;
                }
            }
        }

        if (cookieToken != null) {
            try {
                Claims claims = jwtService.validarEExtrairClaims(cookieToken);
                Long usuarioId = Long.valueOf(claims.getSubject());

                Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
                if (usuarioOpt.isPresent() && usuarioOpt.get().isAtivo()) {
                    Usuario usuario = usuarioOpt.get();
                    UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado(
                            usuario.getId_usuario(),
                            usuario.getRole(),
                            TipoAutenticacao.SESSAO_WEB
                    );

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            usuarioAutenticado,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()),
                                    new SimpleGrantedAuthority("SCOPE_SESSION")
                            )
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
