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
        configuration.setAllowedOriginPatterns(java.util.List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.*:*",
                "http://10.*:*",
                "http://172.16.*:*",
                "http://172.17.*:*",
                "http://172.18.*:*",
                "http://172.19.*:*",
                "http://172.20.*:*",
                "http://172.21.*:*",
                "http://172.22.*:*",
                "http://172.23.*:*",
                "http://172.24.*:*",
                "http://172.25.*:*",
                "http://172.26.*:*",
                "http://172.27.*:*",
                "http://172.28.*:*",
                "http://172.29.*:*",
                "http://172.30.*:*",
                "http://172.31.*:*",
                "http://*:*", "http://*", "https://*", "https://*:*", "https://equadras.app", "https://www.equadras.app"
        ));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(true);

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
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/usuarios", "/usuarios/", "/api/usuarios", "/api/usuarios/").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/usuarios/login", "/api/usuarios/login").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/quadras", "/quadras/**", "/api/quadras", "/api/quadras/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/agendamentos/quadra/**", "/api/agendamentos/quadra/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/agendamentos/bot", "/api/agendamentos/bot").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/agendamentos/horarios-disponiveis", "/api/agendamentos/horarios-disponiveis").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/pagamentos/webhook", "/api/pagamentos/webhook").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**", "/webjars/**").permitAll()

                        // Apenas ADMIN
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/quadras", "/quadras/**", "/api/quadras", "/api/quadras/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/quadras", "/quadras/**", "/api/quadras", "/api/quadras/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/quadras", "/quadras/**", "/api/quadras", "/api/quadras/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/quadras", "/quadras/**", "/api/quadras", "/api/quadras/**").hasRole("ADMIN")
                        .requestMatchers("/notificacoes", "/notificacoes/**", "/api/notificacoes", "/api/notificacoes/**").hasRole("ADMIN")

                        // Qualquer autenticado (CLIENT ou ADMIN)
                        .requestMatchers("/agendamentos", "/agendamentos/**", "/api/agendamentos", "/api/agendamentos/**").authenticated()
                        .requestMatchers("/pagamentos", "/pagamentos/**", "/api/pagamentos", "/api/pagamentos/**").authenticated()
                        .requestMatchers("/usuarios", "/usuarios/**", "/api/usuarios", "/api/usuarios/**").authenticated()

                        // Qualquer outra requer autenticação
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}