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
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Públicos
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/usuarios", "/usuarios/", "/usuarios/login").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/quadras", "/quadras/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/agendamentos/quadra/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/pagamentos/webhook").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // Apenas ADMIN
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/quadras", "/quadras/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/quadras", "/quadras/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/quadras", "/quadras/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/quadras", "/quadras/**").hasRole("ADMIN")
                        .requestMatchers("/notificacoes", "/notificacoes/**").hasRole("ADMIN")

                        // Qualquer autenticado (CLIENT ou ADMIN)
                        .requestMatchers("/agendamentos", "/agendamentos/**").authenticated()
                        .requestMatchers("/pagamentos", "/pagamentos/**").authenticated()
                        .requestMatchers("/usuarios", "/usuarios/**").authenticated()

                        // Qualquer outra requer autenticação
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}