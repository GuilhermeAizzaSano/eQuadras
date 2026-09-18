package com.agendamentos.equadras.config;

import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${equadras.cors.origens-permitidas:http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173,http://127.0.0.1:3000,https://equadras.app,https://www.equadras.app}")
    private String[] origensPermitidas;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(origensPermitidas));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS", "HEAD"));
        // Permite headers legítimos de navegação da SPA. X-API-KEY e Authorization ficam fora da allowlist CORS do browser.
        configuration.setAllowedHeaders(List.of("Content-Type", "X-Client", "X-Correlation-Id"));
        configuration.setExposedHeaders(List.of("Set-Cookie", "Content-Disposition", "X-Total-Count", "X-Correlation-Id", "Retry-After"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtService jwtService,
            ApiKeyService apiKeyService,
            ApiKeyRateLimiter apiKeyRateLimiter,
            UsuarioRepository usuarioRepository,
            JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint,
            JsonAccessDeniedHandler jsonAccessDeniedHandler) throws Exception {

        RateLimitFilter rateLimitFilter = new RateLimitFilter();
        ClientHeaderFilter clientHeaderFilter = new ClientHeaderFilter(jsonAccessDeniedHandler);
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
                jwtService,
                apiKeyService,
                apiKeyRateLimiter,
                usuarioRepository,
                jsonAuthenticationEntryPoint
        );

        http
                // Proteção CSRF: ClientHeaderFilter (exigência estrita de X-Client) + CORS restrito para métodos mutantes
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Endpoints Públicos
                        .requestMatchers(HttpMethod.POST, "/usuarios/login", "/api/usuarios/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/usuarios/logout", "/api/usuarios/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/agendamentos/bot", "/api/agendamentos/bot").permitAll()
                        .requestMatchers(HttpMethod.POST, "/pagamentos/webhook", "/api/pagamentos/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**", "/webjars/**").permitAll()

                        // 2. Rotas de Negócio Explícitas: aceitam tanto Sessão Web quanto API-KEY,
                        // preservando as restrições estritas de ROLE
                        .requestMatchers(HttpMethod.POST, "/quadras", "/quadras/**", "/api/quadras", "/api/quadras/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/quadras", "/quadras/**", "/api/quadras", "/api/quadras/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/quadras", "/quadras/**", "/api/quadras", "/api/quadras/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/quadras", "/quadras/**", "/api/quadras", "/api/quadras/**").hasRole("ADMIN")
                        .requestMatchers("/notificacoes", "/notificacoes/**", "/api/notificacoes", "/api/notificacoes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/pagamentos/*/simular-aprovacao", "/api/pagamentos/*/simular-aprovacao").hasAnyRole("ADMIN", "CLIENT")

                        .requestMatchers("/quadras", "/quadras/**", "/api/quadras", "/api/quadras/**")
                            .hasAnyRole("ADMIN", "CLIENT")
                        .requestMatchers("/agendamentos", "/agendamentos/**", "/api/agendamentos", "/api/agendamentos/**")
                            .hasAnyRole("ADMIN", "CLIENT")
                        .requestMatchers("/pagamentos", "/pagamentos/**", "/api/pagamentos", "/api/pagamentos/**")
                            .hasAnyRole("ADMIN", "CLIENT")

                        // 3. Qualquer outra rota (incluindo todas as rotas /usuarios/**, /usuarios/api-key/**, /usuarios/minha-senha, /usuarios/me):
                        // Negação por padrão: EXIGE estritamente SCOPE_SESSION (API-KEY recebe 403 Forbidden automaticamente)
                        .anyRequest().hasAuthority("SCOPE_SESSION")
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                )
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(rateLimitFilter, org.springframework.security.web.authentication.logout.LogoutFilter.class)
                .addFilterBefore(clientHeaderFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}