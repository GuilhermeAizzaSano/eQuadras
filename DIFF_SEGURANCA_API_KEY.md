# Relatório de Diff: Separação de Sessão Web e API-KEY

Este documento consolida as alterações e os novos arquivos implementados para a separação estrita entre a Sessão Web (cookie HttpOnly `equadras_session` com JWT efêmero) e chaves de integração externas (API-KEY opaca `eq_...` com hash SHA-256 no banco).

---

## 1. `SecurityConfig.java` (Modificado)
**Caminho:** `src/main/java/com/agendamentos/equadras/config/SecurityConfig.java`  
**Objetivo:** Implementar negação por padrão (`anyRequest().hasAuthority("SCOPE_SESSION")`), garantindo que a API-KEY acesse exclusivamente rotas de negócio (`/quadras/**`, `/agendamentos/**`, `/pagamentos/**`) respeitando os papéis `ADMIN`/`CLIENT`, e encadear o `ClientHeaderFilter` e `JwtAuthenticationFilter`.

```diff
diff --git a/src/main/java/com/agendamentos/equadras/config/SecurityConfig.java b/src/main/java/com/agendamentos/equadras/config/SecurityConfig.java
index cc51a41..74c39e9 100644
--- a/src/main/java/com/agendamentos/equadras/config/SecurityConfig.java
+++ b/src/main/java/com/agendamentos/equadras/config/SecurityConfig.java
@@ -1,9 +1,13 @@
 package com.agendamentos.equadras.config;
 
-import com.agendamentos.equadras.security.JwtAuthenticationFilter;
-import com.agendamentos.equadras.security.JwtService;
+import com.agendamentos.equadras.repository.UsuarioRepository;
+import com.agendamentos.equadras.security.*;
+import org.springframework.beans.factory.annotation.Value;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
+import org.springframework.http.HttpMethod;
+import org.springframework.security.config.Customizer;
+import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
 import org.springframework.security.config.annotation.web.builders.HttpSecurity;
 import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
 import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
@@ -12,12 +16,19 @@ import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
 import org.springframework.security.crypto.password.PasswordEncoder;
 import org.springframework.security.web.SecurityFilterChain;
 import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
+import org.springframework.web.cors.CorsConfiguration;
+import org.springframework.web.cors.CorsConfigurationSource;
+import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
+
+import java.util.Arrays;
+import java.util.List;
 
 @Configuration
 @EnableWebSecurity
+@EnableMethodSecurity
 public class SecurityConfig {
 
-    @org.springframework.beans.factory.annotation.Value("${equadras.cors.origens-permitidas:http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173,http://127.0.0.1:3000,https://equadras.app,https://www.equadras.app}")
+    @Value("${equadras.cors.origens-permitidas:http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173,http://127.0.0.1:3000,https://equadras.app,https://www.equadras.app}")
     private String[] origensPermitidas;
 
     @Bean
@@ -43,4 +55,22 @@ public class SecurityConfig {
     @Bean
-    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
+    public SecurityFilterChain filterChain(
+            HttpSecurity http,
+            JwtService jwtService,
+            ApiKeyService apiKeyService,
+            ApiKeyRateLimiter apiKeyRateLimiter,
+            UsuarioRepository usuarioRepository,
+            JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint,
+            JsonAccessDeniedHandler jsonAccessDeniedHandler) throws Exception {
+
+        ClientHeaderFilter clientHeaderFilter = new ClientHeaderFilter();
+        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
+                jwtService,
+                apiKeyService,
+                apiKeyRateLimiter,
+                usuarioRepository,
+                jsonAuthenticationEntryPoint
+        );
+
         http
+                // Proteção CSRF: ClientHeaderFilter (exigência estrita de X-Client) + CORS restrito para métodos mutantes
                 .csrf(AbstractHttpConfigurer::disable)
@@ -49,42 +79,36 @@ public class SecurityConfig {
                 .authorizeHttpRequests(auth -> auth
-                        // Públicos
-                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/usuarios/login", "/api/usuarios/login").permitAll()
-                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/usuarios/logout", "/api/usuarios/logout").permitAll()
-                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/agendamentos/bot", "/api/agendamentos/bot").permitAll()
-                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/pagamentos/webhook", "/api/pagamentos/webhook").permitAll()
-                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/uploads/**").permitAll()
+                        // 1. Endpoints Públicos
+                        .requestMatchers(HttpMethod.POST, "/usuarios/login", "/api/usuarios/login").permitAll()
+                        .requestMatchers(HttpMethod.POST, "/usuarios/logout", "/api/usuarios/logout").permitAll()
+                        .requestMatchers(HttpMethod.POST, "/agendamentos/bot", "/api/agendamentos/bot").permitAll()
+                        .requestMatchers(HttpMethod.POST, "/pagamentos/webhook", "/api/pagamentos/webhook").permitAll()
+                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                         .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**", "/webjars/**").permitAll()
 
-                        // Restrição específica da API (/api/**):
-                        // Permite que usuários com ROLE_CLIENT ou ROLE_ADMIN criem agendamentos via API externa
-                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/agendamentos", "/api/agendamentos/").hasAnyRole("CLIENT", "ADMIN")
-                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/usuarios/minha-senha", "/usuarios/minha-senha").authenticated()
-                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/**").authenticated()
-                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/**").hasRole("ADMIN")
-                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/**").hasRole("ADMIN")
-                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/**").hasRole("ADMIN")
-                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
+                        // 2. Rotas de Negócio Explícitas: aceitam tanto Sessão Web quanto API-KEY,
+                        // preservando as restrições estritas de ROLE
+                        .requestMatchers(HttpMethod.POST, "/quadras", "/quadras/**", "/api/quadras", "/api/quadras/**").hasRole("ADMIN")
+                        .requestMatchers(HttpMethod.PUT, "/quadras", "/quadras/**", "/api/quadras", "/api/quadras/**").hasRole("ADMIN")
+                        .requestMatchers(HttpMethod.PATCH, "/quadras", "/quadras/**", "/api/quadras", "/api/quadras/**").hasRole("ADMIN")
+                        .requestMatchers(HttpMethod.DELETE, "/quadras", "/quadras/**", "/api/quadras", "/api/quadras/**").hasRole("ADMIN")
+                        .requestMatchers("/notificacoes", "/notificacoes/**", "/api/notificacoes", "/api/notificacoes/**").hasRole("ADMIN")
 
-                        // Endpoints Web restritos a ADMIN
-                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/usuarios", "/usuarios/**").hasRole("ADMIN")
-                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/usuarios", "/usuarios/**").hasRole("ADMIN")
-                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/usuarios", "/usuarios/**").hasRole("ADMIN")
-                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/quadras", "/quadras/**").hasRole("ADMIN")
-                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/quadras", "/quadras/**").hasRole("ADMIN")
-                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/quadras", "/quadras/**").hasRole("ADMIN")
-                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/quadras", "/quadras/**").hasRole("ADMIN")
-                        .requestMatchers("/notificacoes", "/notificacoes/**").hasRole("ADMIN")
+                        .requestMatchers("/quadras", "/quadras/**", "/api/quadras", "/api/quadras/**")
+                            .hasAnyRole("ADMIN", "CLIENT")
+                        .requestMatchers("/agendamentos", "/agendamentos/**", "/api/agendamentos", "/api/agendamentos/**")
+                            .hasAnyRole("ADMIN", "CLIENT")
+                        .requestMatchers("/pagamentos", "/pagamentos/**", "/api/pagamentos", "/api/pagamentos/**")
+                            .hasAnyRole("ADMIN", "CLIENT")
 
-                        // Demais rotas web autenticadas (CLIENT ou ADMIN via cookie de sessão ou Bearer token)
-                        .requestMatchers("/quadras", "/quadras/**").authenticated()
-                        .requestMatchers("/agendamentos", "/agendamentos/**").authenticated()
-                        .requestMatchers("/pagamentos", "/pagamentos/**").authenticated()
-                        .requestMatchers("/usuarios", "/usuarios/**").authenticated()
-
-                        // Qualquer outra requer autenticação
-                        .anyRequest().authenticated()
+                        // 3. Qualquer outra rota (incluindo todas as rotas /usuarios/**, /usuarios/api-key/**, /usuarios/minha-senha, /usuarios/me):
+                        // Negação por padrão: EXIGE estritamente SCOPE_SESSION (API-KEY recebe 403 Forbidden automaticamente)
+                        .anyRequest().hasAuthority("SCOPE_SESSION")
+                )
+                .exceptionHandling(e -> e
+                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
+                        .accessDeniedHandler(jsonAccessDeniedHandler)
                 )
                 .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
-                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
-                .addFilterBefore(new com.agendamentos.equadras.security.RateLimitFilter(), JwtAuthenticationFilter.class);
+                .addFilterBefore(clientHeaderFilter, UsernamePasswordAuthenticationFilter.class)
+                .addFilterAfter(jwtAuthenticationFilter, ClientHeaderFilter.class);
 
         return http.build();
     }
```

---

## 2. `JwtAuthenticationFilter.java` (Modificado)
**Caminho:** `src/main/java/com/agendamentos/equadras/security/JwtAuthenticationFilter.java`  
**Objetivo:** Processar API-KEY com precedência estrita (se informada via `X-API-KEY` ou `Authorization: Bearer eq_...`, não realiza fallback para o cookie em caso de falha), aplicar rate limiting por IP, validar no banco e conceder `SCOPE_API_KEY`. Caso não haja chave, extrai cookie de sessão `equadras_session`, valida no banco se o usuário permanece ativo e concede `SCOPE_SESSION`.

```diff
diff --git a/src/main/java/com/agendamentos/equadras/security/JwtAuthenticationFilter.java b/src/main/java/com/agendamentos/equadras/security/JwtAuthenticationFilter.java
index 1726782..ccaa454 100644
--- a/src/main/java/com/agendamentos/equadras/security/JwtAuthenticationFilter.java
+++ b/src/main/java/com/agendamentos/equadras/security/JwtAuthenticationFilter.java
@@ -1,6 +1,7 @@
 package com.agendamentos.equadras.security;
 
-import com.agendamentos.equadras.model.enums.Role;
+import com.agendamentos.equadras.model.entity.Usuario;
+import com.agendamentos.equadras.repository.UsuarioRepository;
 import io.jsonwebtoken.Claims;
 import io.jsonwebtoken.JwtException;
 import jakarta.servlet.FilterChain;
@@ -8,30 +9,121 @@ import jakarta.servlet.ServletException;
 import jakarta.servlet.http.Cookie;
 import jakarta.servlet.http.HttpServletRequest;
 import jakarta.servlet.http.HttpServletResponse;
+import org.springframework.security.authentication.BadCredentialsException;
+import org.springframework.security.authentication.InsufficientAuthenticationException;
 import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
 import org.springframework.security.core.authority.SimpleGrantedAuthority;
 import org.springframework.security.core.context.SecurityContextHolder;
+import org.springframework.security.web.AuthenticationEntryPoint;
 import org.springframework.web.filter.OncePerRequestFilter;
 
 import java.io.IOException;
 import java.util.List;
+import java.util.Optional;
 
 public class JwtAuthenticationFilter extends OncePerRequestFilter {
 
     private final JwtService jwtService;
+    private final ApiKeyService apiKeyService;
+    private final ApiKeyRateLimiter rateLimiter;
+    private final UsuarioRepository usuarioRepository;
+    private final AuthenticationEntryPoint authenticationEntryPoint;
 
-    public JwtAuthenticationFilter(JwtService jwtService) {
+    public JwtAuthenticationFilter(
+            JwtService jwtService,
+            ApiKeyService apiKeyService,
+            ApiKeyRateLimiter rateLimiter,
+            UsuarioRepository usuarioRepository,
+            AuthenticationEntryPoint authenticationEntryPoint) {
         this.jwtService = jwtService;
+        this.apiKeyService = apiKeyService;
+        this.rateLimiter = rateLimiter;
+        this.usuarioRepository = usuarioRepository;
+        this.authenticationEntryPoint = authenticationEntryPoint;
     }
 
     @Override
     protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
             throws ServletException, IOException {
 
-        String uri = request.getRequestURI();
-        String method = request.getMethod();
+        // 1. Precedência Estrita: Verifica se a requisição forneceu API-KEY
+        String rawKey = null;
+        String apiKeyHeader = request.getHeader("X-API-KEY");
+        String authHeader = request.getHeader("Authorization");
+
+        if (apiKeyHeader != null && !apiKeyHeader.isBlank()) {
+            rawKey = apiKeyHeader.trim();
+        } else if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer eq_", 0, 10)) {
+            rawKey = authHeader.substring(7).trim();
+        }
+
+        if (rawKey != null) {
+            String ip = request.getRemoteAddr();
+            if (rateLimiter.isBloqueado(ip)) {
+                long retryAfter = rateLimiter.getRetryAfterSegundos(ip);
+                response.setStatus(429);
+                response.setHeader("Retry-After", String.valueOf(retryAfter));
+                response.setContentType("application/problem+json;charset=UTF-8");
+                response.getWriter().write(String.format("""
+                    {
+                        "status": 429,
+                        "title": "Too Many Requests",
+                        "detail": "Limite de tentativas incorretas de API-KEY excedido para este IP. Tente novamente em %d segundos."
+                    }
+                    """, retryAfter));
+                return;
+            }
+
+            Optional<Usuario> usuarioOpt = apiKeyService.autenticar(rawKey);
+            if (usuarioOpt.isEmpty()) {
+                rateLimiter.registrarFalha(ip);
+                // Não tenta o cookie como fallback
+                authenticationEntryPoint.commence(request, response,
+                        new BadCredentialsException("Chave de API inválida, revogada ou usuário inativo."));
+                return;
+            }
+
+            Usuario usuario = usuarioOpt.get();
+            UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado(
+                    usuario.getId_usuario(),
+                    usuario.getRole(),
+                    TipoAutenticacao.API_KEY
+            );
+
+            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
+                    usuarioAutenticado,
+                    null,
+                    List.of(
+                            new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()),
+                            new SimpleGrantedAuthority("SCOPE_API_KEY")
+                    )
+            );
+            SecurityContextHolder.getContext().setAuthentication(authentication);
+
+            // Registro assíncrono / não-bloqueante de último uso com throttling
+            apiKeyService.registrarUso(usuario.getId_usuario());
+
+            filterChain.doFilter(request, response);
+            return;
+        }
+
+        // 2. Processamento de Sessão Web via Cookie HttpOnly
         String cookieToken = null;
         if (request.getCookies() != null) {
             for (Cookie c : request.getCookies()) {
@@ -42,88 +134,37 @@ public class JwtAuthenticationFilter extends OncePerRequestFilter {
             }
         }
 
-        // 2. Extrai token do cabeçalho Authorization
-        String headerToken = null;
-        String header = request.getHeader("Authorization");
-        if (header != null && header.startsWith("Bearer ")) {
-            headerToken = header.substring(7);
-        }
-
-        // 3. Suporte a SSE (/notificacoes/stream) via query parameter
-        String sseToken = null;
-        if (uri != null && uri.contains("/notificacoes/stream") && request.getParameter("token") != null) {
-            sseToken = request.getParameter("token");
-        }
-
-        // Determina o token a ser validado
-        String token = (headerToken != null) ? headerToken : ((cookieToken != null) ? cookieToken : sseToken);
-
-        if (token != null) {
+        if (cookieToken != null) {
             try {
-                Claims claims = jwtService.validarEExtrairClaims(token);
+                Claims claims = jwtService.validarEExtrairClaims(cookieToken);
                 Long usuarioId = Long.valueOf(claims.getSubject());
-                Role role = Role.valueOf(claims.get("role", String.class));
-                UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado(usuarioId, role);
-
-                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
-                        usuarioAutenticado,
-                        null,
-                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
-                );
-                SecurityContextHolder.getContext().setAuthentication(authentication);
+
+                Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
+                if (usuarioOpt.isPresent() && usuarioOpt.get().isAtivo()) {
+                    Usuario usuario = usuarioOpt.get();
+                    UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado(
+                            usuario.getId_usuario(),
+                            usuario.getRole(),
+                            TipoAutenticacao.SESSAO_WEB
+                    );
+
+                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
+                            usuarioAutenticado,
+                            null,
+                            List.of(
+                                    new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()),
+                                    new SimpleGrantedAuthority("SCOPE_SESSION")
+                            )
+                    );
+                    SecurityContextHolder.getContext().setAuthentication(authentication);
+                } else {
+                    SecurityContextHolder.clearContext();
+                }
             } catch (JwtException | IllegalArgumentException e) {
                 SecurityContextHolder.clearContext();
             }
         }
 
-        // 4. Verificação de segurança para rotas internas consultadas pelo Frontend (rotas não /api)
-        // Permite requisições que possuam sessão válida (cookie ou token Bearer)
-        boolean isApiRoute = uri != null && (uri.startsWith("/api/") || uri.equals("/api"));
-        if (!isApiRoute && !isPublicFrontendRoute(uri, method)) {
-            if (SecurityContextHolder.getContext().getAuthentication() == null) {
-                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
-                response.setContentType("application/problem+json;charset=UTF-8");
-                response.getWriter().write("""
-                    {
-                        "status": 401,
-                        "title": "Acesso não autorizado",
-                        "detail": "Acesso restrito: para consultar recursos do sistema é obrigatório possuir uma autenticação válida (sessão ativa com cookie de login ou token de autorização). Requisições não autenticadas foram recusadas."
-                    }
-                    """);
-                return;
-            }
-        }
-
         filterChain.doFilter(request, response);
     }
 }
```

---

## 3. `ClientHeaderFilter.java` (Novo)
**Caminho:** `src/main/java/com/agendamentos/equadras/security/ClientHeaderFilter.java`  
**Objetivo:** Defesa anti-CSRF para endpoints mutantes (`POST`, `PUT`, `PATCH`, `DELETE`) de Sessão Web. Exige estritamente `X-Client: frontend`, isentando apenas chamadas com API-KEY (que exigem preflight CORS do navegador) e webhooks públicos.

```java
package com.agendamentos.equadras.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class ClientHeaderFilter extends OncePerRequestFilter {

    private static final Set<String> METODOS_MUTANTES = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();

        if (METODOS_MUTANTES.contains(method)) {
            // Isenção para Webhook Mercado Pago e Bot Agendamentos
            boolean isWebhookOuBot = uri != null && (uri.contains("/pagamentos/webhook") || uri.contains("/agendamentos/bot"));

            // Isenção para requisições com X-API-KEY ou Authorization (já disparam preflight CORS contra CSRF)
            String apiKeyHeader = request.getHeader("X-API-KEY");
            String authHeader = request.getHeader("Authorization");
            boolean possuiApiKeyOuAuth = (apiKeyHeader != null && !apiKeyHeader.isBlank()) ||
                                         (authHeader != null && !authHeader.isBlank());

            if (!isWebhookOuBot && !possuiApiKeyOuAuth) {
                String clientHeader = request.getHeader("X-Client");
                if (clientHeader == null || !clientHeader.trim().equalsIgnoreCase("frontend")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/problem+json;charset=UTF-8");
                    response.getWriter().write("""
                        {
                            "status": 403,
                            "title": "Acesso Proibido",
                            "detail": "Cabeçalho X-Client obrigatório ausente ou inválido para requisições com mutação de estado."
                        }
                        """);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

---

## 4. `ApiKeyService.java` (Novo)
**Caminho:** `src/main/java/com/agendamentos/equadras/security/ApiKeyService.java`  
**Objetivo:** Geração de chaves opacas (`eq_` + 43 caracteres Base64Url com 32 bytes de entropia via `SecureRandom`), persistência segura exclusivamente do hash SHA-256 e `last4`, rotação com invalidação atômica, auditoria completa de eventos (`GERADA`, `REGENERADA`, `REVOGADA`) e registro de último uso com throttling de 5 minutos.

```java
package com.agendamentos.equadras.security;

import com.agendamentos.equadras.dto.response.ApiKeyCriadaDTO;
import com.agendamentos.equadras.dto.response.ApiKeyInfoDTO;
import com.agendamentos.equadras.model.entity.AuditoriaApiKey;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.repository.AuditoriaApiKeyRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern API_KEY_PATTERN = Pattern.compile("^eq_[A-Za-z0-9_-]{43}$");

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaApiKeyRepository auditoriaRepository;

    public ApiKeyService(UsuarioRepository usuarioRepository, AuditoriaApiKeyRepository auditoriaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional
    public ApiKeyCriadaDTO gerarOuRegenerar(Long usuarioId, String ip, String userAgent) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado para ID: " + usuarioId));

        if (!usuario.isAtivo()) {
            throw new IllegalStateException("Usuário inativo não pode gerar API-KEY.");
        }

        String evento = (usuario.getApiKeyHash() == null) ? "GERADA" : "REGENERADA";

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomStr = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String rawApiKey = "eq_" + randomStr;

        String hash = sha256Hex(rawApiKey);
        String last4 = rawApiKey.substring(rawApiKey.length() - 4);
        Instant agora = Instant.now();

        usuario.setApiKeyHash(hash);
        usuario.setApiKeyLast4(last4);
        usuario.setApiKeyCriadaEm(agora);
        usuario.setApiKeyUltimoUsoEm(null);
        usuarioRepository.save(usuario);

        auditoriaRepository.save(new AuditoriaApiKey(usuarioId, evento, ip, userAgent, agora));

        log.info("API-KEY evento={} realizado para usuarioId={}, ip={}", evento, usuarioId, ip);

        return new ApiKeyCriadaDTO(rawApiKey, last4, agora);
    }

    @Transactional
    public void revogar(Long usuarioId, String ip, String userAgent) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado para ID: " + usuarioId));

        if (usuario.getApiKeyHash() == null) {
            // No-op idempotente se o usuário não possui chave ativa
            return;
        }

        usuario.setApiKeyHash(null);
        // Preserva last4, criadaEm e ultimoUsoEm para fins de auditoria
        usuarioRepository.save(usuario);

        Instant agora = Instant.now();
        auditoriaRepository.save(new AuditoriaApiKey(usuarioId, "REVOGADA", ip, userAgent, agora));

        log.info("API-KEY evento=REVOGADA realizado para usuarioId={}, ip={}", usuarioId, ip);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> autenticar(String rawKey) {
        if (rawKey == null || !API_KEY_PATTERN.matcher(rawKey).matches()) {
            return Optional.empty();
        }

        String hash = sha256Hex(rawKey);
        return usuarioRepository.findByApiKeyHash(hash)
                .filter(Usuario::isAtivo);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarUso(Long usuarioId) {
        try {
            Instant agora = Instant.now();
            Instant limite = agora.minus(Duration.ofMinutes(5));
            usuarioRepository.atualizarUltimoUsoComThrottling(usuarioId, agora, limite);
        } catch (Exception e) {
            log.warn("Falha não-bloqueante ao registrar último uso de API-KEY para usuarioId={}: {}", usuarioId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ApiKeyInfoDTO info(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado para ID: " + usuarioId));

        boolean possuiChave = usuario.getApiKeyHash() != null;
        if (!possuiChave) {
            return new ApiKeyInfoDTO(false, null, null, null);
        }

        return new ApiKeyInfoDTO(true, usuario.getApiKeyLast4(), usuario.getApiKeyCriadaEm(), usuario.getApiKeyUltimoUsoEm());
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(64);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 não disponível", e);
        }
    }
}
```

---

## 5. `V2__api_key_e_auditoria.sql` (Nova Migration)
**Caminho:** `src/main/resources/db/migration/V2__api_key_e_auditoria.sql`  
**Objetivo:** Adicionar as colunas de suporte à API-KEY na tabela `usuarios`, criar o índice único parcial sobre `api_key_hash` e a tabela de auditoria `auditoria_api_key`.

```sql
ALTER TABLE usuarios
  ADD COLUMN IF NOT EXISTS api_key_hash          VARCHAR(64),
  ADD COLUMN IF NOT EXISTS api_key_last4         VARCHAR(4),
  ADD COLUMN IF NOT EXISTS api_key_criada_em     TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS api_key_ultimo_uso_em TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS idx_usuarios_api_key_hash
  ON usuarios (api_key_hash) WHERE api_key_hash IS NOT NULL;

CREATE TABLE IF NOT EXISTS auditoria_api_key (
  id          BIGSERIAL PRIMARY KEY,
  usuario_id  BIGINT NOT NULL REFERENCES usuarios (id_usuario),
  evento      VARCHAR(20) NOT NULL CHECK (evento IN ('GERADA','REGENERADA','REVOGADA')),
  ip          VARCHAR(45),
  user_agent  VARCHAR(255),
  criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_auditoria_api_key_usuario ON auditoria_api_key (usuario_id, criado_em DESC);
```

---

## 6. `ApiKeySecurityIntegrationTest.java` (Novos Testes de Integração)
**Caminho:** `src/test/java/com/agendamentos/equadras/security/ApiKeySecurityIntegrationTest.java`  
**Objetivo:** Bateria com 11 testes de integração automatizados que validam todas as regras de segurança em nível HTTP/Spring Security.

```java
package com.agendamentos.equadras.security;

import com.agendamentos.equadras.dto.response.ApiKeyCriadaDTO;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class ApiKeySecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private Usuario usuarioAdmin;
    private String rawApiKeyAdmin;
    private String sessionCookieAdmin;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Cria ou atualiza usuário admin no banco
        usuarioAdmin = usuarioRepository.findByEmail_usuario("admin_integ@equadras.com")
                .orElseGet(() -> usuarioRepository.save(
                        Usuario.builder()
                                .nome_usuario("Admin Integracao")
                                .email_usuario("admin_integ@equadras.com")
                                .senha_usuario("$2a$10$N.ZOmG4i2OUPOgD8xDW3qux6w.2B8.0ZkJqPuhf45sC")
                                .phone_usuario("11988887777")
                                .role(Role.ADMIN)
                                .ativo(true)
                                .build()
                ));

        // Gera API-KEY real para o admin
        ApiKeyCriadaDTO apiKeyDto = apiKeyService.gerarOuRegenerar(usuarioAdmin.getId_usuario(), "127.0.0.1", "MockMvc");
        rawApiKeyAdmin = apiKeyDto.apiKey();

        // Gera token JWT de sessão web
        sessionCookieAdmin = jwtService.gerarToken(usuarioAdmin);
    }

    @Test
    @DisplayName("API-KEY válida deve acessar rota de negócio permitida (GET /quadras)")
    void apiKeyValidaDeveAcessarRotaDeNegocio() throws Exception {
        mockMvc.perform(get("/quadras")
                        .header("X-API-KEY", rawApiKeyAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("API-KEY via Authorization Bearer eq_... deve acessar rota de negócio permitida")
    void apiKeyViaBearerDeveAcessarRotaDeNegocio() throws Exception {
        mockMvc.perform(get("/quadras")
                        .header("Authorization", "Bearer " + rawApiKeyAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("API-KEY em rota de conta (GET /usuarios/me) deve retornar 403 Forbidden por isolamento de escopo")
    void apiKeyEmRotaDeContaDeveRetornar403() throws Exception {
        mockMvc.perform(get("/usuarios/me")
                        .header("X-API-KEY", rawApiKeyAdmin))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Acesso Proibido"));
    }

    @Test
    @DisplayName("API-KEY em rota de gerenciamento de chaves (POST /usuarios/api-key/regenerar) deve retornar 403 Forbidden")
    void apiKeyNaoPodeGerenciarPropriaConta() throws Exception {
        mockMvc.perform(post("/usuarios/api-key/regenerar")
                        .header("X-API-KEY", rawApiKeyAdmin))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("API-KEY inválida deve retornar 401 Unauthorized imediatamente")
    void apiKeyInvalidaDeveRetornar401() throws Exception {
        mockMvc.perform(get("/quadras")
                        .header("X-API-KEY", "eq_chaveinvalidaecompletamenteinexistentenobanco12345"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Precedência Estrita: Requisição com X-API-KEY inválido E Cookie de sessão válido deve retornar 401 sem fallback")
    void precedenciaEstritaApiKeySobreCookie() throws Exception {
        mockMvc.perform(get("/quadras")
                        .header("X-API-KEY", "eq_chaveinvalida")
                        .cookie(new Cookie("equadras_session", sessionCookieAdmin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Invalidação Imediata: Ao regenerar a chave, chave anterior retorna 401 no mesmo instante")
    void invalidacaoImediataAoRegenerar() throws Exception {
        // Valida que a chave inicial funciona
        mockMvc.perform(get("/quadras")
                        .header("X-API-KEY", rawApiKeyAdmin))
                .andExpect(status().isOk());

        // Regenera a chave (invalidação atômica no banco)
        ApiKeyCriadaDTO novaChaveDto = apiKeyService.gerarOuRegenerar(usuarioAdmin.getId_usuario(), "127.0.0.1", "MockMvc");
        String novaApiKey = novaChaveDto.apiKey();

        // Chave antiga deve falhar com 401 imediatamente
        mockMvc.perform(get("/quadras")
                        .header("X-API-KEY", rawApiKeyAdmin))
                .andExpect(status().isUnauthorized());

        // Nova chave deve funcionar com 200
        mockMvc.perform(get("/quadras")
                        .header("X-API-KEY", novaApiKey))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("API-KEY de usuário inativo deve retornar 401 Unauthorized")
    void apiKeyUsuarioInativoDeveRetornar401() throws Exception {
        usuarioAdmin.setAtivo(false);
        usuarioRepository.save(usuarioAdmin);

        mockMvc.perform(get("/quadras")
                        .header("X-API-KEY", rawApiKeyAdmin))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Sessão Web via Cookie acessa rota de conta (GET /usuarios/me) com 200 OK")
    void sessaoWebAcessaRotaDeContaComSucesso() throws Exception {
        mockMvc.perform(get("/usuarios/me")
                        .cookie(new Cookie("equadras_session", sessionCookieAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email_usuario").value("admin_integ@equadras.com"));
    }

    @Test
    @DisplayName("Mutação via Sessão Web sem header X-Client deve retornar 403 Forbidden (proteção CSRF)")
    void mutacaoSessaoSemHeaderXClientDeveRetornar403() throws Exception {
        mockMvc.perform(post("/usuarios/api-key/regenerar")
                        .cookie(new Cookie("equadras_session", sessionCookieAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso Proibido"))
                .andExpect(jsonPath("$.detail").value("Cabeçalho X-Client obrigatório ausente ou inválido para requisições com mutação de estado."));
    }

    @Test
    @DisplayName("Mutação via Sessão Web com header X-Client: frontend deve retornar 200 OK")
    void mutacaoSessaoComHeaderXClientDevePassar() throws Exception {
        mockMvc.perform(post("/usuarios/api-key/regenerar")
                        .cookie(new Cookie("equadras_session", sessionCookieAdmin))
                        .header("X-Client", "frontend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey").exists())
                .andExpect(jsonPath("$.last4").exists());
    }
}
```
