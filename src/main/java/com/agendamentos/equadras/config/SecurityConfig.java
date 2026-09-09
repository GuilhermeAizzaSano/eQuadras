package com.agendamentos.equadras.config;

import com.agendamentos.equadras.security.JwtAuthenticationFilter;
import com.agendamentos.equadras.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.List.of("*"));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setExposedHeaders(java.util.List.of("Authorization", "Set-Cookie", "Content-Disposition", "X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Públicos
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/usuarios/login", "/api/usuarios/login").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/usuarios/logout", "/api/usuarios/logout").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/agendamentos/bot", "/api/agendamentos/bot").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/pagamentos/webhook", "/api/pagamentos/webhook").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**", "/webjars/**").permitAll()

                        // Restrição específica da API (/api/**):
                        // Usuário com Role CLIENT pode apenas consultar/listar dados (GET)
                        // Modificações (POST, PUT, PATCH, DELETE) na API exigem ROLE_ADMIN
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/usuarios/minha-senha", "/usuarios/minha-senha").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/**").hasRole("ADMIN")

                        // Endpoints Web restritos a ADMIN
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/usuarios", "/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/usuarios", "/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/usuarios", "/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/quadras", "/quadras/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/quadras", "/quadras/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/quadras", "/quadras/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/quadras", "/quadras/**").hasRole("ADMIN")
                        .requestMatchers("/notificacoes", "/notificacoes/**").hasRole("ADMIN")

                        // Demais rotas web autenticadas (CLIENT ou ADMIN via cookie de sessão)
                        .requestMatchers("/quadras", "/quadras/**").authenticated()
                        .requestMatchers("/agendamentos", "/agendamentos/**").authenticated()
                        .requestMatchers("/pagamentos", "/pagamentos/**").authenticated()
                        .requestMatchers("/usuarios", "/usuarios/**").authenticated()

                        // Qualquer outra requer autenticação
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new com.agendamentos.equadras.security.RateLimitFilter(), JwtAuthenticationFilter.class);

        return http.build();
    }
}